/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.properties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Properties;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;


/**
 * Names and helper methods for project properties.
 * 
 * @author Kimmo Karlsson
 */
public class TexlipseProperties {
	
    // properties
    public static final String PACKAGE_NAME = TexlipsePlugin.class.getPackage().getName();
    private static final String LATEX_PROJECT_SETTINGS_FILE = ".texlipse";

    public static final String MAINFILE_PROPERTY = "mainTexFile";
    public static final String OUTPUTFILE_PROPERTY = "outputFile";
    public static final String BIBFILE_PROPERTY = "bibFiles";
    public static final String BIBSTYLE_PROPERTY = "bibStyle";
    public static final String PREAMBLE_PROPERTY = "preamble";
    public static final String PARTIAL_BUILD_PROPERTY = "partialBuild";
    public static final String PARTIAL_BUILD_FILE = "partialFile";
    public static final String BIBFILES_CHANGED = "bibFilesChanged";
    
    public static final String OUTPUT_DIR_PROPERTY = "outputDir";
    public static final String SOURCE_DIR_PROPERTY = "srcDir";
    public static final String TEMP_DIR_PROPERTY = "tempDir";
    
    public static final String BIBCONTAINER_PROPERTY = "bibContainer";
    public static final String LABELCONTAINER_PROPERTY = "labelContainer";
    public static final String COMCONTAINER_PROPERTY = "commandContainer";
//    public static final String LISTENERS_PROPERTY = "changeListeners";

    // preferences
    public static final String BIB_DIR = "bibDir";
    public static final String TEMP_FILE_EXTS = "tempFileExts";
    public static final String FILE_LOCATION_PORT = "fileLocPort";
    public static final String BUILD_ENV_SETTINGS = "buildEnvSet";
    public static final String VIEWER_ENV_SETTINGS = "viewerEnvSet";

    public static final String TEX_COMPLETION = "texCompletion";
    public static final String TEX_COMPLETION_DELAY = "texDelay";

    public static final String TEX_ITEM_COMLETION = "texItemCompletion"; 
    public static final String TEX_ITEM_COMLETION_VALUE = "true";
    
    public static final String AUTO_PARSING = "autoParsing";
    public static final String AUTO_PARSING_DELAY = "autoParsingDelay";
    
    public static final String BIB_COMPLETION = "bibCompletion";
    public static final String BIB_COMPLETION_DELAY = "bibDelay";

    public static final String INDENTATION = "indent";
    public static final String INDENTATION_LEVEL = "indentLevel";
    public static final String INDENTATION_ENVS = "indentEnvs";
     
    public static final String WORDWRAP_TYPE = "wrapType";
    public static final String WORDWRAP_LENGTH = "lineLength";
    public static final String WORDWRAP_TYPE_NONE = "none";
    public static final String WORDWRAP_TYPE_SOFT = "soft";
    public static final String WORDWRAP_TYPE_HARD = "hard";
    
    public static final String CODE_FOLDING = "codeFolding";
    public static final String CODE_FOLDING_PREAMBLE = "codeFoldingPreamble";
    public static final String CODE_FOLDING_PART = "codeFoldingPart";
    public static final String CODE_FOLDING_CHAPTER = "codeFoldingChapter";
    public static final String CODE_FOLDING_SECTION = "codeFoldingSection";
    public static final String CODE_FOLDING_SUBSECTION = "codeFoldingSubSection";
    public static final String CODE_FOLDING_SUBSUBSECTION = "codeFoldingSubSubSection";
    public static final String CODE_FOLDING_PARAGRAPH = "codeFoldingParagraph";
    public static final String CODE_FOLDING_ENVS = "codeFoldingEnvs";
    
    public static final String OUTLINE_PREAMBLE = "outlinePreamble";
    public static final String OUTLINE_PART = "outlinePart";
    public static final String OUTLINE_CHAPTER = "outlineChapter";
    public static final String OUTLINE_SECTION = "outlineSection";
    public static final String OUTLINE_SUBSECTION = "outlineSubSection";
    public static final String OUTLINE_SUBSUBSECTION = "outlineSubSubSection";
    public static final String OUTLINE_PARAGRAPH = "outlineParagraph";
    public static final String OUTLINE_ENVS = "outlineEnvs";
    
    public static final String BUILDER_NUMBER = "builderNum";
    public static final String BUILDER_CONSOLE_OUTPUT = "builderConsole";
    public static final String BUILD_BEFORE_VIEW = "buildBeforeView";
    
    public static final String OUTPUT_FORMAT = "outputFormat";
    public static final String OUTPUT_FORMAT_AUX = "aux";
    public static final String OUTPUT_FORMAT_BIB = "bib";
    public static final String OUTPUT_FORMAT_IDX = "idx";
    public static final String OUTPUT_FORMAT_TEX = "tex";
    public static final String OUTPUT_FORMAT_DVI = "dvi";
    public static final String OUTPUT_FORMAT_PS = "ps";
    public static final String OUTPUT_FORMAT_PDF = "pdf";

    // session variables
    public static final String SESSION_BIBTEX_RERUN = "rerunBibtex";
    public static final String SESSION_LATEX_RERUN = "rerunLatex";
    public static final String SESSION_MAKEINDEX_RERUN = "rerunMakeindex";
    public static final String SESSION_PROPERTIES_LOAD = "propsLoaded";
    // attribute for session properties to hold the viewer process object
    public static final String SESSION_ATTRIBUTE_VIEWER = "active.viewer";
    
    /**
       * A named preference that controls whether bracket matching highlighting is turned on or off.
       * <p>
       * Value is of type <code>Boolean</code>.
       * </p>
       */
    public final static String MATCHING_BRACKETS = "matchingBrackets"; //$NON-NLS-1$
    /**
       * A named preference that holds the color used to highlight matching brackets.
       * <p>
       * Value is of type <code>String</code>. A RGB color value encoded as a string using class <code>PreferenceConverter</code>
       * </p>
       * 
       * @see org.eclipse.jface.resource.StringConverter
       * @see org.eclipse.jface.preference.PreferenceConverter
       */
    public final static String MATCHING_BRACKETS_COLOR = "matchingBracketsColor"; //$NON-NLS-1$

    
    /**
     * Returns the contents of a file in the project.
     * 
     * @param resource The file resource to read from
     * @return Contents of the given file in string form or null if the given resource wasn't a file
     * @throws IOException if an error occurs
     */
    public static String getFileContents(IResource resource) throws IOException {
        
//       IFolder srcDir = getProjectSourceDir(project);
//       IFile file = srcDir.getFile(filename);
       if (resource.getType() != IResource.FILE)
           return null;

       File f = resource.getLocation().toFile();
       FileInputStream in = new FileInputStream(f);
       ByteArrayOutputStream out = new ByteArrayOutputStream();
       byte[] buff = new byte[4096];
       
       int len = 0;
       while ((len = in.read(buff)) == buff.length) {
           out.write(buff);
       }
       
       if (len > 0) {
           out.write(buff, 0, len);
       }
       in.close();
       String contents = null;
       try {
           // cast will succeed, because we have already checked for it to be a file
           contents = out.toString(((IFile) resource).getCharset());
       } catch (UnsupportedEncodingException e) {
           // if the correct encoding is not supported, try with default
           contents = out.toString();
       } catch (CoreException e) {
           // should not happen
       }
       out.close();
       return contents;
    }
    
    /**
     * Returns an array of file handles to all files in project's source
     * directory, which have one of the following extensions:
     * tex,ltx,bib,idx.
     * This method recursively checks all subdirectories.
     * 
     * @param project the current project
     * @return an array of file handles
     */
    public static IResource[] getAllProjectFiles(IProject project) {
        
        IContainer sourceDir = getProjectSourceDir(project);
        if (sourceDir == null) {
            sourceDir = project;
        }
        return (IResource[]) getAllMemberFiles(sourceDir, new String[] { "tex", "ltx", "bib", "idx" }).toArray(new IResource[0]);
    }
    
    /**
     * This method returns a list of files under the given directory
     * or its subdirectories. The directories themselves are not returned.
     * 
     * @param dir a directory
     * @return list of IResource objects representing the files under
     *         the given directory and its subdirectories
     */
    public static ArrayList getAllMemberFiles(IContainer dir, String[] exts) {
        
        ArrayList list = new ArrayList();
        
        IResource[] arr = null;
        try {
            arr = dir.members();
        } catch (CoreException e) {
        }
        
        for (int i = 0; arr != null && i < arr.length; i++) {
        	
            if (arr[i].getType() == IResource.FOLDER) {
                list.addAll(getAllMemberFiles((IFolder)arr[i], exts));
            } else {
            	
            	for (int j = 0; j < exts.length; j++) {
            		if (exts[j].equals(arr[i].getFileExtension())) {
                        list.add(arr[i]);
                        break;
            		}
            	}
            	
            }
        }
        
        return list;
    }
    
    /**
     * Find the project's main source file. The file may or may not exist.
     * @param project the current project
     * @return a handle to the project's main source file, or null if main file not set
     */
    public static IFile getProjectSourceFile(IProject project) {
    	IFolder folder = getProjectSourceDir(project);
    	if (folder == null) {
    		return project.getFile(getProjectProperty(project, MAINFILE_PROPERTY));
    	}
        return folder.getFile(getProjectProperty(project, MAINFILE_PROPERTY));
    }
    
    /**
     * Find the project's source directory.
     * @param project the current project
     * @return a handle to the project's source directory or null if source directory is
     *         the same as project directory
     */
    public static IFolder getProjectSourceDir(IProject project) {
        
        String dir = TexlipseProperties.getProjectProperty(project, TexlipseProperties.SOURCE_DIR_PROPERTY);
        if (dir != null && dir.length() > 0) {
            return project.getFolder(dir);
        }
        return null;
    }
    
    /**
     * Find the project's output file. The file may or may not exist.
     * @param project the current project
     * @return a handle to the project's output file or null if output file not set
     */
    public static IFile getProjectOutputFile(IProject project) {
    	IFolder folder = getProjectOutputDir(project);
    	if (folder == null) {
    		return project.getFile(getProjectProperty(project, OUTPUTFILE_PROPERTY));
    	}
        return folder.getFile(getProjectProperty(project, OUTPUTFILE_PROPERTY));
    }
    
    /**
     * Find the project's output directory.
     * @param project the current project
     * @return a handle to the project's output directory or null if output directory is
     *         the same as project's directory
     */
    public static IFolder getProjectOutputDir(IProject project) {
        String dir = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_DIR_PROPERTY);
        if (dir != null && dir.length() > 0) {
            return project.getFolder(dir);
        }
        return null;
    }
    
    /**
     * Find the project's temporary files directory.
     * @param project the current project
     * @return a handle to the project's temp directory or null if temp directory is
     *         the same as project's directory
     */
    public static IFolder getProjectTempDir(IProject project) {
        String dir = TexlipseProperties.getProjectProperty(project, TexlipseProperties.TEMP_DIR_PROPERTY);
        if (dir != null && dir.length() > 0) {
            return project.getFolder(dir);
        }
        return null;
    }
    
    /**
     * Loads the project properties from a file.
     * 
     * @param project current project
     */
    public static void loadProjectProperties(IProject project) {
        
        IFile settingsFile = project.getFile(LATEX_PROJECT_SETTINGS_FILE);
        if (!settingsFile.exists()) {
            return;
        }
        
        Properties prop = new Properties();
        try {
            prop.load(settingsFile.getContents());
        } catch (CoreException e) {
            TexlipsePlugin.log("Loading project property file", e);
            return;
        } catch (IOException e) {
            TexlipsePlugin.log("Loading project property file", e);
            return;
        }
        
        setSessionProperty(project, SESSION_PROPERTIES_LOAD, new Long(System.currentTimeMillis()));
        setProjectProperty(project, MAINFILE_PROPERTY, prop.getProperty(MAINFILE_PROPERTY, ""));
        setProjectProperty(project, OUTPUTFILE_PROPERTY, prop.getProperty(OUTPUTFILE_PROPERTY, ""));
        setProjectProperty(project, SOURCE_DIR_PROPERTY, prop.getProperty(SOURCE_DIR_PROPERTY, ""));
        setProjectProperty(project, OUTPUT_DIR_PROPERTY, prop.getProperty(OUTPUT_DIR_PROPERTY, ""));
        setProjectProperty(project, TEMP_DIR_PROPERTY, prop.getProperty(TEMP_DIR_PROPERTY, ""));
        setProjectProperty(project, BUILDER_NUMBER, prop.getProperty(BUILDER_NUMBER, ""));
        setProjectProperty(project, OUTPUT_FORMAT, prop.getProperty(OUTPUT_FORMAT, ""));
    }
    
    /**
     * Saves the project properties to a file.
     * 
     * @param project current project
     */
    public static void saveProjectProperties(IProject project) {
        
        IFile settingsFile = project.getFile(LATEX_PROJECT_SETTINGS_FILE);
        Properties prop = new Properties();
        
        prop.setProperty(MAINFILE_PROPERTY, getProjectProperty(project, MAINFILE_PROPERTY));
        prop.setProperty(OUTPUTFILE_PROPERTY, getProjectProperty(project, OUTPUTFILE_PROPERTY));
        prop.setProperty(SOURCE_DIR_PROPERTY, getProjectProperty(project, SOURCE_DIR_PROPERTY));
        prop.setProperty(OUTPUT_DIR_PROPERTY, getProjectProperty(project, OUTPUT_DIR_PROPERTY));
        prop.setProperty(TEMP_DIR_PROPERTY, getProjectProperty(project, TEMP_DIR_PROPERTY));
        prop.setProperty(BUILDER_NUMBER, getProjectProperty(project, BUILDER_NUMBER));
        prop.setProperty(OUTPUT_FORMAT, getProjectProperty(project, OUTPUT_FORMAT));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            prop.store(baos, "TeXlipse project settings");
        } catch (IOException e) {}
        
        NullProgressMonitor mon = new NullProgressMonitor();
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try {
            
            if (settingsFile.exists()) {
                settingsFile.setContents(bais, true, false, mon);
            } else {
                settingsFile.create(bais, true, mon);
            }
            
        } catch (CoreException e) {
            TexlipsePlugin.log("Saving project property file", e);
        }
    }

    /**
     * Check if the project properties should be read from disk.
     * 
     * @param project current project
     * @return true, if the project properties should be read from disk
     */
    public static boolean isProjectPropertiesFileChanged(IProject project) {
        
        IResource settingsFile = project.findMember(LATEX_PROJECT_SETTINGS_FILE);
        if (settingsFile == null) {
            return false;
        }
        
        Long lastLoadTime = (Long) getSessionProperty(project, SESSION_PROPERTIES_LOAD);
        if (lastLoadTime == null) {
            return true;
        }
        
        long timeStamp = settingsFile.getLocalTimeStamp();
        return timeStamp > lastLoadTime.longValue();
    }
    
    /**
     * Read a project property.
     * 
     * @param project the current project
     * @param property the name of the property
     * @return the value of the named project property or null if the property is not found
     */
    public static String getProjectProperty(IResource project, String property) {
        String value = null;
        try {
            value = project.getPersistentProperty(new QualifiedName(TexlipseProperties.PACKAGE_NAME, property));
        } catch (CoreException e) {
            // do nothing
        }
        return value;
    }

    /**
     * Write a project property. This value will be stored to the project settings file on disk.
     * 
     * @param project the current project
     * @param property the name of the property
     * @param value new value for the property
     */
    public static void setProjectProperty(IResource project, String property, String value) {
        try {
            project.setPersistentProperty(new QualifiedName(TexlipseProperties.PACKAGE_NAME, property), value);
        } catch (CoreException e) {
            // do nothing
        }
    }
    
    /**
     * Read a session property.
     * 
     * @param project the current project
     * @param property the name of the property
     * @return the value of session property or null if the property is not found
     */
    public static Object getSessionProperty(IResource project, String property) {
        Object value = null;
        try {
            value = project.getSessionProperty(new QualifiedName(TexlipseProperties.PACKAGE_NAME, property));
        } catch (CoreException e) {
            // do nothing
        }
        return value;
    }

    /**
     * Write a session property. This value will be stored only in memory as long as Eclipse is still running.
     * 
     * @param project the current project
     * @param property the name of the property
     * @param value new value for the session property
     */
    public static void setSessionProperty(IResource project, String property, Object value) {
        try {
            project.setSessionProperty(new QualifiedName(TexlipseProperties.PACKAGE_NAME, property), value);
        } catch (CoreException e) {
            // do nothing
        }
    }
}
