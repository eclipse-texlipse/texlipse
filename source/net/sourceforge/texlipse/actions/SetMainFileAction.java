/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * This action sets the currently open file as project's main file.
 * 
 * @author Kimmo Karlsson
 */
public class SetMainFileAction implements IWorkbenchWindowActionDelegate, IEditorActionDelegate {
    
    // the window
	private IWorkbenchWindow window;
    private IEditorPart editor;

	/**
     * 
	 */
	public void run(IAction action) {
	    
	    IResource file = ((FileEditorInput)editor.getEditorInput()).getFile();
	    IProject project = file.getProject();
	    
        //load settings, if changed on disk
        if (TexlipseProperties.isProjectPropertiesFileChanged(project)) {
            TexlipseProperties.loadProjectProperties(project);
        }
        
        // check that there is an output format property
        String format = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT);
        if (format == null || format.length() == 0) {
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT, TexlipsePlugin.getPreference(TexlipseProperties.OUTPUT_FORMAT));
        }
        
        // check that there is a builder number
        String builderNum = TexlipseProperties.getProjectProperty(project, TexlipseProperties.BUILDER_NUMBER);
        if (builderNum == null || builderNum.length() == 0) {
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.BUILDER_NUMBER, TexlipsePlugin.getPreference(TexlipseProperties.BUILDER_NUMBER));
        }
        
	    String name = file.getName();
        IPath path = file.getFullPath();
        
        IResource currentMainFile = TexlipseProperties.getProjectSourceFile(project);
        // check if this is already the main file
        if (currentMainFile != null && path.equals(currentMainFile.getFullPath())) {
            return;
        }
        
        // set main file
	    TexlipseProperties.setProjectProperty(project, TexlipseProperties.MAINFILE_PROPERTY, name);
	    
        // set output files
	    String output = name.substring(0, name.lastIndexOf('.')+1) + format;
	    TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUTFILE_PROPERTY, output);

        // set source directory
	    String dir = path.removeFirstSegments(1).removeLastSegments(1).toString();
        TexlipseProperties.setProjectProperty(project, TexlipseProperties.SOURCE_DIR_PROPERTY, dir);
        
        // make sure there is output directory setting
        String oldOut = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_DIR_PROPERTY);
        if (oldOut == null) {
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUT_DIR_PROPERTY, dir);
        }
        
        // make sure there is temp directory setting
        String oldTmp = TexlipseProperties.getProjectProperty(project, TexlipseProperties.TEMP_DIR_PROPERTY);
        if (oldTmp == null) {
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.TEMP_DIR_PROPERTY, dir);
        }

        //save settings to file
        TexlipseProperties.saveProjectProperties(project);
	}

	/**
     * Nothing to do.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
     * Nothing to do.
	 */
	public void dispose() {
	}

	/**
	 * Cache the window object in order to be able to provide
     * UI components for the action.
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

    /**
     * 
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        editor = targetEditor;
        action.setEnabled(editor instanceof ITextEditor);
    }
}
