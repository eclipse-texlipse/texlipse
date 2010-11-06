/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.MarkerUtilities;


/**
 * Monitor the output of an external viewer program and open the
 * file from the correct location when viewer outputs
 * a "file:lineNumber" -string.
 *  
 * @author Kimmo Karlsson
 */
public class ViewerOutputScanner implements Runnable {

    // the stream reader
    private BufferedReader br;
    
    // the project this viewer is viewing
    private IProject project;

    /**
     * Create a new scanner.
     * @param project
     * @param in
     */
    public ViewerOutputScanner(IProject project, InputStream in) {
        this.project = project;
        this.br = new BufferedReader(new InputStreamReader(in));
    }

    /**
     * Empty constructor for the static version of the file open method.
     */
    protected ViewerOutputScanner(IProject project) {
        this.project = project;
    }
    
    /**
     * Convenience method for outsiders.
     * @param proj the current project
     * @param file file name, relative or absolute
     * @param lineNumber line number
     */
    public static void openInEditor(IProject proj, String file, int lineNumber) {
        new ViewerOutputScanner(proj).openFileFromLineNumber(file, lineNumber);
    }
    
    /**
     * Parse filename and line number from the given line of text.
     * 
     * @param line line of text from viewer, possibly a line number event
     */
    private void checkLine(String line) {
        
        int index = line.indexOf(':');
        if (index < 0) {
            return;
        }
        
        String file = line.substring(0, index);
        String number = line.substring(index+1);
        
        int lineNumber = -1;
        try {
            lineNumber = Integer.parseInt(number);
        } catch (NumberFormatException e) {
        }
        
        if (lineNumber >= 0) {
            openFileFromLineNumber(file, lineNumber);
        }
    }
    
    /**
     * Opens the given file from the given location if the file belongs
     * to the currently open project.
     * 
     * There is basically two possibilities for the filename:
     * 1) Path is relative: the filename represents a relative path
     *    from the current directory (project source dir) to the output dir.
     * 2) Path is absolute: the filename is an absolute
     *    path from the filesystem root to the project dir.
     * 
     * @param file the file name, relative or absolute
     * @param lineNumber line number
     */
    protected void openFileFromLineNumber(String file, int lineNumber) {

        if (project == null) {
            return;
        }
        
        IResource resource = null;
        //Patch for wrong directory seperators used by Yap
        if (File.separatorChar == '\\') {
            file = file.replace('/', '\\');
        }
        
        // path may contain project path
        String projDir = project.getLocation().addTrailingSeparator().toOSString();
        int index = file.indexOf(projDir);
        if (index == 0) {
            
            // remove the project path (external or inside workspace)
            file = file.substring(projDir.length());
            // yap also adds the output dir
            IFolder outdir = TexlipseProperties.getProjectOutputDir(project);
            if (outdir != null) {
                String outdirName = outdir.getProjectRelativePath().toString() + File.separator;
                index = file.indexOf(outdirName);
                if (index == 0) {
                    // remove output path
                    file = file.substring(outdirName.length());
                }
            }
            resource = project.findMember(file);
            if (resource == null) {
                //maybe in src folder
                IContainer srcDir = TexlipseProperties.getProjectSourceDir(project);
                resource = srcDir.findMember(file);                
            }
            
        } else {
            
            // path is relative
            if (file.startsWith("..")) {
                // remove dots and 'File.separator'
                file = file.substring(3);
            }
            
            String outDir = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_DIR_PROPERTY);
            if (outDir != null && outDir.length() > 0) {
                
                if (outDir.endsWith("/") || outDir.endsWith("\\")) {
                    outDir = outDir.substring(0, outDir.length()-1);
                }
                outDir = outDir.trim();
                if (file.indexOf(outDir) == 0) {
                    // remove output dir from path
                    file = file.substring(outDir.length()+1);
                }
            }
            
            IContainer srcDir = TexlipseProperties.getProjectSourceDir(project);
            resource = srcDir.findMember(file);
        }
        
        if (resource == null) {
            return;
        }
        
        IMarker mark = null;
        try {
            mark = resource.createMarker(IMarker.BOOKMARK);
            MarkerUtilities.setLineNumber(mark, lineNumber);
        } catch (CoreException e) {
        }
        
        if (mark != null) {
            
            Display display = TexlipsePlugin.getDefault().getWorkbench().getDisplay();
            display.syncExec(new EditorOpener(mark));
            
            try {
                mark.delete();
            } catch (CoreException e) {
            }
            
            new Thread(new Runnable() {
                public void run() {
                      ViewerManager.returnFocusToEclipse(
                    		  TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(
                          			TexlipseProperties.BUILDER_FORCE_RETURN_FOCUS));
                }
             }).start();
        }
    }

    /**
     * Editor opener task. Opens a file containing the given marker
     * to a new window in the editor and scrolls the editor window
     * to the right position.
     */
    class EditorOpener implements Runnable {

        private IMarker marker;

        public EditorOpener(IMarker marker) {
            this.marker = marker;
        }
        
        public void run() {
            
            IWorkbenchPage page = TexlipsePlugin.getCurrentWorkbenchPage();
            if (page == null) {
                return;
            }
            
            try {
                IDE.openEditor(page, marker, false);
            } catch (PartInitException e) {
            }
        }
    }

    /**
     * Monitor the stream line by line.
     */
    public void run() {
        String line = null;
        
        try {
            while ((line = br.readLine()) != null) {
                
                checkLine(line);
            }
        } catch (IOException e) {
        }
    }
}
