package net.sourceforge.texlipse.builder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;


/**
 * Parses and analyzes latex output written to files with the extension '.fls'
 * when using the '-recorder' switch on the command line.
 *
 * @author Matthias Erll
 *
 */
public class FlsAnalyzer {

    private static final String WORKING_DIR_KEYWORD_STR = "PWD";
    private static final String INPUT_KEYWORD_STR = "INPUT";
    private static final String OUTPUT_KEYWORD_STR = "OUTPUT";

    private final IPath projectPath;
    private final IPath flsFilePath;
    private final Set<IPath> inputFiles;
    private final Set<IPath> outputFiles;
    private final boolean isWindowsPlatform;

    private IPath workingDir;

    /**
     * Determines if the given path is absolute in the file system or relative
     * to the current working directory.
     *
     * @param path path to check
     * @return <code>true</code> if path is absolute, <code>false<code>
     *  otherwise
     */
    private boolean isAbsolutePath(final String path) {
        if (path.length() < 2) {
            return false;
        }
        else if (isWindowsPlatform) {
            return path.charAt(1) == ':';
        }
        else {
            return path.charAt(0) == '/';
        }
    }

    /**
     * Extracts the file system object name from the given text line string and
     * turns it into an IPath relative to the project root. Absolute file names
     * are converted into project relative file names, if they are inside the
     * project folder structure. Relative file names are interpreted towards
     * the working directory (declared with <code>PWD ...</code>) inside the
     * FLS file. If the file or folder name is found to be lying outside of the
     * project folder structure, <code>null</code> is returned.
     *
     * @param prefix prefix to remove in the beginning of the text line
     * @param line entire text line to extract name from
     * @return project relative name of the file system object (can be file or
     *  folder), or <code>null</code> if invalid
     */
    private IPath extractProjectRelativeName(final String prefix,
            final String line) {
        if (prefix.length() + 2 < line.length()) {
            // skip prefix length plus one (space)
            final String name = line.substring(prefix.length() + 1);
            if (isAbsolutePath(name)) {
                final IPath absPath = new Path(name);
                // absolute path, attempt to make project relative
                if (projectPath.isPrefixOf(absPath)) {
                    return absPath.removeFirstSegments(projectPath.segmentCount()).setDevice(null);
                }
            }
            else if (workingDir != null) {
                // working dir relative path, make project relative
                final IPath relPath = workingDir.append(name);
                if (relPath.segmentCount() == 1
                        || !"..".equals(relPath.segment(0))) {
                    // do not allow higher level than project path
                    return relPath;
                }
            }
        }
        // return null if path is invalid or in any way cannot be converted
        // to a project relative path
        return null;
    }

    /**
     * Parses one line of text from the FLS file.
     *
     * @param line text line
     */
    private void processLine(final String line) {
        if (line != null && line.length() > 0) {
            if (line.startsWith(INPUT_KEYWORD_STR)) {
                final IPath newName = extractProjectRelativeName(INPUT_KEYWORD_STR, line);
                if (newName != null) {
                    inputFiles.add(newName);
                }
            }
            else if (line.startsWith(OUTPUT_KEYWORD_STR)) {
                final IPath newName = extractProjectRelativeName(OUTPUT_KEYWORD_STR, line);
                if (newName != null) {
                    outputFiles.add(newName);
                }
            }
            else if (line.startsWith(WORKING_DIR_KEYWORD_STR)) {
                final IPath newWorkingDir = extractProjectRelativeName(WORKING_DIR_KEYWORD_STR, line);
                if (newWorkingDir != null) {
                    workingDir = newWorkingDir;
                }
            }
        }
    }

    /**
     * Constructor.
     *
     * @param project current project
     * @param resource resource which is being built
     */
    public FlsAnalyzer(final IProject project, final IResource resource) {
        this.projectPath = project.getLocation();
        final IContainer sourceContainer = resource.getParent();
        final IPath flsDir = sourceContainer.getLocation();
        final String flsFileName = OutputFileManager.stripFileExt(
                resource.getName(), null).concat(".fls");
        this.flsFilePath = flsDir.append(flsFileName);
        this.inputFiles = new HashSet<IPath>();
        this.outputFiles = new HashSet<IPath>();
        this.isWindowsPlatform = Platform.getOS().equals(Platform.OS_WIN32);
        // Initialize working dir, but it is likely to be declared on the
        // first line of the FLS file
        this.workingDir = sourceContainer.getProjectRelativePath();
    }

    /**
     * Parses a the entire FLS file. It recognizes three types of commands:
     * <ul>
     * <li>PWD [Working dir]: Sets the working directory. Following file paths
     *  are treated as relative to this.
     * <li>INPUT [File path]: The file has been read by latex;</li>
     * <li>OUTPUT [File path]: The file has been written to by latex.</li>
     * </ul>
     * Input and output files are recorded in each one set. Every
     * file name is only added once per collection.
     *
     * @throws IOException if the FLS file cannot be read
     */
    public void parse() throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(flsFilePath.toFile()));
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line);
            }
        }
        catch (FileNotFoundException e) {
            throw e;
        }
        catch (IOException e) {
            try {
                br.close();
            } catch (IOException e1) {
            }
            throw e;
        }
    }

    /**
     * Clears the generated sets of input and output files.
     */
    public void clear() {
        inputFiles.clear();
        outputFiles.clear();
    }

    /**
     * Retrieves the set of input files as read from the FLS text file. All
     * file paths are relative to the project root.
     *
     * @return set of latex input files
     */
    public Set<IPath> getInputFiles() {
        return inputFiles;
    }

    /**
     * Retrieves the set of output files as read from the FLS text file. All
     * file paths are relative to the project root.
     *
     * @return set of latex output files
     */
    public Set<IPath> getOutputFiles() {
        return outputFiles;
    }

    /**
     * Retrieves the working directory read from the FLS text file, relative
     * to the project root. If no valid path has been read, this will point
     * to the source folder.
     *
     * @return working directory of the latex process
     */
    public IPath getWorkingDir() {
        return workingDir;
    }

}
