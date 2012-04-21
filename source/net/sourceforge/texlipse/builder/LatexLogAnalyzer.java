
package net.sourceforge.texlipse.builder;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;


/**
 * Analyzes the latex output log for error and warning messages, indications for required
 * additional cycles of latex and bibliography runners, and input files.
 *
 * @author Matthias Erll
 * @author Kimmo Karlsson
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public class LatexLogAnalyzer {

    private static enum FollowingItem {
        LATEX_ERROR_MSG,
        PACKAGE_WARNING_MSG,
        PDFTEX_WARNING_MSG,
        LINE_NUMBER_ERROR,
        LINE_NUMBER_WARNING,
        ERROR_VERB };

    private static final int MAX_LINE_LENGTH = 79;
    private static final int BUFFER_LENGTH = 5 * MAX_LINE_LENGTH;

    private static final String MULTI_SPACE_STR = " {2,}";
    private static final String LATEX_C_ERROR_STR = "^(.+?\\.\\w{3}):(\\d+): (.+)$";
    private static final String TEX_ERROR_STR = "^!\\s+(.*)$";
    private static final String BOX_WARNING_STR = "^(Over|Under)full \\\\[hv]box .* at lines? (\\d+)-?-?(\\d+)?";
    private static final String GENERAL_WARNING_STR = "^(.+)?\\s?[Ww]arning.*?: (.*)$";
    private static final String AT_LINE_ERROR_STR = "^l\\.(\\d+) (.*?)(\\s+(.*))?$";
    private static final String AT_LINE_WARNING_STR = ".* line (\\d+).*";
    private static final String NO_BIB_FILE_STR = "^No file .+\\.bbl\\.$";
    private static final String NO_TOC_FILE_STR = "^No file .+\\.toc\\.$";
    private static final String MISSING_CHAR_INSERTED_STR = "^(Missing .+? inserted|Extra .+?, or forgotten .+?).*";
    private static final char[] GRAPHIC_USE_STR = { 'u', 's', 'e', ' ' };

    private static final Pattern P_MULTI_SPACE = Pattern.compile(MULTI_SPACE_STR);
    private static final Pattern P_LATEX_C_ERROR = Pattern.compile(LATEX_C_ERROR_STR);
    private static final Pattern P_TEX_ERROR = Pattern.compile(TEX_ERROR_STR);
    private static final Pattern P_FULL_BOX = Pattern.compile(BOX_WARNING_STR);
    private static final Pattern P_WARNING = Pattern.compile(GENERAL_WARNING_STR);
    private static final Pattern P_AT_LINE_ERROR =  Pattern.compile(AT_LINE_ERROR_STR);
    private static final Pattern P_AT_LINE_WARNING = Pattern.compile(AT_LINE_WARNING_STR);
    private static final Pattern P_MISSING_CHAR_INSERTED = Pattern.compile(MISSING_CHAR_INSERTED_STR);
    private static final Pattern P_NO_BIB_FILE = Pattern.compile(NO_BIB_FILE_STR);
    private static final Pattern P_NO_TOC_FILE = Pattern.compile(NO_TOC_FILE_STR);

    private final IProject project;
    private final IResource mainFile;
    private final IPath projectPath;
    private final IPath workingDir;
    private final boolean isWindowsPlatform;
    private final Stack<String> fileStack;
    private final Set<IPath> inputFiles;
    private final Set<String> externalNames;

    private String currentFile;
    private StringBuffer currentLine;
    private boolean parsingStackErrors;
    private boolean latexRerun;
    private boolean bibRerun;
    private boolean citeNotFound;
    private boolean errors;
    private boolean warnings;

    private FollowingItem following;
    private int skip;
    private String packageName;
    private String message;
    private Integer lineNumber;
    private int severity;

    /**
     * Replaces multiple spaces with one, and removes all space characters
     * at the beginning and end of the given string.
     *
     * @param line line string
     * @return modified string
     */
    private static String trimLine(final String line) {
        return P_MULTI_SPACE.matcher(line).replaceAll(" ").trim();
    }

    /**
     * Checks if the given character could be a valid file name character. This
     * does not exclude all characters which are invalid in LaTeX files!
     *
     * @param c int value of character
     * @return <code>true</code> if the character could be valid for a file name,
     *  otherwise <code>false</code>
     */
    private static boolean isValidFileNameChar(int c) {
        final int[] invalidChars = { '*', '?', '"', '<', '>', '|' };
        for (int x : invalidChars) {
            if (c == x) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given character could be the initial character indicating
     * an input file. This includes a dot (for relative characters), a letter,
     * or - for unix platforms - a forward slash.
     *
     * @param c int value of character
     * @return <code>true</code> if the character could indicate a file name,
     *  otherwise <code>false</code>
     */
    private boolean isPotentialInitialFileChar(int c) {
        return c == '.'
                || Character.isLetter(c)
                || (c == '/' || c == '\\');
    }

    /**
     * Determines if the given path is absolute in the file system or relative
     * to the project directory.
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
     * the working directory (the built resource's parent folder). If the file
     * or folder name is found to be lying outside of the
     * project folder structure, <code>null</code> is returned.
     *
     * @param prefix prefix to remove in the beginning of the text line
     * @param name text to extract name from
     * @return project relative name of the file system object (can be file or
     *  folder), or <code>null</code> if invalid
     */
    private IPath convertToProjectRelativeName(final String name) {
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
        // return null if path is invalid or in any way cannot be converted
        // to a project relative path
        return null;
    }

    /**
     * Retrieves the project resource addressed by the given string,
     * which must be a project relative path.
     *
     * @param fileStr relative path string
     * @return resource object
     */
    private IResource getProjectFile(final String fileStr) {
        if (fileStr != null) {
            IPath path = convertToProjectRelativeName(fileStr);
            if (path != null) {
                return project.getFile(path);
            }
        }
        return null;
    }

    /**
     * Parses a number from a string and returns it as an Integer object. Does
     * not throw and exception, but returns <code>null</code> if the string does
     * not represent a valid number.
     *
     * @param str string to parse
     * @return Integer object
     */
    private Integer getLineNumber(final String str) {
        try {
            return Integer.valueOf(str);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Reports an error with the given message on the current resource. If the
     * current resource cannot be determined, the line number argument is ignored,
     * and the error is reported on the project's main resource.
     *
     * @param message error message
     * @param lineNo line number
     */
    private void reportError(final String message, Integer lineNo) {
        final IResource res = getLastValidResource();
        if (res != null) {
            AbstractProgramRunner.createMarker(res, lineNo, message);
        }
        else {
            AbstractProgramRunner.createMarker(mainFile, -1, message);
        }
        errors = true;
    }

    /**
     * Reports a layout warning with the given message on the current resource. If the
     * current resource cannot be determined, the line number argument is ignored,
     * and the warning is reported on the project's main resource.
     *
     * @param message warning message
     * @param lineNo line number
     */
    private void reportLayoutWarning(final String message, Integer lineNo) {
        final IResource res = getLastValidResource();
        if (res != null) {
            AbstractProgramRunner.createLayoutMarker(res, lineNo, message);
        }
        else {
            AbstractProgramRunner.createLayoutMarker(mainFile, -1, message);
        }
        warnings = true;
    }

    /**
     * Reports a pending error or warning message, where information had to
     * be gathered across multiple lines of the log. Afterwards, the information about
     * pending messages is cleared.
     */
    private void processUnreported() {
        final IResource res = getLastValidResource();
        if (res != null) {
            AbstractProgramRunner.createMarker(res, lineNumber, message, severity);
        }
        else {
            AbstractProgramRunner.createMarker(mainFile, -1, message, severity);
        }
        if (severity == IMarker.SEVERITY_ERROR) {
            errors = true;
        }
        else {
            warnings = true;
        }
        lineNumber = null;
        message = null;
        severity = 0;
    }

    /**
     * Returns the last detected project resource, searching from the top of the input
     * file stack. If no valid resource can be determined, <code>null</code> is returned.
     *
     * @return project resource, which was most recently found
     */
    private IResource getLastValidResource() {
        IResource res = getProjectFile(currentFile);
        if (res != null && res.exists()) {
            return res;
        }
        ListIterator<String> i = fileStack.listIterator(fileStack.size());
        String fileName;
        while (i.hasPrevious()) {
            fileName = i.previous();
            res = getProjectFile(fileName);
            if (res != null && res.exists()) {
                return res;
            }
        }
        return null;
    }

    /**
     * Adds a file to the list of input paths, and also to the input files,
     * provided that it is a valid project resource. Depending on the
     * <code>push</code> parameter also adds the file to the stack of input
     * files (not checking for validity).
     *
     * @param fileName file name, project relative or absolute
     * @param push set to <code>true</code>, if the file should now be the current
     *  file on top of the input file stack
     */
    private void addFile(final String fileName, boolean push) {
        final IPath path = convertToProjectRelativeName(fileName);
        if (path != null) {
            inputFiles.add(path);
        }
        else {
            externalNames.add(fileName);
        }
        if (push) {
            fileStack.push(fileName);
            currentFile = fileName;
        }
    }

    /**
     * Removes the most recent input file from the input file stack, making the previous
     * file the current resource again.
     */
    private void popFile() {
        if (!fileStack.isEmpty()) {
            fileStack.pop();
            if (!fileStack.isEmpty()) {
                currentFile = fileStack.peek();
            }
            else {
                currentFile = null;
            }
        }
        else {
            parsingStackErrors = true;
        }
    }

    /**
     * Parses the current line for a graphic input file from the graphicx package,
     * which is indicated by a string in the format <code>&lt;use [file name]&gt;</code>.
     *
     * @param reader pushback reader
     * @return <code>true</code> if a potential graphic file name was found, <code>false</code>
     *  if the string ended prematurely.
     * @throws IOException should not occur, since we are not parsing a file; named in throws
     *  to handle in the calling method's catch block
     */
    private boolean parseGraphicName(PushbackReader reader) throws IOException {
        int i = -1;
        int c;
        do {
            c = reader.read();
            i++;
        } while (c == GRAPHIC_USE_STR[i] && i < GRAPHIC_USE_STR.length - 1);
        if (c == GRAPHIC_USE_STR[i]) {
            StringBuffer sb = new StringBuffer();
            boolean isValid = true;
            boolean done = false;
            do {
                c = reader.read();
                if (c == -1) {
                    isValid = false;
                }
                else if (c == '>') {
                    done = true;
                }
                else {
                    isValid = isValidFileNameChar(c);
                    sb.append((char) c);
                }
            } while (!done && isValid);
            if (isValid) {
                addFile(sb.toString(), false);
            }
            return true;
        }
        else {
            for (int j = i - 1; j >= 0; j--) {
                reader.unread(GRAPHIC_USE_STR[j]);
            }
            reader.unread(c);
            return false;
        }
    }

    /**
     * Parses the current line for an input file. Files are considered potential input
     * files if they satisfy the following criteria:
     * <ul>
     * <li>They are displayed either as project relative or absolute file names,
     *  which implies that at the latest, the third character must be a path
     *  delimiter.</li>
     * <li>They have a file extension between 1 and 9 characters.</li>
     * <li>Their file name can contain all valid file name characters. Spaces are valid
     *  in directory names, but are also considered as indicating the end of a file name
     *  when found after a file extension dot.</li>
     * <li>File names can contain parentheses, but file extensions cannot.</li>
     * </ul>
     *
     * If a potentially valid file name is found, it is added to the set of input files.
     * Unless the file name was followed by a closing parenthesis, this is also the new
     * current input file and added to the stack.
     *
     * If the initial character does not appear to be pointing to a file, it is pushed
     * back into the buffer and this method returns. If more characters are read without
     * any results, they are not pushed back.
     *
     * @param reader pushback reader
     * @return <code>true</code> if a file name was detected, <code>false</code>
     *  otherwise. In the latter case, the parser should forward to the next closing
     *  parenthesis.
     * @throws IOException should not occur, since we are not parsing a file; named in throws
     *  to handle in the calling method's catch block
     */
    private boolean parseSingleFileName(PushbackReader reader) throws IOException {
        int c = reader.read();
        if (c > -1 && isPotentialInitialFileChar(c)) {
            boolean isValid = true;
            boolean done = false;
            boolean push = true;
            boolean hasPathDelim = c == '/' || c == '\\';
            int fileExtLen = -1;
            StringBuffer sb = new StringBuffer();
            sb.append((char) c);

            do {
                c = reader.read();
                if (c == -1) {
                    done = true;
                    isValid = hasPathDelim && fileExtLen > 0 && fileExtLen < 10;
                }
                else if (c == ' '
                        && fileExtLen > 0 && fileExtLen < 10) {
                    done = true;
                }
                else if ((c == '(' || c == ')')
                        && fileExtLen > 0 && fileExtLen < 10) {
                    done = true;
                    if (c == '(') {
                        reader.unread(c);
                    }
                    else {
                        push = false;
                    }
                }
                else {
                    if (c == '.') {
                        fileExtLen = 0;
                    }
                    else if (c == '/' || c == '\\') {
                        fileExtLen = -1;
                        hasPathDelim = true;
                    }
                    else if (fileExtLen > -1) {
                        fileExtLen++;
                        isValid = isValidFileNameChar(c) && c != ':';
                    }
                    else if (c == '(' || c == ')' || c == ' ') {
                        isValid = hasPathDelim;
                    }
                    else {
                        isValid = isValidFileNameChar(c)
                                && (c != ':' || (isWindowsPlatform && sb.length() == 1))
                                && (hasPathDelim || sb.length() < 3);
                    }
                    sb.append((char) c);
                }
            } while (!done && isValid);
            if (isValid) {
                addFile(sb.toString(), push);
            }
            return isValid;
        }
        else {
            reader.unread(c);
            return false;
        }
    }

    /**
     * Parses the current line for input files, either indicated by latex or the
     * graphicx package.
     */
    private void parseFileNames() {
        PushbackReader reader = new PushbackReader(
                new StringReader(currentLine.toString()), 5);
        try {
            int c = reader.read();
            while (c > -1) {
                if (c == '<') {
                    if (!parseGraphicName(reader)) {
                        do {
                            c = reader.read();
                        } while (c > -1 && c != '>');
                    }
                }
                else if (c == '(') {
                    if (!parseSingleFileName(reader)) {
                        do {
                            c = reader.read();
                        } while (c > -1 && c != ')');
                    }
                }
                else if (c == ')') {
                    popFile();
                }
                c = reader.read();
            }
            reader.close();
        }
        catch (IOException e) {
            // Should not be relevant, as we are parsing a buffered string.
        }
    }

    /**
     * Processes the current line in the latex log output, performing tasks in the
     * following priority, and returning if the first one applies:
     * <ul>
     * <li>If there are lines pending that should be skipped, this method does that
     *  and decrements the skip counter.</li>
     * <li>If there are pending messages waiting for more input, these are combined with
     *  the contents of the current line, and if applicable reported.</li>
     * <li>A number of regular expression patterns are applied for checking, if the
     *  current line represents a commonly-known log message that should be displayed
     *  in Eclipse's "Problems" view.</li>
     * <li>If none of the aforementioned conditions applies, the line is checked for
     *  names of input files.</li>
     * </ul>
     *
     * @return <code>true</code> if the current line was "complete", which means that
     *  the line did or did not provide any relevant information, but did not produce
     *  any unreported messages; <code>false</code> if a pending message is waiting for
     *  more information to be reported later.
     */
    private boolean processLine() {
        if (skip > 0) {
            skip--;
            return true;
        }
        final String line = currentLine.toString();
        if (following != null) {
            final Matcher ml;
            switch (following) {
            case LATEX_ERROR_MSG:
                if (Character.isLowerCase(line.charAt(0))) {
                    message += ' ' + line;
                }
                break;
            case PACKAGE_WARNING_MSG:
                if (line.startsWith(packageName)) {
                    final String followMsg = line.substring(packageName.length()).trim();
                    if (followMsg.length() > 0) {
                        message += ' ' + followMsg;
                    }
                    return false;
                }
                else {
                    packageName = null;
                }
                break;
            case PDFTEX_WARNING_MSG:
                final String followMsg = line.trim();
                if (followMsg.length() > 0) {
                    message += ' ' + followMsg;
                    return false;
                }
                break;
            case LINE_NUMBER_ERROR:
                ml = P_AT_LINE_ERROR.matcher(line);
                if (ml.matches()) {
                    lineNumber = getLineNumber(ml.group(1));
                    message += ml.group(2);
                    final String part2 = ml.group(4);
                    if (part2 != null) { 
                        if (part2.length() > 0) {
                            message += " (followed by: " + part2 + ")";
                        }
                    }
                    else {
                        // Verbatim text with additional info should be on the next line
                        following = FollowingItem.ERROR_VERB;
                        return false;
                    }
                }
                break;
            case LINE_NUMBER_WARNING:
                ml = P_AT_LINE_WARNING.matcher(line);
                if (ml.matches()) {
                    lineNumber = getLineNumber(ml.group(1));
                    skip = 1;
                }
                break;
            case ERROR_VERB:
                message += " (followed by: " + line + ")";
                break;
            }
            processUnreported();
            following = null;
            return true;
        }

        if (line.length() == 0) {
            return true;
        }

        Matcher m = P_LATEX_C_ERROR.matcher(line);
        if (m.matches()) {
            //C-Style LaTeX error
            if (currentFile == null) {
                currentFile = m.group(1);
            }
            reportError(m.group(3), getLineNumber(m.group(2)));
            return true;
        }
        m = P_WARNING.matcher(line);
        if (m.matches()) {
            final String warningMessage = m.group(2);
            if (warningMessage.startsWith("Label(s) may have changed.")) {
                // prepare to re-run latex
                latexRerun = true;
                skip = 1;
                return true;
            }
            if (warningMessage.startsWith("There were undefined")) {
                if (citeNotFound) {
                    // prepare to run bibtex
                    bibRerun = true;
                }
                skip = 1;
                return true;
            }

            // Ignore undefined references because they are
            // found by the parser
            if (warningMessage.startsWith("Reference ")) {
                skip = 1;
                return true;
            }
            if (warningMessage.startsWith("Citation ")) {
                citeNotFound = true;
                skip = 1;
                return true;
            }

            severity = IMarker.SEVERITY_WARNING;
            message = warningMessage;

            final String warningSource = m.group(1);
            if (warningSource.startsWith("Package ")) {
                final String pName = warningSource.substring("Package ".length()).trim();
                if (pName.length() > 0) {
                    packageName = '(' + pName + ')';
                    message = pName + ": " + message;
                    following = FollowingItem.PACKAGE_WARNING_MSG;
                    return false;
                }
            }
            if (warningSource.contains("pdfTeX")) {
                following = FollowingItem.PDFTEX_WARNING_MSG;
                // pdfTeX sometimes hides parentheses here
                parseFileNames();
                return false;
            }

            // Check if we can find a line number here
            m = P_AT_LINE_WARNING.matcher(line);
            if (m.matches()) {
                lineNumber = getLineNumber(m.group(1));
                skip = 1;
                return true;
            }
            // If nothing has been fitting so far, look for following line numbers
            following = FollowingItem.LINE_NUMBER_WARNING;
            return false;
        }
        m = P_TEX_ERROR.matcher(line);
        if (m.matches()) {
            final String errorMessage = m.group(1);
            if (errorMessage.startsWith("Undefined control sequence.")){
                // Undefined Control Sequence
                message = "Undefined control sequence: ";
            }
            else if (errorMessage.startsWith("LaTeX Error: ")) {
                message = line.substring("LaTeX Error: ".length());
            }
            else {
                m = P_MISSING_CHAR_INSERTED.matcher(errorMessage);
                if (m.matches()) {
                    message = m.group(1) + ": ";
                    skip = 2;
                }
                else {
                    message = errorMessage;
                }
            }
            severity = IMarker.SEVERITY_ERROR;
            following = FollowingItem.LINE_NUMBER_ERROR;
            return false;
        }
        m = P_FULL_BOX.matcher(line);
        if (m.matches()) {
            reportLayoutWarning(line, getLineNumber(m.group(2)));
            skip = 1;
            return true;
        }
        m = P_NO_BIB_FILE.matcher(line);
        if (m.matches()){
            // prepare to run bibtex
            bibRerun = true;
            return true;
        }
        m = P_NO_TOC_FILE.matcher(line);
        if (m.matches()){
            // prepare to re-run latex
            latexRerun = true;
            return true;
        }
        parseFileNames();
        return true;
    }

    /**
     * Constructor.
     *
     * @param resource the file resource processed by the latex program
     */
    public LatexLogAnalyzer(final IResource resource) {
        this.mainFile = resource;
        this.project = resource.getProject();
        this.projectPath = project.getLocation();
        this.workingDir = resource.getParent().getProjectRelativePath();
        this.isWindowsPlatform = Platform.getOS().equals(Platform.OS_WIN32);
        this.fileStack = new Stack<String>();
        this.inputFiles = new HashSet<IPath>();
        this.externalNames = new HashSet<String>();
        reset();
    }

    /**
     * Parses a single line of the log output. If the line appears to be continued in
     * the next log line, this method buffers it and will append the following line(s) to
     * process them all at once.
     *
     * @param line log line
     * @return <code>true</code> if the line appeared to be complete, <code>false</code>
     *  if the parser is waiting for more input.
     */
    public boolean parseLine(final String line) {
        if (line == null) {
            processLine();
            currentLine = null;
            return true;
        }
        if (currentLine == null) {
            currentLine = new StringBuffer(BUFFER_LENGTH);
        }
        currentLine.append(trimLine(line));
        if (line.length() > 0 && !line.endsWith("...") 
                && line.length() % MAX_LINE_LENGTH == 0) {
            return false;
        }
        else {
            processLine();
            currentLine = null;
            return true;
        }
    }

    /**
     * Parses the entire latex log output text. Ignores the first line (version info).
     *
     * @param text log output
     */
    public void parseText(final String text) {
        skip = 1;
        int i = 0;
        // Not using the StringTokenizer, in order to consider empty lines.
        int next = text.indexOf('\n');
        while (next > -1) {
            parseLine(text.substring(i, next));
            i = next + 1;
            next = text.indexOf('\n', i);
        }
        if (i < text.length()) {
            parseLine(text.substring(i));
        }
        if (!fileStack.isEmpty()) {
            parsingStackErrors = true;
        }
    }

    /**
     * Resets the state of the latex log analyzer and clears all information about
     * detected input files.
     */
    public void reset() {
        fileStack.clear();
        inputFiles.clear();
        parsingStackErrors = false;
        latexRerun = false;
        bibRerun = false;
        citeNotFound = false;
        errors = false;
        warnings = false;
        following = null;
        skip = 0;
        packageName = null;
        lineNumber = null;
        message = null;
        severity = 0;
    }

    /**
     * Returns all input files that could be identified as resources in the project.
     *
     * @return IPath objects of input files
     */
    public Set<IPath> getInputFiles() {
        return new HashSet<IPath>(this.inputFiles);
    }

    /**
     * Returns the full path to all input files that are not located inside the
     * project structure.
     *
     * @return String with file paths, as found in the log output
     */
    public Set<String> getExternalNames() {
        return new HashSet<String>(this.externalNames);
    }

    /**
     * Returns if there where errors while going through the file stack. In this case,
     * there will likely be wrong assignments of messages to file resources.
     *
     * @return <code>true</code>, if any parsing stack errors occurred
     */
    public boolean hasParsingStackErrors() {
        return this.parsingStackErrors;
    }

    /**
     * Returns whether log messages indicated that an additional latex run is necessary.
     *
     * @return <code>true</code>, if latex should be run again
     */
    public boolean needsLatexRerun() {
        return this.latexRerun;
    }

    /**
     * Returns whether log messages indicated that the bibliography generation tool (e.g.
     * biber or biblatex) should be run.
     *
     * @return <code>true</code>, if the bibliography generation or sorting should run
     */
    public boolean needsBibRerun() {
        return this.bibRerun;
    }

    /**
     * Returns whether error messages have been added to the "Problems" view.
     *
     * @return <code>true</code>, if any error messages have been reported.
     */
    public boolean hasErrors() {
        return this.errors;
    }

    /**
     * Returns whether warning messages (including layout warnings) have been added
     * to the "Problems" view.
     *
     * @return <code>true</code>, if any warning messages have been reported.
     */
    public boolean hasWarnings() {
        return this.warnings;
    }

}
