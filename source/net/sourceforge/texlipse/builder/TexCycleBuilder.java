package net.sourceforge.texlipse.builder;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.auxparser.AuxFileParser;
import net.sourceforge.texlipse.builder.factory.BuilderDescription;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.viewer.ViewerManager;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;


/**
 * This tex builder monitors builds a latex file. After each build process it
 * re-evaluates, if additional runners are required and if latex needs to run
 * in further increments.
 *
 * @author Matthias Erll
 *
 */
public class TexCycleBuilder extends AbstractLatexBuilder implements CycleBuilder {

    private ProgramRunner utilRunner;
    private BuildCycleDetector cycleDetector;
    private boolean stopped;

    public TexCycleBuilder(BuilderDescription description) {
        super(description);
        this.utilRunner = null;
        this.cycleDetector = null;
    }

    @Override
    public void stopRunners() {
        if (latex != null) {
            latex.stop();
        }
        if (utilRunner != null) {
            utilRunner.stop();
        }
        stopped = true;
    }

    /**
     * Runs the latex builder. This includes
     * <ul>
     * <li>running the latex builder once;</li>
     * <li>checking, if any runners need to be triggered afterwards;</li>
     * <li>checking, if these runners require any more runners;</li>
     * <li>returning to running latex, and repeating as often as necessary for
     *  completing the build process; or until the maximum as defined in the
     *  preferences has been reached.</li>
     * </ul>
     */
    @Override
    public void buildResource(IResource resource) throws CoreException {
        if (cycleDetector == null) {
            throw new CoreException(TexlipsePlugin.stat(
                    "Cycle detector has not been initialized."));
        }

        stopped = false;
        // Make sure we close the output document first 
        // (using DDE on Win32)
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
            monitor.subTask("Closing output document");     
            ViewerManager.closeOutputDocument();
            monitor.worked(5);          
        }

        final IProject project = resource.getProject();
        final boolean showMaxError = TexlipsePlugin.getDefault().getPreferenceStore()
                .getBoolean(TexlipseProperties.BUILD_CYCLE_MAX_ERROR);
        final boolean haltOnInvalid = TexlipsePlugin.getDefault().getPreferenceStore()
                .getBoolean(TexlipseProperties.BUILD_CYCLE_HALT_INVALID);
        final boolean parseAuxFiles = TexlipsePlugin.getDefault().getPreferenceStore()
                .getBoolean(TexlipseProperties.BUILDER_PARSE_AUX_FILES);
        final String auxFileName = getAuxFileName(resource);
        final IResource auxFile = project.getFile(auxFileName);
        // Initialize file hashes
        cycleDetector.initFileTracking(monitor);

        monitor.subTask("Building document");
        latex.run(resource);
        monitor.worked(10);
        if (stopped) {
            return;
        }

        if (parseAuxFiles && auxFile.exists()) {
            updateContainers(resource, new AuxFileParser(project, auxFileName));
        }
        else {
            updateContainers(resource, null);
        }

        final Set<String> brokenRunners = new TreeSet<String>();

        cycleDetector.checkInitialLatexOutput(monitor);
        while (!cycleDetector.isDone() && (!haltOnInvalid || brokenRunners.isEmpty())) {
            clearMarkers(project);
            utilRunner = cycleDetector.getNextRunner();
            while (utilRunner != null && (!haltOnInvalid || brokenRunners.isEmpty())) {
                if (stopped) {
                    return;
                }
                if (utilRunner.isValid()) {
                    utilRunner.run(resource);
                    monitor.worked(10);
                    cycleDetector.checkRunnerOutput(monitor);
                }
                else {
                    brokenRunners.add(utilRunner.getProgramName());
                }
                utilRunner = cycleDetector.getNextRunner();
            }
            if (stopped) {
                return;
            }
            latex.run(resource);
            monitor.worked(10);
            cycleDetector.checkLatexOutput(monitor);
        }
        // Store hash values
        cycleDetector.saveFileTracking(monitor);

        if (cycleDetector.isMaxedOut() && showMaxError) {
            // If the preferences say to throw an error, do that here
            AbstractProgramRunner.createMarker(resource, null,
                    TexlipsePlugin.getResourceString("builderErrorMaximumReached"),
                    IMarker.SEVERITY_ERROR);
        }

        if (!brokenRunners.isEmpty()) {
            final StringBuffer buffer = new StringBuffer();
            final Iterator<String> i = brokenRunners.iterator();
            buffer.append(i.next());
            while (i.hasNext()) {
                buffer.append("; ");
                buffer.append(i.next());
            }

            // If build process has been halted due to a runner problem, make this
            // an error, otherwise a warning
            final int severity = haltOnInvalid ?
                    IMarker.SEVERITY_ERROR : IMarker.SEVERITY_WARNING;
            AbstractProgramRunner.createMarker(resource, null, String.format(
                    TexlipsePlugin.getResourceString("builderWarningBrokenRunners"),
                    new Object[] {buffer.toString()}), severity);
        }
    }

    public BuildCycleDetector getCycleDetector() {
        return cycleDetector;
    }

    public void setCycleDetector(BuildCycleDetector cycleDetector) {
        this.cycleDetector = cycleDetector;
    }

}
