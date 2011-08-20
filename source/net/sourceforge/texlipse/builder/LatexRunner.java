/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


/**
 * Run the external latex program.
 * 
 * @author Kimmo Karlsson
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public class LatexRunner extends AbstractProgramRunner {
    
    private static final int MAX_LINE_LENGTH = 79;
	
	private Stack<String> parsingStack;
    private boolean alreadyShowError;
    
    /**
     * Create a new ProgramRunner.
     */
    public LatexRunner() {
        super();
        this.parsingStack = new Stack<String>();
    }
    
    protected String getWindowsProgramName() {
        return "latex.exe";
    }
    
    protected String getUnixProgramName() {
        return "latex";
    }
    
    public String getDescription() {
        return "Latex program";
    }
    
    public String getDefaultArguments() {
        return "-interaction=nonstopmode --src-specials %input";
    }
    
    public String getInputFormat() {
        return TexlipseProperties.INPUT_FORMAT_TEX;
    }
    
    /**
     * Used by the DviBuilder to figure out what the latex program produces.
     * 
     * @return output file format (dvi)
     */
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_DVI;
    }
    
    protected String[] getQueryString() {
        return new String[] { "\nPlease type another input file name:" , "\nEnter file name:" };
    }
    
    /**
     * Adds a problem marker
     * 
     * @param error The error or warning string
     * @param causingSourceFile name of the sourcefile
     * @param linenr where the error occurs
     * @param severity
     * @param resource
     * @param layout true, if this is a layout warning
     */
    private void addProblemMarker(String error, String causingSourceFile,
            int linenr, int severity, IResource resource, boolean layout) {
        
        
    	IProject project = resource.getProject();
        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
        
        IResource extResource = null;
        if (causingSourceFile != null) {
        	IPath p = new Path(causingSourceFile);
        	
        	if (p.isAbsolute()) {
        		//Make absolute path relative to source directory
        		//or to the directory of the resource
        		if (sourceDir.getLocation().isPrefixOf(p)) {
        			p = p.makeRelativeTo(sourceDir.getLocation());
        		}
        		else if (resource.getParent().getLocation().isPrefixOf(p)) {
        			p = p.makeRelativeTo(resource.getParent().getLocation());
        		}
        	}

        	extResource = sourceDir.findMember(p);
            if (extResource == null) {
                extResource = resource.getParent().findMember(p);
            }
        }
        if (extResource == null)
            createMarker(resource, null, error + (causingSourceFile != null ? " (Occurance: "
                    + causingSourceFile + ")" : ""), severity);
        else {
            if (linenr >= 0) {
                if (layout)
                    createLayoutMarker(extResource, new Integer(linenr), error);
                else
                    createMarker(extResource, new Integer(linenr), error, severity);
            } else
                createMarker(extResource, null, error, severity);
        }
    }
    
    /**
     * Parse the output of the LaTeX program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        
        TexlipseProperties.setSessionProperty(resource.getProject(), TexlipseProperties.SESSION_LATEX_RERUN, null);
        TexlipseProperties.setSessionProperty(resource.getProject(), TexlipseProperties.SESSION_BIBTEX_RERUN, null);
        
        parsingStack.clear();
        boolean errorsFound = false;
        boolean citeNotfound = false;
        alreadyShowError = false;
        StringTokenizer st = new StringTokenizer(output, "\r\n");

        final Pattern LATEXERROR = Pattern.compile("^! LaTeX Error: (.*)$");
        final Pattern LATEXCERROR = Pattern.compile("^(.+?\\.\\w{3}):(\\d+): (.+)$");
        final Pattern TEXERROR = Pattern.compile("^!\\s+(.*)$");
        final Pattern FULLBOX = Pattern.compile("^(?:Over|Under)full \\\\[hv]box .* at lines? (\\d+)-?-?(\\d+)?");
        final Pattern WARNING = Pattern.compile("^.+[Ww]arning.*: (.*)$");
        final Pattern ATLINE =  Pattern.compile("^l\\.(\\d+)(.*)$");
        final Pattern ATLINE2 =  Pattern.compile(".* line (\\d+).*");
        final Pattern NOBIBFILE = Pattern.compile("^No file .+\\.bbl\\.$");
        final Pattern NOTOCFILE = Pattern.compile("^No file .+\\.toc\\.$");
        
        String line;
        boolean hasProblem = false;
        String error = null;
        int severity = IMarker.SEVERITY_WARNING;
        int linenr = -1;
        String occurance = null;
        
        while (st.hasMoreTokens()) {
            line = st.nextToken();
            //Add more lines if line length is a multiple of 79 and
            //it does not end with ...
            while (!line.endsWith("...") && st.hasMoreTokens() 
            		&& line.length() % MAX_LINE_LENGTH == 0) {
            	line = line + st.nextToken();
            }
            line = line.replaceAll(" {2,}", " ").trim();
            Matcher m = LATEXCERROR.matcher(line);
            if (m.matches()) {
                //C-Style LaTeX error
                addProblemMarker(m.group(3), m.group(1), Integer.parseInt(m.group(2)), IMarker.SEVERITY_ERROR, resource, false);
                //Maybe parsingStack is empty...
                if (parsingStack.isEmpty()) {
                    //Add the file to the stack
                    parsingStack.push("(" + m.group(1));
                }
                continue;
            }
            m = TEXERROR.matcher(line);
            if (m.matches() && line.toLowerCase().indexOf("warning") == -1) {
                if (hasProblem) {
                    // We have a not reported problem
                    addProblemMarker(error, occurance, linenr, severity, resource, false);
                    linenr = -1;
                }
                hasProblem = true;
                errorsFound = true;
                severity = IMarker.SEVERITY_ERROR;
                occurance = determineSourceFile();
                Matcher m2 = LATEXERROR.matcher(line);
                if (m2.matches()) {
                    // LaTex error
                    error = m2.group(1);

        
                    String part2 = st.nextToken().trim();
                    
                    if (Character.isLowerCase(part2.charAt(0))) {
                        error += ' ' + part2;
                    }
                    updateParsedFile(part2);
                    continue;
                }
                if (line.startsWith("! Undefined control sequence.")){
                    // Undefined Control Sequence
                    error = "Undefined control sequence: ";
                    continue;
                }
                m2 = WARNING.matcher(line);
                if (m2.matches())
                    severity = IMarker.SEVERITY_WARNING;
                error = m.group(1);
                continue;
            }
            m = WARNING.matcher(line);
            if (m.matches()){
                if (hasProblem){
                    // We have a not reported problem
                    addProblemMarker(error, occurance, linenr, severity, resource, false);
                    linenr = -1;
                    hasProblem = false;
                }
                if (line.indexOf("Label(s) may have changed.") > -1) {
                    // prepare to re-run latex
                    TexlipseProperties.setSessionProperty(resource.getProject(),
                            TexlipseProperties.SESSION_LATEX_RERUN, "true");
                    continue;
                }
                else if (line.indexOf("There were undefined") > -1) {
                    if (citeNotfound) {
                        // prepare to run bibtex
                        TexlipseProperties.setSessionProperty(resource.getProject(),
                                TexlipseProperties.SESSION_BIBTEX_RERUN, "true");
                    }
                    continue;
                }

                // Ignore undefined references because they are
                // found by the parser
                if (line.indexOf("Warning: Reference ") > -1)
                    continue;
                if (line.indexOf("Warning: Citation ") > -1) {
                    citeNotfound = true;
                    continue;
                }
                severity = IMarker.SEVERITY_WARNING;
                occurance = determineSourceFile();
                hasProblem = true;
                if (line.startsWith("LaTeX Warning: ") || line.indexOf("pdfTeX warning") != -1) {
                    error = m.group(1);
                    //Try to get the line number
                    Matcher pM = ATLINE2.matcher(line);
                    if (pM.matches()) {
                        linenr = Integer.parseInt(pM.group(1));
                    }
                    String nextLine = st.nextToken().replaceAll(" {2,}", " ");
                    pM = ATLINE2.matcher(nextLine);
                    if (pM.matches()) {
                        linenr = Integer.parseInt(pM.group(1));
                    }
                    updateParsedFile(nextLine);
                    error += nextLine;
                    if (linenr != -1) {
                        addProblemMarker(line, occurance, linenr, severity,
                                resource, false);
                        hasProblem = false;
                        linenr = -1;
                    }
                    continue;
                } else {
                    error = m.group(1);
                    //Try to get the line number
                    Matcher pM = ATLINE2.matcher(line);
                    if (pM.matches()) {
                        linenr = Integer.parseInt(pM.group(1));
                    }
                    continue;
                }
            }
            m = FULLBOX.matcher(line);
            if (m.matches()) {
                if (hasProblem) {
                    // We have a not reported problem
                    addProblemMarker(error, occurance, linenr, severity,
                            resource, false);
                    linenr = -1;
                    hasProblem = false;
                }
                severity = IMarker.SEVERITY_WARNING;
                occurance = determineSourceFile();
                error = line;
                linenr = Integer.parseInt(m.group(1));
                addProblemMarker(line, occurance, linenr, severity, resource,
                        true);
                hasProblem = false;
                linenr = -1;
                continue;
            }
            m = NOBIBFILE.matcher(line);
            if (m.matches()){
                // prepare to run bibtex
                TexlipseProperties.setSessionProperty(resource.getProject(),
                        TexlipseProperties.SESSION_BIBTEX_RERUN, "true");
                continue;
            }
            m = NOTOCFILE.matcher(line);
            if (m.matches()){
                // prepare to re-run latex
                TexlipseProperties.setSessionProperty(resource.getProject(),
                        TexlipseProperties.SESSION_LATEX_RERUN, "true");
                continue;
            }
            m = ATLINE.matcher(line);
            if (hasProblem && m.matches()) {
                linenr = Integer.parseInt(m.group(1));
                String part2 = st.nextToken();
                int index = line.indexOf(' ');
                if (index > -1) {
	                error += " " + line.substring(index).trim() + " (followed by: "
	                        + part2.trim() + ")";
	                addProblemMarker(error, occurance, linenr, severity, resource,
	                        false);
	                linenr = -1;
	                hasProblem = false;
	                continue;
                }
            }
            m = ATLINE2.matcher(line);
            if (hasProblem && m.matches()) {
                linenr = Integer.parseInt(m.group(1));
                addProblemMarker(error, occurance, linenr, severity, resource,
                        false);
                linenr = -1;
                hasProblem = false;
                continue;
            }
            updateParsedFile(line);
        }
        if (hasProblem) {
            // We have a not reported problem
            addProblemMarker(error, occurance, linenr, severity, resource, false);
        }
        return errorsFound;
    }
    
    /**
     * Updates the stack that determines which file we are currently
     * parsing, so that errors can be annotated in the correct file. 
     * 
     * @param logLine A line from latex' output containing which file we are in
     */
    private void updateParsedFile(String logLine) {
        if (logLine.indexOf('(') == -1 && logLine.indexOf(')') == -1)
            return;
        for (int i = 0; i < logLine.length(); i++) {
            if (logLine.charAt(i) == '(') {
                int j;
                for (j = i + 1; j < logLine.length()
                        && isAllowedinName(logLine.charAt(j)); j++)
                    ;
                parsingStack.push(logLine.substring(i, j).trim());
                i = j - 1;
            } else if (logLine.charAt(i) == ')' && !parsingStack.isEmpty()) {
                parsingStack.pop();
            } else if (logLine.charAt(i) == ')' && !alreadyShowError) {
                alreadyShowError = true;
                // There was a parsing error, this is very rare
                TexlipsePlugin.log("Error while parsing the LaTeX output. " +
                        "Please consult the console output", null);
            }
        }
    }

    /**
     * Check if the character is allowed in a filename
     * @param c the character
     * @return true if the character is legal
     */
    private boolean isAllowedinName(char c) {
        if (c == '(' || c == ')' || c == '[')
            return false;
        else
            return true;
    }
        
    private static boolean isValidName(String name) {
        //File must have a file ending
        int p = name.lastIndexOf('.');
        if (p < 0) return false;
        //File ending must be shorter than 9 characters
        if (name.length()-p > 10) return false;
        return true;
    }
    
    /**
     * Determines the source file we are currently parsing.
     * 
     * @return The filename or null if no file could be determined
     */
    private String determineSourceFile() {
        int i = parsingStack.size()-1;
        while (i >= 0) {
            String fileName = parsingStack.get(i).substring(1);
            //Remove "
            if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                fileName = fileName.substring(1, fileName.length() - 1);
            }
            if (isValidName(fileName)) return fileName;
            i--;
        }
        return null;
    }
}
