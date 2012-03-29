package net.sourceforge.texlipse.builder;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.auxparser.AuxFileParser;
import net.sourceforge.texlipse.builder.factory.BuilderDescription;
import net.sourceforge.texlipse.model.ReferenceContainer;
import net.sourceforge.texlipse.model.ReferenceEntry;
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
public class TexCycleBuilder extends AbstractBuilder {

    private ProgramRunner latex;
    private ProgramRunner utilRunner;
    private BuildCycleDetector cycleDetector;
    private boolean stopped;

    /**
     * Check if '-recorder' command argument is necessary for latex runner and
     * add as needed.
     *
     * @param latexRunner
     */
    private void checkRecorderFlag() {
        // First check if cycle detector is depending on this
        if (cycleDetector.needsRecorderFlag()) {
            String arg = latex.getProgramArguments();
            // Check if the flag is already present
            if (arg.indexOf("-recorder") < 0) {
                // TODO clean up
                //config.setProgramArguments("-recorder ".concat(arg));
            }
        }
    }

    /**
     * Determines the name of the root aux-file to be used by the 
     * <code>AuxFileParser</code>.
     *
     * @param buildResource resource which is currently being built
     * @return potential name of the aux file
     */
    private String getAuxFileName(IResource buildResource) {
        final String buildFileName = buildResource.getProjectRelativePath().toPortableString();
        return OutputFileManager.stripFileExt(buildFileName, ".tex").concat(".aux");
    }

    /**
     * Extracts all labels defined in the aux-file and adds them to the
     * label container
     *
     * @param afp the <code>AuxFileParser</code> used to extract the labels
     */
    private void extractLabels(AuxFileParser afp) {
    	ReferenceContainer labelC = (ReferenceContainer) TexlipseProperties
    			.getSessionProperty(afp.getProject(),
    					TexlipseProperties.LABELCONTAINER_PROPERTY);
    	if (labelC != null) {
    		// Add temp path to aux-File
    		String tempPath = TexlipseProperties.getProjectProperty(afp.getProject(),
    				TexlipseProperties.TEMP_DIR_PROPERTY);
    		String correctedAuxFileName = tempPath + File.separator
    				+ afp.getRootAuxFile();

    		// First remove the labels
    		labelC.addRefSource(correctedAuxFileName,
    				new LinkedList<ReferenceEntry>());
    		// and reorganize
    		labelC.organize();
    		// now add them
    		labelC.updateRefSource(correctedAuxFileName, afp.getLabels());
    	}
    }

    /**
     * Clears errors and warnings from the problem view. If LaTeX runs more than once, this
     * makes sure, the view only shows the messages of the last run, which are still valid.
     *
     * @param project the project
     */
    private void clearMarkers(IProject project) {
        try {
            project.deleteMarkers(TexlipseBuilder.MARKER_TYPE, false, IResource.DEPTH_INFINITE);
            project.deleteMarkers(TexlipseBuilder.LAYOUT_WARNING_TYPE, false, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
        }
    }

    public TexCycleBuilder(BuilderDescription description) {
        super(description);
        this.latex = BuilderRegistry.getRunner(description.getRunnerId());
        this.utilRunner = null;
        this.cycleDetector = null;
    }

    @Override
    public boolean isValid() {
        if (latex == null || !latex.isValid()) {
            latex = BuilderRegistry.getRunner(description.getRunnerId());
        }
        return latex != null && latex.isValid();
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
        checkRecorderFlag();

        stopped = false;
        // Make sure we close the output document first 
        // (using DDE on Win32)
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
            monitor.subTask("Closing output document");     
            ViewerManager.closeOutputDocument();
            monitor.worked(5);          
        }

        final IProject project = resource.getProject();
        final boolean parseAuxFiles = TexlipsePlugin.getDefault().getPreferenceStore()
                .getBoolean(TexlipseProperties.BUILDER_PARSE_AUX_FILES);
        final boolean showMaxError = TexlipsePlugin.getDefault().getPreferenceStore()
                .getBoolean(TexlipseProperties.BUILD_CYCLE_MAX_ERROR);
        final boolean haltOnInvalid = TexlipsePlugin.getDefault().getPreferenceStore()
                .getBoolean(TexlipseProperties.BUILD_CYCLE_HALT_INVALID);
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
            final AuxFileParser afp = new AuxFileParser(project, auxFileName);
            // add the labels defined in the .aux-file to the label container
            extractLabels(afp);
        }

        final Set<String> brokenRunners = new TreeSet<String>();

        cycleDetector.checkLatexOutput(monitor);
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

    /**
     * Returns the cycle detector of this builder instance.
     *
     * @return cycle detector
     */
    public BuildCycleDetector getCycleDetector() {
        return cycleDetector;
    }

    /**
     * Sets the cycle detector, which determines how often latex and other
     * runners need to be triggered.
     * 
     * @param cycleDetector cycle detector instance
     */
    public void setCycleDetector(BuildCycleDetector cycleDetector) {
        this.cycleDetector = cycleDetector;
    }

}
