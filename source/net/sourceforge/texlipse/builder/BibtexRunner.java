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

import java.io.File;
import java.util.StringTokenizer;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;


/**
 * Run the external bibtex command.
 * 
 * @author Kimmo Karlsson
 */
public class BibtexRunner extends AbstractProgramRunner {

    /**
     * Create a new bibtex program runner
     */
    public BibtexRunner() {
        super();
    }

    /**
     * @return bibtex program name in windows
     */
    protected String getWindowsProgramName() {
        return "bibtex.exe";
    }
    
    /**
     * @return bibtex program name in unix systems
     */
    protected String getUnixProgramName() {
        return "bibtex";
    }
    
    /**
     * @return bibtex program description
     */
    public String getDescription() {
        return "Bibtex program";
    }
    
    protected String[] getQueryString() {
        return new String[] {"Please type input file name (no extension)--"};
    }

    /**
     * @param resource the project's main file to build
     * @return arguments for the bibtex program when building the given resource
     */
    public String getArguments(IResource resource) {
        String args = super.getArguments(resource);
        //Bibtex stops if there is a fileextension, so remove it
        String name = resource.getName();
        String baseName = name.substring(0, name.lastIndexOf('.'));
        int bPos = args.indexOf(baseName + "." + getInputFormat());
        if (bPos >= 0) {
            args = args.substring(0, bPos + baseName.length()) + 
            args.substring(bPos + baseName.length() + 1 + getInputFormat().length());
        }
        return args;
        // String args = "";

        // Seems unnecessary now, but we need more testing
//        String os = System.getProperty("os.name").toLowerCase();
//        if (os.indexOf("windows") >= 0) {
//            args = getIncludeDirArguments(resource.getProject());
//        }
        
/*        String name = resource.getName();
        String baseName = name.substring(0, name.lastIndexOf('.'));
        //return args + baseName;
        return baseName;*/
    }

    /**
     * Find out the real locations of the needed bib files.
     * @return the directories containing needed bib files as arguments
     */
    private String getIncludeDirArguments(IResource project) {
        
        String[] bibs = (String[]) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBFILE_PROPERTY);
        if (bibs == null) {
            return " ";
        }
        
        String bibDirs = TexlipsePlugin.getDefault().getPreferenceStore().getString(TexlipseProperties.BIB_DIR);
        if (bibDirs == null) {
            bibDirs = "";
        }
        
        Boolean biblatexMode = (Boolean) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_BIBLATEXMODE_PROPERTY);

        // TODO useless? shouldn't be such a project property
        String aDir = TexlipseProperties.getProjectProperty(project, TexlipseProperties.BIBFILE_PROPERTY);
        if (aDir == null) {
            aDir = "";
        }

        String pDir = project.getLocation().toOSString();
        
        String[] dirs = (bibDirs + aDir + File.pathSeparatorChar + pDir).split(File.pathSeparator);
        if (dirs == null) {
            dirs = new String[0];
        }
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bibs.length; i++) {
            for (int j = 0; j < dirs.length; j++) {
                String bibPath;
                if (biblatexMode == null) {
                    bibPath = dirs[j] + bibs[i] + ".bib";
                }
                else {
                    bibPath = dirs[j] + bibs[i]; 
                }
                File b = new File(bibPath);
                if (b.exists()) {
                    sb.append(" -include-dir=");
                    sb.append(dirs[j]);
                }
            }
        }
        
        sb.append(' ');
        return sb.toString();
    }

    public String getInputFormat() {
        return TexlipseProperties.INPUT_FORMAT_BIB;
    }
    
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_AUX;
    }

    /**
     * Parse the output of the BibTeX program.
     *  
     * @param origResource the input tex file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource origResource, String output) {
        
        IProject project = origResource.getProject();
        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
        if (sourceDir == null) {
            sourceDir = project;
        }
        //Initialize bibResource with origResource so that all errors are at least
        //displayed even if no valid bib file was found
        IResource bibResource = origResource;
        
        boolean errorsFound = false;
        StringTokenizer st = new StringTokenizer(output, "\r\n");
        while (st.hasMoreTokens()) {

            String line = st.nextToken();
            if (line.indexOf("I was expecting a ") == 0) {

                parseErrorLine(sourceDir, line);
                errorsFound = true;
                
            } else if (line.startsWith("You're missing a field name")) {
                
                parseErrorLine(sourceDir, line);
                errorsFound = true;
                
            } else if (line.startsWith("Warning--")) {
                
                String message = line.substring(9);
                Integer lineNumber = null;
                
                String nextLine = st.nextToken();
                // see if additional info is available about the warning
                if (nextLine.startsWith("--line ")) {
                    
                    int index = nextLine.indexOf(" of file ");
                    try {
                        lineNumber = new Integer(nextLine.substring(7, index));
                    } catch (NumberFormatException e) {
                    }
                    
                    String fileName = nextLine.substring(index + 9);
                    IResource resource = sourceDir.findMember(fileName);
                    createMarker(resource, lineNumber, message, IMarker.SEVERITY_WARNING);
                    
                } else if (nextLine.startsWith("Warning--")) {
                    
                    // if followed by another warning, this is the endlist with no info
                    createMarker(bibResource, lineNumber, message, IMarker.SEVERITY_WARNING);
                    createMarker(bibResource, lineNumber, nextLine.substring(9), IMarker.SEVERITY_WARNING);
                    
                } else {
                    // list of warnings ended
                    createMarker(bibResource, lineNumber, message, IMarker.SEVERITY_WARNING);
                }
                
            } else if (line.startsWith("Database file ")) {
                
                String bibName = line.substring(line.indexOf(':')+2);
                bibResource = sourceDir.findMember(bibName);
                if (bibResource == null) {
                	//Could happen if bibName is not part of project (kpathsea)
                	bibResource = origResource;
                }

            } else if (line.startsWith("I couldn't open database file ")) {
                
                String bibName = line.substring(line.indexOf("file")+5);
                // TODO Add marker to main file, but we should really find the offending
                // \bibliography command in the main file and add it to the right line.
                String srcFile = TexlipseProperties.getProjectProperty(project,
                        TexlipseProperties.MAINFILE_PROPERTY);
                IResource mainFileResource = sourceDir.findMember(srcFile);
                createMarker(mainFileResource, new Integer(0), "Could not open bibtex database file " + bibName);
                
                return true; // errors found, and no use in parsing the rest of the file
            }
        }
        
        return errorsFound;
    }

    /**
     * Parse a Bibtex error message from the given line.
     * Create also an error marker to the right place.
     * 
     * @param sourceDir directory containing project's source files 
     * @param line the error message line
     */
    private void parseErrorLine(IContainer sourceDir, String line) {
        
        int index = line.indexOf("---line ");
        int index2 = line.indexOf(" of file ", index);
        String lineNumberString = line.substring(index+8, index2);
        
        Integer lineNumber = null;
        try {
            int num = Integer.parseInt(lineNumberString);
            lineNumber = new Integer(num);
        } catch (NumberFormatException e) {
        }
        
        String fileName = line.substring(index2+9);
        IResource resource = sourceDir.findMember(fileName);
        
        String error = line.substring(0, index);
        createMarker(resource, lineNumber, error);
    }
}
