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

import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.part.FileEditorInput;


/**
 * This action enables the partial building mode 
 * on the current project.
 * 
 * @author Kimmo Karlsson
 */
public class PartialBuildAction implements IWorkbenchWindowActionDelegate, IEditorActionDelegate {
    
    // the current window
    private IWorkbenchWindow window;
    
    // the current editor
    private IEditorPart editor;

	/**
     * 
	 */
	public void run(IAction action) {

        String value = action.isChecked() ? "true" : null;
        IProject project = ((FileEditorInput)editor.getEditorInput()).getFile().getProject();
        //System.out.println("partial-build state before: "+ TexlipseProperties.getProjectProperty(project, "partialBuild") + ", after: " + value);
        TexlipseProperties.setProjectProperty(project, TexlipseProperties.PARTIAL_BUILD_PROPERTY, value);
        if (value == null) {
            IFile f = (IFile) TexlipseProperties.getSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE);
            if (f != null && f.exists()) {
                try {
                    IContainer dir = f.getParent();
                    f.delete(true, new NullProgressMonitor());
                    
                    String ext = f.getFileExtension();
                    String name = f.getName();
                    String base = name.substring(0, name.length() - ext.length());
                    
                    IResource log = dir.findMember(base + "log");
                    if (log != null) {
                        log.delete(true, new NullProgressMonitor());
                    }
                    
                    IResource aux = dir.findMember(base + "aux");
                    if (aux != null) {
                        aux.delete(true, new NullProgressMonitor());
                    }
                    
                } catch (CoreException e) {
                }
            }
            //TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, null);
        }
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
        action.setEnabled(editor instanceof TexEditor);
        if (action.isEnabled()) {
            IProject project = ((FileEditorInput)editor.getEditorInput()).getFile().getProject();
            //System.out.println("partial-build-running-from");
            action.setChecked(TexlipseProperties.getProjectProperty(project, TexlipseProperties.PARTIAL_BUILD_PROPERTY) != null);
            run(action);
        }
    }
}
