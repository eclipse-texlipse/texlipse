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

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;


/**
 * Run the external latex program.
 * 
 * @author Kimmo Karlsson
 * @author Oskar Ojala
 */
public class LatexRunner extends AbstractProgramRunner {

    private Stack parsingStack;
    
    /**
     * Create a new ProgramRunner.
     */
    public LatexRunner() {
        super();
        this.parsingStack = new Stack();
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
        return "-interaction=scrollmode --src-specials %input";
    }
    
    public String getInputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_TEX;
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
     * Parse the output of the LaTeX program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        
        TexlipseProperties.setSessionProperty(resource.getProject(), TexlipseProperties.SESSION_LATEX_RERUN, null);
        TexlipseProperties.setSessionProperty(resource.getProject(), TexlipseProperties.SESSION_BIBTEX_RERUN, null);
        
        IProject project = resource.getProject();
        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
        if (sourceDir == null) {
            sourceDir = project;
        }
        
        boolean errorsFound = false;
        //String prevLine = "";
        StringTokenizer st = new StringTokenizer(output, "\r\n");

        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (line.startsWith("! Undefined control sequence.")) {

                String part1 = st.nextToken();
                String part2 = st.nextToken();
                int index = part1.indexOf(' ');
                int comIndex = part1.indexOf('\\');
                if (comIndex == -1)
                    comIndex = index; // just in case...
                
                String lineNumberString = part1.substring(2, index);
                
                Integer lineNumber = null;
                try {
                    int num = Integer.parseInt(lineNumberString);
                    lineNumber = new Integer(num);
                } catch (NumberFormatException e) {
                    continue;
                }
                
                String error = "Undefined control sequence: "
                        + part1.substring(comIndex).trim() + " (followed by: " + part2.trim() + ")";

                errorsFound = true;

                String causingSourceFile = determineSourceFile();
                //System.out.println("cause: " + causingSourceFile);
                IResource extResource = null;
                if (causingSourceFile != null) {
                    extResource = sourceDir.findMember(causingSourceFile);
                }
                
                if (extResource != null) {
                    createMarker(extResource, lineNumber, error);
                } else {
                    createMarker(resource, lineNumber, error);
                }

            } else if (line.startsWith("! LaTeX Error:")) {

                String error = line.substring(15);
                String part2 = st.nextToken().trim();

                if (Character.isLowerCase(part2.charAt(0))) {
                    error += ' ' + part2;
                }

                // find additional information related to the error
                Integer lineNumber = null;
                if (part2.startsWith("See the LaTeX manual")) {

                    String help2 = st.nextToken();
                    if (help2.startsWith("Type ")) {

                        String dots = st.nextToken().trim();
                        if (dots.startsWith("..")) {

                            String lineNumStr = st.nextToken().trim();
                            if (lineNumStr.length() == 0) {
                                lineNumStr = st.nextToken().trim();
                            }

                            if (lineNumStr.startsWith("l.")) {

                                int lineNum = -1;
                                try {
                                    lineNum = Integer.parseInt(lineNumStr.substring(2, lineNumStr.indexOf(' ', 2)));
                                } catch (NumberFormatException e) {
                                }
                                
                                if (lineNum != -1) {
                                    lineNumber = new Integer(lineNum);
                                }
                            }
                        }
                    }
                }

                errorsFound = true;
                
                String causingSourceFile = determineSourceFile();
                IResource extResource = null;
                if (causingSourceFile != null) {
                    extResource = sourceDir.findMember(causingSourceFile);
                }
                
                if (extResource != null) {
                    createMarker(extResource, lineNumber, error);
                } else {
                    createMarker(resource, lineNumber, error);
                }
            } else if (line.startsWith("LaTeX Warning: ")) {
                
                if (line.indexOf("Label(s) may have changed.") > 0) {
                    // prepare to re-run latex
                    TexlipseProperties.setSessionProperty(resource.getProject(), TexlipseProperties.SESSION_LATEX_RERUN, "true");
                } else if (line.indexOf("There were undefined references.") > 0) {
                    // prepare to run bibtex
                    TexlipseProperties.setSessionProperty(resource.getProject(), TexlipseProperties.SESSION_BIBTEX_RERUN, "true");
                }
            } else if (line.startsWith("Overfull \\hbox") || line.startsWith("Underfull \\hbox")) {
                
                int startIndex = line.indexOf("lines ") + 6;
                if (startIndex == -1)
                    continue;
                int endIndex = startIndex;
                Integer lineNumber = null;
                
                //System.out.println(line.substring(startIndex));
                for (; endIndex < line.length(); endIndex++) {
                    if (!Character.isDigit(line.charAt(endIndex))) {
                        try {
                            //System.out.println(line.substring(startIndex, endIndex));
                            int num = Integer.parseInt(line.substring(startIndex, endIndex));
                            lineNumber = new Integer(num);
                            break;
                        } catch (NumberFormatException e) {}                        
                    }
                }
                if (lineNumber != null) {
                    String causingSourceFile = determineSourceFile();
                    //System.out.println("cause: " + causingSourceFile);
                    IResource extResource = null;
                    if (causingSourceFile != null) {
                        extResource = sourceDir.findMember(causingSourceFile);
                    }
                    
                    if (extResource != null) {
                        createMarker(extResource, lineNumber, line, IMarker.SEVERITY_WARNING);
                    } else {
                        createMarker(resource, lineNumber, line, IMarker.SEVERITY_WARNING);
                    }                    
                }
                
            } else if (line.indexOf("(") != -1 || line.indexOf(")") != -1) {
                // keep track of which source file we are parsing
                this.updateParsedFile(line);
            }
        }
        return errorsFound;
    }

    /**
     * Updates the source file we are currently parsing from the given
     * line of latex' log.
     * 
     * @param logLine A line from latex' output containing which file we are in
     */
    private void updateParsedFile(String logLine) {
        //Stack st = new Stack();
        //System.out.println(logLine);
        //TODO this still might not properly handle file names with spaces
//        String partCommands[] = logLine.split("[^\\\\]\\s");
        String partCommands[] = logLine.split("\\s");
        for (int i = 0; i < partCommands.length; i++) {
            if (partCommands[i].startsWith("(")) {
                //System.out.println(partCommands[i]);
                if (!partCommands[i].endsWith(")")) {
                    parsingStack.push(partCommands[i]);
                } else {
                    this.removeClosingParentheses(partCommands[i]);
                }
            } else if (partCommands[i].endsWith(")")) {
                this.removeClosingParentheses(partCommands[i]);
            }
        }
    }

    /**
     * Calculates how many closing parentheses the command has and
     * pops the equivalent number of entries from the stack.
     * 
     * @param command A command containing closing parentheses
     */
    private void removeClosingParentheses(String command) {
        int amount = -1;
        for (int j = command.length() - 1; j >= 0; j--) {
            if (command.charAt(j) == ')') {
                amount++;
            } else {
                break;
            }
        }
        for (;amount > 0; amount--) {
            if (!parsingStack.empty()) {
                parsingStack.pop();
            } else {
                break;
            }
        }
    }
    
    /**
     * Determines the source file we are currently parsing.
     * 
     * @return The filename or null if no file could be determined
     */
    private String determineSourceFile() {
        if (!parsingStack.empty())
            return ((String) parsingStack.peek()).substring(1);
        else
            return null;
    }
}
