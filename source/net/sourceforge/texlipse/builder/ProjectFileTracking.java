package net.sourceforge.texlipse.builder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Tracks project files for maintaining information about user or LaTeX build
 * generated files, as well as the changes therein.
 *
 * @author Matthias Erll
 *
 */
public class ProjectFileTracking {

    private final IProject project;
    private final Set<IFolder> excludeFolders;

    private IFolder outputDir;
    private IFolder tempDir;

    private Set<IPath> tempDirNames;
    private Map<IPath, Long> buildDirNames;

    /**
     * Checks if the given file name has any of the extensions in
     * <code>ext</code>.
     *
     * @param name file name
     * @param ext array of potential file extensions to match
     * @return true, if any of the extensions matches
     */
    private static boolean hasMatchingExt(String name, String[] ext) {
        if (name != null) {
            for (String e : ext) {
                if (name.endsWith(e)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the given file name has any of the extensions in
     * <code>ext</code>. If so, the first matching extension is returned.
     *
     * @param name file name
     * @param ext array of potential file extensions to match
     * @return true, if any of the extensions matches
     */
    private static String getMatchingExt(String name, String[] ext) {
        if (ext != null) {
            for (String e : ext) {
                if (name.endsWith(e)) {
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the given file is a standard project file and should not be
     * messed with.
     *
     * @param name file name
     * @return true, if the file is a project file
     */
    private static boolean isProjectFile(String name) {
        if (name != null) {
            return ".project".equals(name) || ".texlipse".equals(name)
                    || hasMatchingExt(name, new String[] {".tex", ".cls",
                            ".sty", ".ltx"});
        }
        else {
            return false;
        }
    }

    /**
     * Check whether the given file has a temp file extension.
     * 
     * @param name file name
     * @param ext temp. file extensions
     * @param format build output format
     * @return true, if file has a temporary file extension or is
     *  an intermediate output file
     */
    private static boolean isTempFile(String name, String[] ext,
            String format) {
        return hasMatchingExt(name, ext)
            // dvi and ps can also be temporary files at this point
            // pdf can not, because nothing is generated from pdfs
                || (name.endsWith(".dvi") && !"dvi".equals(format))
                || (name.endsWith(".ps") && !"ps".equals(format));
    }

    /**
     * Checks if the given time stamp is newer than the reference time stamp
     * recorded in the snapshot of the build directory. The file is also
     * considered newer, if it had not been recorded before.
     *
     * @param name IPath reference to the file
     * @param currentTimestamp current time stamp of the file
     * @return true, if the file is new or newer than the snapshot; false
     *  otherwise
     */
    private boolean isNewer(IPath name, long currentTimestamp) {
        Long prevTimestamp = buildDirNames.get(name);
        return prevTimestamp == null
                || (prevTimestamp.longValue() < currentTimestamp);
    }

    /**
     * Recursively scans the given container for files and adds them to the
     * given map, along with their time stamps. This does not list the
     * folders it recurses into.
     *
     * @param container container to scan for files
     * @param nameMap map of file paths to put the files and time stamps into
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void recursiveScanFiles(final IContainer container,
            final Map<IPath, Long> nameMap, IProgressMonitor monitor)
                    throws CoreException {
        IResource[] res = container.members();
        for (IResource current : res) {
            if (current instanceof IFolder
                    && !excludeFolders.contains(current)) {
                // Recurse into subfolders
                IFolder subFolder = (IFolder) current;
                recursiveScanFiles(subFolder, nameMap, monitor);
            }
            else if (!isProjectFile(current.getName())) {
                Long timestamp = new Long(current.getModificationStamp());
                nameMap.put(current.getProjectRelativePath(), timestamp);
            }
            monitor.worked(1);
        }
    }

    /**
     * Generates a map of output files in the given folder, along with their
     * file extensions. The latter can be used for renaming the output files
     * later. Output files can be the main output file, a partial build output,
     * or a file with the same name, but different file extension as the source
     * file. This different extension has to be any of the ones passed in
     * <code>derivedExts</code>.
     *
     * @param aSourceContainer source container to scan for output files
     * @param sourceBaseName name without extension of the current source file
     * @param derivedExts derived file extensions
     * @param format current output format
     * @param monitor progress monitor
     * @return a map with output files (keys) and extensions (values)
     * @throws CoreException if an error occurs
     */
    public static Map<IPath, String> getOutputNames(IContainer aSourceContainer,
            String sourceBaseName, String[] derivedExts, String format,
            IProgressMonitor monitor) throws CoreException {
        final Map<IPath, String> outputNames = new HashMap<IPath, String>();
        final String dotFormat = '.' + format;
        final String currentOutput = sourceBaseName + dotFormat;
    
        for (IResource res : aSourceContainer.members()) {
            // Disregard subfolders
            if (res instanceof IFile) {
                String name = res.getName();
                if (name.equals(currentOutput)) {
                    outputNames.put(res.getProjectRelativePath(), dotFormat);
                }
                else {
                    String ext = getMatchingExt(name, derivedExts);
                    if (ext != null
                            && OutputFileManager.stripFileExt(name, ext).equals(sourceBaseName)) {
                        outputNames.put(res.getProjectRelativePath(), ext);
                    }
                }
            }
            monitor.worked(1);
        }
        return outputNames;
    }

    /**
     * Constructor.
     *
     * @param project current project
     */
    public ProjectFileTracking(final IProject project) {
        this.project = project;
        this.excludeFolders = new HashSet<IFolder>();
        init();
    }

    /**
     * (Re-)Initializes this instance and reads the current settings
     * from the project preferences.
     */
    public void init() {
        excludeFolders.clear();
        tempDirNames = null;
        buildDirNames = null;
        outputDir = TexlipseProperties.getProjectOutputDir(project);
        tempDir = TexlipseProperties.getProjectTempDir(project);
        excludeFolders.add(outputDir);
        excludeFolders.add(tempDir);
    }

    /**
     * Checks if snapshots have been created.
     *
     * @return true if snapshots exist, false otherwise
     */
    public boolean isInitial() {
        return tempDirNames == null || buildDirNames == null;
    }

    /**
     * Retrieves the current snapshot of temporary files.
     *
     * @param monitor progress monitor
     * @return a set of paths to all files in the current snapshot
     */
    public Set<IPath> getTempFiles() {
        return new HashSet<IPath>(tempDirNames);
    }

    /**
     * Retrieves the current contents of the temporary files folder.
     *
     * @return a set of paths to all files inside the temp folder
     * @throws CoreException if an error occurs
     */
    public Set<IPath> getTempFolderNames(IProgressMonitor monitor)
            throws CoreException {
        final Map<IPath, Long> tempDirFiles = new HashMap<IPath, Long>();
        if (tempDir != null && tempDir.exists()) {
            recursiveScanFiles(tempDir, tempDirFiles, monitor);
        }
        // We do not need the time stamps in this case.
        return tempDirFiles.keySet();
    }

    /**
     * Determines the temporary files, which have been added to or changed
     * within the source container during the last build. Temporary files
     * are defined by the file extensions given in <code>tempExts</code>.
     *
     * @param container source container to scan for new files
     * @param tempExts extensions of temporary files
     * @param monitor progress monitor
     * @return set of new temporary files
     * @throws CoreException if an error occurs
     */
    public Set<IPath> getNewTempNames(final IContainer container,
            final String[] tempExts, final String format,
            IProgressMonitor monitor) throws CoreException {
        Set<IPath> newNames = new HashSet<IPath>();
        Map<IPath, Long> currentNames = new HashMap<IPath, Long>();
        // Scan for current files in the build folder
        recursiveScanFiles(container, currentNames, monitor);
        for (Entry<IPath, Long> names : currentNames.entrySet()) {
            // Check which of the files are new, and if they are temporary files
            IPath name = names.getKey();
            Long timestamp = names.getValue();
            if (isTempFile(name.lastSegment(), tempExts, format)
                    && isNewer(name, timestamp.longValue())) {
                newNames.add(name);
            }
            monitor.worked(1);
        }
        return newNames;
    }

    /**
     * Memorizes two sets of files:
     * <ul>
     * <li>temporary files currently located in the temp. files folder</li>
     * <li>all files currently located in the source container</li>
     * </ul>
     * These can later be used to determine, which temporary files have been
     * added during a LaTeX build process.
     *
     * @param container source container
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void refreshSnapshots(final IContainer container,
            IProgressMonitor monitor) throws CoreException {
        tempDirNames = getTempFolderNames(monitor);

        final Map<IPath, Long> newBuildDirFiles = new HashMap<IPath, Long>();
        if (container != null && container.exists()) {
            recursiveScanFiles(container, newBuildDirFiles, monitor);
        }
        buildDirNames = newBuildDirFiles;
    }

    /**
     * Drops the snapshots of the temporary files directory and build directory.
     */
    public void clearSnapshots() {
        tempDirNames = null;
        buildDirNames = null;
    }

}
