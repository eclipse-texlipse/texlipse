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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
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
     * @param file the file name, relative or absolute
     * @param lineNumber line number
     */
    protected void openFileFromLineNumber(String file, int lineNumber) {
        
        IResource resource = null;
        
        // resolve file path, which is hopefully always absolute
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        String wsDir = root.getLocation().toOSString();

        int index = file.indexOf(wsDir);
        if (index != 0) {
            
            // path not absolute, guess current project
            if (project == null) {
                return;
            }
            
            IFolder srcDir = TexlipseProperties.getProjectSourceDir(project);
            if (srcDir != null) {
                resource = srcDir.findMember(file);
            } else {
                resource = project.findMember(file);
            }
            
        } else {
            
            // viewer reports full path, check that source/output dirs aren't mixed up
            String path = file.substring(wsDir.length()+1+project.getName().length()+1);
            
            String outDir = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_DIR_PROPERTY);
            if (outDir != null && outDir.length() > 0) {
                
                outDir = outDir.replace('/' , ' ');
                outDir = outDir.replace('\\' , ' ');
                outDir = outDir.trim();
                if (path.indexOf(outDir) == 0) {
                    path = path.substring(outDir.length()+1);
                }
            }
            
            String srcDir = TexlipseProperties.getProjectProperty(project, TexlipseProperties.SOURCE_DIR_PROPERTY);
            if (srcDir != null && srcDir.length() > 0) {
                
                srcDir = srcDir.replace('/' , ' ');
                srcDir = srcDir.replace('\\' , ' ');
                srcDir = srcDir.trim();
                path = srcDir + File.separator + path;
            }
            
            resource = project.findMember(path);
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
