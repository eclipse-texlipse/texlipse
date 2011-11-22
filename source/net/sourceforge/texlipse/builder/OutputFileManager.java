package net.sourceforge.texlipse.builder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;


/**
 * Performs actions on output files. This includes moving, renaming, or setting
 * derived file flags, as defined in the preferences.
 *
 * @author Matthias Erll
 *
 */
public class OutputFileManager {

    private final IProject project;
    private final ProjectFileTracking tracking;

    private IContainer sourceDir;
    private IFolder outputDir;
    private IFolder tempDir;
    private String format;
    private IFile sourceFile;
    private IFile currentSourceFile;

    private Set<IPath> movedFiles;

    /**
     * Moves a file to the output directory with a new name.
     * 
     * @param project the current project
     * @param sourceFile output file to be moved
     * @param destDir the destination directory of the file
     * @param destName the new name of the file
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     * @return file in the new location
     */
    private static IFile moveFile(IProject project, IFile sourceFile,
    		IContainer destContainer, String destName,
    		IProgressMonitor monitor) throws CoreException {
    	if (sourceFile != null && sourceFile.exists() && destName != null) {
    	    final IPath destRelPath = new Path(destName);
            final IFile dest = destContainer.getFile(destRelPath);

            if (dest.exists()) {
                File outFile = new File(sourceFile.getLocationURI());
                File destFile = new File(dest.getLocationURI());
                try {
                    // Try to move the content instead of deleting the old file
                    // and replace it by the new one. This is better for some
                    // viewers like Sumatrapdf
                    FileOutputStream out = new FileOutputStream(destFile);
                    out.getChannel().tryLock();
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(outFile));

                    byte[] buf = new byte[4096];
                    int l;
                    while ((l = in.read(buf)) != -1) {
                        out.write(buf, 0, l);
                    }
                    in.close();
                    out.close();
                    sourceFile.delete(true, monitor);
                } catch (IOException e) {
                    // try to delete and move the file
                    dest.delete(true, monitor);
                    sourceFile.move(dest.getFullPath(), true, monitor);
                }
            }
            else {
                // move the file
                sourceFile.move(dest.getFullPath(), true, monitor);
            }
            monitor.worked(1);
            return dest;
        }
    	else {
    	    return null;
    	}
    }

    /**
     * Retrieves an IFile object of the currently selected output file,
     * no matter if the file actually exists.
     * This method is used to respect partial builds.
     *
     * @return current output file, or null if not selected
     */
    private IFile getSelectedOutputFile() {
        String fileName = TexlipseProperties.getOutputFileName(project);
        if (fileName != null) {
            if (outputDir == null) {
                return project.getFile(fileName);
            }
            else {
                return outputDir.getFile(fileName);
            }
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves, and if necessary creates, the currently selected output
     * folder. If the output folder is not available, the project root is
     * used instead.
     * This method is used to respect partial builds.
     *
     * @param markAsDerived if set to true, the "derived" flag will be set
     *  for the folder
     * @param monitor progress monitor
     * @return output container
     * @throws CoreException if an error occurs
     */
    private IContainer getSelectedOutputContainer(boolean markAsDerived,
            IProgressMonitor monitor) throws CoreException {
        if (outputDir != null) {
            if (!outputDir.exists()) {
                outputDir.create(true, true, monitor);
            }
            if (markAsDerived) {
                outputDir.setDerived(true);
            }
            return outputDir;
        }
        else {
            // if not set, assume project directory
            return project;
        }
    }

    /**
     * Retrieves the IFile object of the actually used source file, no matter
     * if it actually exists.
     * This method is used to respect partial builds.
     *
     * @return actually selected source file, or null if no source file has
     *  been set
     */
    private IFile getActualSourceFile() {
        if (currentSourceFile == null) {
            return sourceFile;
        }
        else {
            return currentSourceFile;
        }
    }

    /**
     * Retrieves the IContainer object of the actually selected source file,
     * no matter if it actually exists.
     * This method is used to respect partial builds.
     *
     * @return current source file container, or null of no source file
     *  has been set
     */
    private IContainer getActualSourceContainer() {
        if (currentSourceFile == null) {
            if (sourceFile != null) {
                return sourceFile.getParent();
            }
            else {
                return null;
            }
        }
        else {
            return currentSourceFile.getParent();
        }
    }

    /**
     * Find the output file and get the local time stamp.
     *
     * @return the "last modified" -timestamp of the project output file,
     *  or -1 if file does not exist
     */
    private long getOutputFileDate() {
        IFile of = getSelectedOutputFile();
        if (of != null && of.exists()) {
            return of.getLocalTimeStamp();
        }
        else {
            return -1;
        }
    }

    /**
     * Moves a set of files from the source container to the destination. All
     * files need to be inside the source container or any of its
     * subfolders. Existing folders are not removed, if left empty. If source
     * and destination container are identical, files are if applicable only
     * marked as derived.
     *
     * @param source source container
     * @param dest destination folder (can be null, for only marking files as
     *  derived)
     * @param nameSet set of file paths to move
     * @param markAsDerived mark files as derived after moving
     * @param force overwrite exiting files and create subfolders in destination
     *  folder, if necessary
     * @param monitor progress monitor
     * @return a new set of file paths in their new location. This only includes
     *  files which have actually been moved.
     * @throws CoreException if an error occurs
     */
    private Set<IPath> moveFiles(final IContainer source, final IContainer dest,
            final Set<IPath> nameSet, boolean markAsDerived, boolean force,
            IProgressMonitor monitor) throws CoreException {
        Set<IPath> newNames = new HashSet<IPath>();
        if (nameSet != null && !nameSet.isEmpty()) {
            IPath sourcePath = source.getProjectRelativePath();
            IPath destPath;
            if (dest != null) {
                destPath = dest.getProjectRelativePath();
            }
            else {
                destPath = sourcePath;
            }
            boolean moveFiles = !sourcePath.equals(destPath);
            int sourceSeg = sourcePath.segmentCount();
            // Sort paths for running through file system structure incrementally
            IPath[] sortedNames = nameSet.toArray(new IPath[0]);
            Arrays.sort(sortedNames, new Comparator<IPath>() {
                public int compare(IPath o1, IPath o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });

            for (IPath filePath : sortedNames) {
                if (sourcePath.isPrefixOf(filePath)) {
                    IFile currentFile = project.getFile(filePath);
                    if (moveFiles) {
                        // Generate new path
                        IPath destFilePath = destPath.append(filePath.removeFirstSegments(sourceSeg));
                        IFile destFile = project.getFile(destFilePath);
                        if (currentFile.exists() && (force || !destFile.exists())) {
                            // Retrieve destination parent folder
                            IContainer destFolder = destFile.getParent();
                            if (destFolder instanceof IFolder && !destFolder.exists()
                                    && force) {
                                // Create destination folder if necessary
                                ((IFolder) destFolder).create(true, true, monitor);
                                if (markAsDerived) {
                                    destFolder.setDerived(true);
                                }
                            }
                            if (destFolder.exists()) {
                                // Move file
                                if (destFile.exists() && force) {
                                    destFile.delete(true, monitor);
                                }
                                currentFile.move(destFile.getFullPath(), true, monitor);
                                if (markAsDerived && destFile.exists()) {
                                    destFile.setDerived(true);
                                }
                                // Store path for later reversal
                                newNames.add(destFilePath);
                            }
                        }
                    }
                    else {
                        if (markAsDerived && currentFile.exists()) {
                            currentFile.setDerived(true);
                        }
                    }
                    monitor.worked(1);
                }
            }
        }
        return newNames;
    }

    /**
     * Deletes a set of files from the file system, and also their parent
     * folders if those become empty during this process.
     *
     * @param nameSet set of file paths
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void deleteFiles(final Set<IPath> nameSet,
            IProgressMonitor monitor) throws CoreException {
        if (nameSet == null || nameSet.isEmpty()) {
            return;
        }
        Set<IContainer> subFolders = new HashSet<IContainer>(); 
        for (IPath filePath : nameSet) {
            // Generate new path
            IFile currentFile = project.getFile(filePath);
            if (currentFile.exists()) {
                // Retrieve parent folder and store for deletion
                IContainer folder = currentFile.getParent();
                subFolders.add(folder);
                currentFile.delete(true, monitor);
            }
            monitor.worked(1);
        }
        // Delete parent folders, if they are empty
        for (IContainer folder : subFolders) {
            if (folder.exists() && folder.members().length == 0) {
                folder.delete(true, monitor);
            }
            monitor.worked(1);
        }
    }

    /**
     * Renames output files and/or moves them if necessary. A file is
     * considered an output file, if
     * <ul>
     * <li>it is the current output file (which can also be from a temporary
     *  build)</li>
     * <p><b>or</b></p>
     * <li>it has the same file name as the current input file, apart from its
     *  file extension, and one of the derived file extensions as specified in
     *  the preferences</li>
     * </ul>
     *
     * @param monitor progress monitor
     * @return set of paths to the (possibly moved) files
     * @throws CoreException if an error occurs
     */
    private Set<IPath> moveOutputFiles(IProgressMonitor monitor)
            throws CoreException {
        final boolean markAsDerived = "true".equals(
                TexlipseProperties.getProjectProperty(project,
                TexlipseProperties.MARK_OUTPUT_DERIVED_PROPERTY));
        final String[] derivedExts = TexlipsePlugin.getPreferenceArray(
                TexlipseProperties.DERIVED_FILES);

        final IFile aSourceFile = getActualSourceFile();
        final IContainer aSourceContainer = getActualSourceContainer();
        final IFile sOutputFile = getSelectedOutputFile();
        final IContainer sOutputContainer = getSelectedOutputContainer(markAsDerived,
                monitor);
        if (aSourceFile == null || aSourceContainer == null
                || sOutputFile == null || sOutputContainer == null) {
            // Something is wrong with the settings
            return null;
        }

        // Get name without extension from main files for renaming
        final String dotFormat = '.' + format;
        final String sourceBaseName = stripFileExt(aSourceFile.getName(), null);
        final String outputBaseName = stripFileExt(sOutputFile.getName(), dotFormat);

        // Check if files are to be moved or renamed
        final boolean moveFiles = !sourceBaseName.equals(outputBaseName)
                || !sOutputContainer.equals(aSourceContainer);
        // Retrieve output and other derived files along with their extensions
        final Map<IPath, String> outputFiles =
                ProjectFileTracking.getOutputNames(aSourceContainer,
                sourceBaseName, derivedExts, format, monitor);

        // Check if there is anything to do
        if ((moveFiles || markAsDerived) && !outputFiles.isEmpty()) {
            final Set<IPath> movedFiles = new HashSet<IPath>(outputFiles.size());

            project.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
                    // Move files to destination folder and rename
                    for (Entry<IPath, String> entry : outputFiles.entrySet()) {
                        IFile currentFile = project.getFile(entry.getKey());
                        if (moveFiles) {
                            // Determine new file name
                            String destName = outputBaseName + entry.getValue();
                            // Move file
                            IFile dest = moveFile(project, currentFile, sOutputContainer,
                                    destName, monitor);
                            if (dest != null && markAsDerived) {
                                dest.setDerived(true);
                            }
                            movedFiles.add(dest.getProjectRelativePath());
                        }
                        else {
                            // Possibly mark as derived
                            if (markAsDerived) {
                                currentFile.setDerived(true);
                            }
                            movedFiles.add(entry.getKey());
                        }
                    }
                }
            }, monitor);

            return movedFiles;
        }
        else {
            return outputFiles.keySet();
        }
    }

    /**
     * Moves temporary files out of the build directory, if applicable. A file
     * is considered a temporary file, if
     * <ul>
     * <li>it had been in the temporary files folder before the build
     *  process</li>
     * <p><b>or</b></p>
     * <li>it was created or modified during the build process, and has a
     *  temporary file extension as specified in the preferences</li>
     * </ul>
     *
     * @param excludes set of paths to exclude from moving, e.g. because they
     *  are the main output files
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void moveTempFiles(final Set<IPath> excludes, IProgressMonitor monitor)
            throws CoreException {
        final IContainer aSourceContainer = getActualSourceContainer();
        if (tracking.isInitial() || aSourceContainer == null
                || !aSourceContainer.exists()) {
            return;
        }

        final boolean markAsDerived = "true".equals(
                TexlipseProperties.getProjectProperty(project,
                        TexlipseProperties.MARK_TEMP_DERIVED_PROPERTY));
        final String[] tempExts = TexlipsePlugin.getPreferenceArray(
                TexlipseProperties.TEMP_FILE_EXTS);

        // Check if there is anything to do
        if (markAsDerived || tempDir != null) {
            // First move temporary files, which had been placed into the source folder
            // just prior to the build;
            // then check for new temporary files, which need to be moved
            project.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
                    if (movedFiles != null) {
                        if (excludes != null) {
                            movedFiles.removeAll(excludes);
                        }
                        moveFiles(sourceDir, tempDir, movedFiles, markAsDerived, true, monitor);
                    }
                    final Set<IPath> newTempNames = tracking.getNewTempNames(aSourceContainer,
                            tempExts, format, monitor);
                    if (excludes != null) {
                        newTempNames.removeAll(excludes);
                    }
                    moveFiles(sourceDir, tempDir, newTempNames, markAsDerived, true, monitor);
                }
            }, monitor);
        }
    }

    /**
     * Moves all files currently located in the temporary files folder into the
     * build directory
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void restoreTempFiles(IProgressMonitor monitor) throws CoreException {
        final Set<IPath> tempNames = tracking.getTempFiles();
        if (tempDir == null || tempNames.isEmpty()) {
            movedFiles = new HashSet<IPath>();
            return;
        }

        // Move files and store new paths for later reversal
        project.getWorkspace().run(new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                movedFiles = moveFiles(tempDir, sourceDir,
                        tracking.getTempFiles(), false, false, monitor);
            }
        }, monitor);
    }

    /**
     * Utility method for refreshing the current view on all relevant input and
     * output folders. This makes sure, that methods determining and moving files
     * get the current workspace contents.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void refreshView(IProgressMonitor monitor) throws CoreException {
        sourceDir.refreshLocal(IProject.DEPTH_INFINITE, monitor);
        if (outputDir != null
                && !sourceDir.getProjectRelativePath().isPrefixOf(outputDir.getProjectRelativePath())) {
            outputDir.refreshLocal(IProject.DEPTH_ONE, monitor);
        }
        if (tempDir != null
                && !sourceDir.getProjectRelativePath().isPrefixOf(tempDir.getProjectRelativePath())) {
            tempDir.refreshLocal(IProject.DEPTH_INFINITE, monitor);
        }
        if (!sourceDir.getLocation().equals(project.getLocation())) {
            project.refreshLocal(IProject.DEPTH_ONE, monitor);
        }
    }

    /**
     * Returns the given file name without the extension. The file extension
     * can be provided in <code>ext</code> or be determined automatically. If
     * the former applies, the extension(s) should start with a dot. The method
     * does not check for that condition. If determined automatically, the last
     * dot marks the beginning of the file extension (similar to
     * <code>getFileExtension()</code> of IFile). 
     *
     * @param name file name
     * @param ext suggested file extension (can be null)
     * @return the given file name without the extension
     */
    public static String stripFileExt(String name, String ext) {
        if (name != null) {
            if (ext == null) {
                int idx = name.lastIndexOf('.');
                if (idx > 0) {
                    return name.substring(0, idx);
                }
                else {
                    return name;
                }
            }
            else {
                int nameLen = name.length();
                int extLen = ext.length();
                if (nameLen > extLen) {
                    return name.substring(0, nameLen - extLen);
                }
                else {
                    return "";
                }
            }
        }
        else {
            return name;
        }
    }

    /**
     * Constructor.
     *
     * @param project current project
     * @param tracking file tracking for the project
     */
    public OutputFileManager(final IProject project,
            final ProjectFileTracking tracking) {
        this.project = project;
        this.tracking = tracking;
        this.init();
    }

    /**
     * Initializes variables, which are often reused. This should be called
     * every time there is a chance that project settings have been changed.
     */
    public void init() {
        sourceDir = TexlipseProperties.getProjectSourceDir(project);
        outputDir = TexlipseProperties.getProjectOutputDir(project);
        tempDir = TexlipseProperties.getProjectTempDir(project);
        format = TexlipseProperties.getProjectProperty(project,
                TexlipseProperties.OUTPUT_FORMAT);
        sourceFile = TexlipseProperties.getProjectSourceFile(project);
    }

    /**
     * Performs actions before a LaTeX document is built; namely:
     * <ul>
     * <li>memorizing which files are present in the temporary and build source
     *  folder, and</li>
     * <li>moving temporary files from their folder into the build folder, so
     *  the build process has access to them.</li>
     * </ul>
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void performBeforeBuild(IProgressMonitor monitor) throws CoreException {
        // capture current state of build and temp folder
        tracking.refreshSnapshots(sourceDir, monitor);

        // use temp files from previous build
        restoreTempFiles(monitor);
    }

    /**
     * Performs actions after the LaTeX builder has finished building a document
     * for the current source; namely:
     * <ul>
     * <li>renaming and/or moving output (and other derived) files out of the
     *  build directory into the output folder</li>
     * <li>moving old and new temporary files out of the build directory into
     *  the temporary files folder</li>
     * </ul>
     *
     * @param inputFile name of the input file; this can be <code>null</code>,
     *  if the current main document has just been built, but should be set
     *  after partial builds
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void performAfterBuild(IProgressMonitor monitor)
            throws CoreException {
        // keeping first exception, which occurs when moving files; however, attempts
        // to still perform following steps
        CoreException ex = null;

        // make sure this has access to all files (if this fails, it means trouble to
        // all following steps)
        refreshView(monitor);

        Set<IPath> outputFiles = null;
        try { // possibly move output files away from the source dir and mark as derived
            outputFiles = moveOutputFiles(monitor);
        } catch (CoreException e) {
            // store exception for throwing it later
            ex = new BuilderCoreException(TexlipsePlugin.stat(
                    TexlipsePlugin.getResourceString("builderCoreErrorOutputBlock")));
        }

        try { // move temp files out of this folder and mark as derived
            moveTempFiles(outputFiles, monitor);
        } catch (CoreException e) {
            // we only worry about this one, if the build was okay
            if (ex == null) {
                ex = new BuilderCoreException(TexlipsePlugin.stat(
                        TexlipsePlugin.getResourceString("builderCoreErrorTempBlock")));
            }
        }

        try {
            refreshView(monitor);
        } catch (CoreException e) {
            // this is not irrelevant, but not as severe as the others
            if (ex == null) {
                ex = e;
            }
        }

        tracking.clearSnapshots();
        // now throw any pending exception, after cleaning up
        if (ex != null) {
            throw ex;
        }
    }

    /**
     * Deletes the output file.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void cleanOutputFile(IProgressMonitor monitor) throws CoreException {
        monitor.subTask(TexlipsePlugin.getResourceString("builderSubTaskCleanOutput"));

        IFile outputFile = getSelectedOutputFile(); 
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete(true, monitor);
        }

        monitor.worked(1);
    }

    /**
     * Deletes the contents of the temporary files folder, including subfolders
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void cleanTempFiles(IProgressMonitor monitor) throws CoreException {
        if (tempDir != null && tempDir.exists()) {
            monitor.beginTask(TexlipsePlugin.getResourceString("builderSubTaskClean"),
                    tempDir.members().length);
            monitor.subTask(TexlipsePlugin.getResourceString("builderSubTaskCleanTemp"));

            // Retrieve current temp folder content
            final Set<IPath> currentTmpFiles = tracking.getTempFolderNames(monitor);

            // Perform deletion
            deleteFiles(currentTmpFiles, monitor);
        }
        tracking.clearSnapshots();
    }

    /**
     * Determines, if the current output file is up to date (i.e. all source
     * files are older). This method is aware of partial builds.
     * 
     * @return true, if the output file does not need to be rebuilt; false
     *  if it should
     */
    public boolean isUpToDate() {
        long lastBuildStamp = getOutputFileDate();

        IResource[] files = TexlipseProperties.getAllProjectFiles(project);
        for (int i = 0; i < files.length; i++) {
            long stamp = files[i].getLocalTimeStamp(); 
            if (stamp > lastBuildStamp) {
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieves the currently set source file.
     *
     * @return source file
     */
    public IFile getCurrentSourceFile() {
        return currentSourceFile;
    }

    /**
     * Sets the current main source file for the project. This method does not
     * check, if it exists. If <code>null</code> is passed, the project's main
     * input file is assumed. Therefore, it should be set to the temporary
     * input for partial builds.
     *
     * @param sourceFile source file
     */
    public void setCurrentSourceFile(IFile sourceFile) {
        this.currentSourceFile = sourceFile;
    }

}
