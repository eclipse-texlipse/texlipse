package net.sourceforge.texlipse.builder;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;


/**
 * Detects and automatically generates the cycle for a complete latex build
 * process. It uses a project file tracking instance for monitoring which files
 * are being modified in the source folder. Additionally, it detects which
 * files are in return read by latex, and which additional runners (e.g.
 * bibtex) need to be triggered.
 *
 * @author Matthias Erll
 *
 */
public class BuildCycleDetector {

    private final IProject project;
    private final ProjectFileTracking fileTracking;
    private final FlsAnalyzer analyzer;
    private final LinkedList<ProgramRunner> runners;
    private final IContainer sourceContainer;
    private final String[] tempExts;
    private final String[] addExts;
    private final int totalMax;

    private int totalCount;
    private boolean done;

    /**
     * Schedules a program runner to run, i.e. moves it to the list of runners.
     * Avoids duplicates.
     *
     * @param runner program runner
     * @param push whether to move the runner to the beginning of the list
     */
    private void queueRunner(ProgramRunner runner, boolean push) {
        int currentPos = runners.indexOf(runner);
        if (push) {
            // If scheduled later, move to front of the list
            if (currentPos > 0) {
                runners.remove(currentPos);
            }
            if (currentPos != 0) {
                runners.addFirst(runner);
            }
        }
        else {
            // If already scheduled, there is no use for scheduling
            // it later
            if (currentPos < 0) {
                runners.add(runner);
            }
        }
    }

    /**
     * Checks whether session variables indicate that bibtex or biblatex needs to run. This should
     * only be done once per build cycle, since otherwise it leads to infinite loops if citations
     * are undefined. 
     */
    private void checkBibtexVariables() {
        final String bibRerun = (String) TexlipseProperties.getSessionProperty(
                project, TexlipseProperties.SESSION_BIBTEX_RERUN);
        final Boolean bibFilesChanged = (Boolean) TexlipseProperties.getSessionProperty(
                project, TexlipseProperties.BIBFILES_CHANGED);
        if ("true".equals(bibRerun)
                || (Boolean.TRUE.equals(bibFilesChanged))) {
            final ProgramRunner bibRunner;
            Boolean biblatexMode = (Boolean) TexlipseProperties.getSessionProperty(project,
                    TexlipseProperties.SESSION_BIBLATEXMODE_PROPERTY);
            if (Boolean.TRUE.equals(biblatexMode)) {
                bibRunner = BuilderRegistry.getRunner(
                        TexlipseProperties.INPUT_FORMAT_BCF,
                        TexlipseProperties.OUTPUT_FORMAT_BBL);
            }
            else {
                bibRunner = BuilderRegistry.getRunner(
                        TexlipseProperties.INPUT_FORMAT_BIB,
                        TexlipseProperties.OUTPUT_FORMAT_AUX);
            }
            if (bibRunner != null) {
                queueRunner(bibRunner, true);
                done = false;
            }
        }
    }

    /**
     * Checks if the latex runner has set any session variables, which affect the
     * build process.
     */
    private void checkLatexOutputVariables() {
        final String latexRerun = (String) TexlipseProperties.getSessionProperty(
                project, TexlipseProperties.SESSION_LATEX_RERUN);
        // Initialize with values from log output
        if ("true".equals(latexRerun)) {
            done = false;
        }
    }

    /**
     * Checks the given latex or runner changed output files for their consequences
     * regarding the required build process:
     * <ul>
     * <li>If a runner for this file exists, it is scheduled to be started.</li>
     * <li>Independent from other runners, if latex re-reads this file (e.g.
     *  toc files), another latex run is scheduled.
     * </ul>
     *
     * @param changedOutputFiles set of files which have been modified during a
     *  recent run
     * @param push if set to <code>true</code>, runners which need to be triggered
     *  are scheduled before any other waiting runners. Otherwise they are moved
     *  to be back of the queue, but still started before the next latex process
     */
    private void checkOutputFiles(final Set<IPath> changedOutputFiles, boolean push) {
        final Set<IPath> inputFiles;
        if (analyzer != null) {
            inputFiles = analyzer.getInputFiles();
        }
        else {
            //TODO get file set from log output
            inputFiles = new HashSet<IPath>();
        }
        for (IPath changedFile : changedOutputFiles) {
            final String fileExt = changedFile.getFileExtension();
            if (fileExt != null && fileExt.length() > 0
                    && !"tex".equals(fileExt)) {
                ProgramRunner runner = BuilderRegistry.getRunner(fileExt, null);
                if (runner != null) {
                    // Schedule runner before next LaTeX rebuild
                    queueRunner(runner, push);
                    done = false;
                }
                if (inputFiles.contains(changedFile)) {
                    // LaTeX incremental build, needs another run-through
                    done = false;
                }
            }
        }
    }

    /**
     * Constructor.
     *
     * @param project current project
     * @param resource resource to be built
     * @param fileTracking project file tracking instance
     */
    public BuildCycleDetector(final IProject project, final IResource resource,
            final ProjectFileTracking fileTracking) {
        this.project = project;
        this.fileTracking = fileTracking;
        final String useFlsParser = TexlipsePlugin.getPreference(
                TexlipseProperties.BUILD_CYCLE_FLS_ENABLED);
        if ("true".equals(useFlsParser)) {
            this.analyzer = new FlsAnalyzer(project, resource);
        }
        else {
            this.analyzer = null;
        }
        this.runners = new LinkedList<ProgramRunner>();
        this.sourceContainer = resource.getParent();
        this.totalMax = TexlipsePlugin.getDefault().getPreferenceStore()
                .getInt(TexlipseProperties.BUILD_CYCLE_MAX);
        this.tempExts = TexlipsePlugin.getPreferenceArray(
                TexlipseProperties.TEMP_FILE_EXTS);
        this.addExts = TexlipsePlugin.getPreferenceArray(
                TexlipseProperties.BUILD_CYCLE_ADD_EXTS);
        this.totalCount = 0;
        this.done = true;
    }

    /**
     * Reads and initializes the hash values for detecting file modifications.
     * Additionally, session variables for bibtex / biblatex are evaluated.
     * If file changes have been made since the cache was stored (or the cache has been
     * initial), this can also lead to scheduling runners. However, runners are
     * not run before the first LaTeX run-through.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void initFileTracking(IProgressMonitor monitor) throws CoreException {
        Set<IPath> initialChanges = fileTracking.initFileCache(tempExts, addExts, monitor);
        checkBibtexVariables();
        checkOutputFiles(initialChanges, false);
    }

    /**
     * Make the file tracking persistent, in case the session gets closed.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void saveFileTracking(IProgressMonitor monitor) throws CoreException {
        fileTracking.saveFileCache(monitor);
    }

    /**
     * Returns the next runner, which is scheduled to run. This should be
     * called until <code>null</code> is returned.
     *
     * @return runner which is next in line to start
     */
    public ProgramRunner getNextRunner() {
        ProgramRunner nextRunner = runners.poll();
        return nextRunner;
    }

    /**
     * Checks the latex output (i.e. the FLS file) for input and output files,
     * compares this to actual changes in the source folder. If necessary, latex
     * and other runners are scheduled to be run again. This list of necessary
     * activities might get modified later when analyzed further runner output.
     * Latex is only re-run to a certain maximum count, to avoid infinite loops.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void checkLatexOutput(IProgressMonitor monitor) throws CoreException {
        // Check if the limit of build cycles has been reached
        totalCount++;
        if (isMaxedOut()) {
            done = true;
            return;
        }
        else {
            // Initialize depending on queued runners
            done = runners.isEmpty();
        }

        // Check if further action relies on latex or FLS output
        if (analyzer != null) {
            try {
                analyzer.parse();
            }
            catch (IOException e) {
                throw new CoreException(TexlipsePlugin.stat(TexlipsePlugin
                        .getResourceString("builderErrorCannotReadFls"), e));
            }
        }
        // Check latex output for additional info
        checkLatexOutputVariables();

        final Set<IPath> changedContentFiles = fileTracking.updateChangedFiles(
                sourceContainer, tempExts, addExts, monitor);

        checkOutputFiles(changedContentFiles, false);
    }

    /**
     * Checks the project source folder for changes in the file system, schedules
     * more runners, and also more latex run-throughs if found necessary.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void checkRunnerOutput(IProgressMonitor monitor) throws CoreException {
        final Set<IPath> changedContentFiles = fileTracking.updateChangedFiles(
                sourceContainer, tempExts, addExts, monitor);
        checkOutputFiles(changedContentFiles, true);
    }

    /**
     * Return if the '-recorder' flag is needed by the cycle detector.
     *
     * @return true, if the recorder flag needs to be added to the latex
     *  command line arguments, false otherwise
     */
    public boolean needsRecorderFlag() {
        return analyzer != null;
    }

    /**
     * Returns <code>false</code> if latex should be run at least once more. If
     * it returns <code>true</code> this can either mean the latex build process
     * is completed, or that the set maximum of latex build cycles has been
     * reached.
     *
     * @return if latex should run again
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Returns if the maximum amount of build cycles has been reached.
     *
     * @return if the counted amount of latex run-throughs is equal or larger
     *  than the desired maximum
     */
    public boolean isMaxedOut() {
        return totalCount >= totalMax;
    }

}
