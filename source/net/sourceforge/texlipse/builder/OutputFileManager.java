package net.sourceforge.texlipse.builder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
 * Tracks project files for maintaining information about user or LaTeX build
 * generated files. Also moves files and sets the derived file flag, as defined
 * in the preferences.
 *
 * @author Matthias Erll
 *
 */
public class OutputFileManager {

    private final IProject project;

    private Set<IFolder> excludeFolders;
    private String format;
    private IContainer sourceDir;
    private IFile sourceFile;
    private IFile currentSourceFile;
    private IFolder outputDir;
    private IFolder tempDir;
    private Set<IPath> tempDirNames;
    private Set<IPath> buildDirNames;

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
    private static String stripFileExt(String name, String ext) {
        if (name != null) {
            if (ext == null) {
                int idx = name.lastIndexOf('.');
                if (idx > 0) {
                    return name.substring(0, idx - 1);
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
     * Recursively scans the given container for files and adds them to the
     * given set. This does not list the folders it recurses into.
     *
     * @param container container to scan for files
     * @param nameSet set of file paths to add list the files in
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void recursiveScanFiles(final IContainer container,
            final Set<IPath> nameSet, IProgressMonitor monitor)
                    throws CoreException {
        IResource[] res = container.members();
        for (IResource current : res) {
            if (current instanceof IFolder
                    && !excludeFolders.contains(current)) {
                // Recurse into subfolders
                IFolder subFolder = (IFolder) current;
                recursiveScanFiles(subFolder, nameSet, monitor);
            }
            else if (!isProjectFile(current.getName())) {
                nameSet.add(current.getProjectRelativePath());
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
     * @param monitor progress monitor
     * @return a map with output files (keys) and extensions (values)
     * @throws CoreException if an error occurs
     */
    private Map<IPath, String> getOutputNames(IContainer aSourceContainer,
            String sourceBaseName, String[] derivedExts,
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
                            && stripFileExt(name, ext).equals(sourceBaseName)) {
                        outputNames.put(res.getProjectRelativePath(), ext);
                    }
                }
            }
            monitor.worked(1);
        }
        return outputNames;
    }

    /**
     * Determines the temporary files, which have been added to the source
     * container during the last build. Temporary files are defined by the
     * file extensions given in <code>tempExts</code>.
     *
     * @param container source container to scan for new files
     * @param tempExts extensions of temporary files
     * @param monitor progress monitor
     * @return set of new temporary files
     * @throws CoreException if an error occurs
     */
    private Set<IPath> getNewTempNames(final IContainer container,
            final String[] tempExts, IProgressMonitor monitor)
                    throws CoreException {
        Set<IPath> newNames = new HashSet<IPath>();
        Set<IPath> currentNames = new HashSet<IPath>();
        // Scan for current files in the build folder
        recursiveScanFiles(container, currentNames, monitor);
        for (IPath name : currentNames) {
            // Check which of the files are new, and if they are temporary files
            if (!buildDirNames.contains(name)
                    && isTempFile(name.lastSegment(), tempExts, format)) {
                newNames.add(name);
            }
            monitor.worked(1);
        }
        return newNames;
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
                outputDir.setDerived(true, monitor);
            }
            return outputDir;
        }
        else {
            // if not set, assume project directory
            return project;
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
     * subfolders. Existing folders are not removed, if left empty.
     *
     * @param source source container
     * @param dest destination folder
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
            IPath destPath = dest.getProjectRelativePath();
            int sourceSeg = sourcePath.segmentCount();
            // Sort paths for running through file system structure incrementally
            IPath[] sortedNames = nameSet.toArray(new IPath[0]);
            Arrays.sort(sortedNames, new Comparator<IPath>() {
                public int compare(IPath o1, IPath o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });
    
            for (IPath filePath : sortedNames) {
                // Generate new path
                if (sourcePath.isPrefixOf(filePath)) {
                    IFile currentFile = project.getFile(filePath);
                    IPath destFilePath = destPath.append(filePath.removeFirstSegments(sourceSeg));
                    IFile destFile = project.getFile(destFilePath);
                    if (currentFile.exists() && !filePath.equals(destFilePath)
                            && (force || !destFile.exists())) {
                        // Retrieve destination parent folder
                        IContainer destFolder = destFile.getParent();
                        if (destFolder instanceof IFolder && !destFolder.exists()
                                && force) {
                            // Create destination folder if necessary
                            ((IFolder) destFolder).create(true, true, monitor);
                            if (markAsDerived) {
                                destFolder.setDerived(true, monitor);
                            }
                        }
                        if (destFolder.exists()) {
                            // Move file
                            currentFile.move(destFile.getFullPath(), false, monitor);
                            if (markAsDerived && destFile.exists()) {
                                destFile.setDerived(true, monitor);
                            }
                            // Store path for later reversal
                            newNames.add(destFilePath);
                        }
                    }
                }
                monitor.worked(1);
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
     * Memorizes two sets of files:
     * <ul>
     * <li>temporary files currently located in the temp. files folder</li>
     * <li>all files currently located in the source container</li>
     * </ul>
     * These can later be used to determine, which temporary files have been
     * added during a LaTeX build process.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void refreshSnapshots(IProgressMonitor monitor) throws CoreException {
        final Set<IPath> newTempDirFiles = new HashSet<IPath>();
        if (tempDir != null && tempDir.exists()) {
            recursiveScanFiles(tempDir, newTempDirFiles, monitor);
        }
        tempDirNames = newTempDirFiles;
    
        final Set<IPath> newBuildDirFiles = new HashSet<IPath>();
        if (sourceDir != null && sourceDir.exists()) {
            recursiveScanFiles(sourceDir, newBuildDirFiles, monitor);
        }
        buildDirNames = newBuildDirFiles;
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
     * @throws CoreException if an error occurs
     */
    private void moveOutputFiles(IProgressMonitor monitor)
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
            return;
        }

        // Get name without extension from main files for renaming
        final String dotFormat = '.' + format;
        final String sourceBaseName = stripFileExt(aSourceFile.getName(), dotFormat);
        final String outputBaseName = stripFileExt(sOutputFile.getName(), dotFormat);

        // Check if files are to be moved or renamed
        final boolean moveFiles = !sourceBaseName.equals(outputBaseName)
                || !sOutputContainer.equals(aSourceContainer);
        // Check if there is anything to do
        if (moveFiles || markAsDerived) {
            project.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException {
                    // Retrieve output and other derived files along with their extensions
                    final Map<IPath, String> outputFiles = getOutputNames(aSourceContainer,
                            sourceBaseName, derivedExts, monitor);

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
                                dest.setDerived(true, monitor);
                            }
                        }
                        else {
                            // Possibly mark as derived
                            if (markAsDerived) {
                                currentFile.setDerived(true, monitor);
                            }
                        }
                    }
                }
            }, monitor);    
        }
    }

    /**
     * Moves temporary files out of the build directory, if applicable. A file
     * is considered a temporary file, if
     * <ul>
     * <li>it had been in the temporary files folder before the build
     *  process</li>
     * <p><b>or</b></p>
     * <li>it was created during the build process, and has a temporary file
     *  extension as specified in the preferences</li>
     * </ul>
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void moveTempFiles(IProgressMonitor monitor) throws CoreException {
        final IContainer aSourceContainer = getActualSourceContainer();
        if (tempDirNames == null || buildDirNames == null
                || tempDir == null || aSourceContainer == null ||
                !aSourceContainer.exists()) {
            return;
        }
    
        final boolean markAsDerived = "true".equals(
                TexlipseProperties.getProjectProperty(project,
                        TexlipseProperties.MARK_TEMP_DERIVED_PROPERTY));
        final String[] tempExts = TexlipsePlugin.getPreferenceArray(
                TexlipseProperties.TEMP_FILE_EXTS);
    
        // First move temporary files, which had been placed into the source folder
        // just prior to the build;
        // then check for new temporary files, which need to be moved
        project.getWorkspace().run(new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {        
                moveFiles(sourceDir, tempDir, tempDirNames, markAsDerived, true, monitor);
                Set<IPath> newTempNames = getNewTempNames(aSourceContainer,
                        tempExts, monitor);
                moveFiles(sourceDir, tempDir, newTempNames, markAsDerived, true, monitor);
            }
        }, monitor);
    }

    /**
     * Moves all files currently located in the temporary files folder into the
     * build directory
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void restoreTempFiles(IProgressMonitor monitor) throws CoreException {
        if (tempDir == null || tempDirNames == null
                || tempDirNames.isEmpty()) {
            return;
        }
    
        // Move files and store new paths for later reversal
        final Set<IPath> movedFiles = new HashSet<IPath>();
        project.getWorkspace().run(new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {        
                final Set<IPath> newSet = moveFiles(tempDir, sourceDir,
                        tempDirNames, false, false, monitor);
                if (newSet != null) {
                    movedFiles.addAll(newSet);
                }
            }
        }, monitor);
        tempDirNames = movedFiles;
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
     * Constructor.
     *
     * @param project current project
     */
    public OutputFileManager(final IProject project) {
        this.project = project;
        this.init();
    }

    /**
     * Initializes variables, which are often reused. This should be called
     * every time there is a chance that project settings have been changed.
     */
    public void init() {
        excludeFolders = new HashSet<IFolder>();
        sourceDir = TexlipseProperties.getProjectSourceDir(project);
        sourceFile = TexlipseProperties.getProjectSourceFile(project);
        outputDir = TexlipseProperties.getProjectOutputDir(project);
        tempDir = TexlipseProperties.getProjectTempDir(project);
        excludeFolders.add(outputDir);
        excludeFolders.add(tempDir);
        format = TexlipseProperties.getProjectProperty(project,
                TexlipseProperties.OUTPUT_FORMAT);
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
        refreshSnapshots(monitor);

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

        try { // possibly move output files away from the source dir and mark as derived
            moveOutputFiles(monitor);
        } catch (CoreException e) {
            // store exception for throwing it later
            ex = new BuilderCoreException(TexlipsePlugin.stat(
                    TexlipsePlugin.getResourceString("builderCoreErrorOutputBlock")));
        }

        try { // move temp files out of this folder and mark as derived
            moveTempFiles(monitor);
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
            final Set<IPath> currentTmpFiles = new HashSet<IPath>();
            recursiveScanFiles(tempDir, currentTmpFiles, monitor);
    
            // Perform deletion
            deleteFiles(currentTmpFiles, monitor);
        }
        tempDirNames = null;
        buildDirNames = null;
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
