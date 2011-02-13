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
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
 * @author Boris von Loesch
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
        TexlipseProperties.setProjectProperty(project, TexlipseProperties.PARTIAL_BUILD_PROPERTY, value);
        if (value == null) {
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, null);
            // delete all tempPartial0000 files
            try {
                IResource[] res;
                IFolder projectOutputDir = TexlipseProperties.getProjectOutputDir(project);
                if (projectOutputDir != null)
                    res = projectOutputDir.members();
                else
                    res = project.members();
                for (int i = 0; i < res.length; i++) {
                    if (res[i].getName().startsWith("tempPartial0000"))
                        res[i].delete(true, null);
                }
                
                IFolder projectTempDir = TexlipseProperties.getProjectTempDir(project);
                if (projectTempDir != null && projectTempDir.exists())
                    res = projectTempDir.members();
                else
                    res = project.members();
                
                for (int i = 0; i < res.length; i++) {
                    if (res[i].getName().startsWith("tempPartial0000"))
                        res[i].delete(true, null);
                }
                IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
                res = sourceDir.members();
                for (int i = 0; i < res.length; i++) {
                    if (res[i].getName().startsWith("tempPartial0000"))
                        res[i].delete(true, null);
                }

            } catch (CoreException e) {
                TexlipsePlugin.log("Error while deleting temp files", e);
            }
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
            IProject project = ((TexEditor) editor).getProject();
            if (project == null) {
                action.setEnabled(false);
                return;
            }
            //System.out.println("partial-build-running-from");
            action.setChecked(TexlipseProperties.getProjectProperty(project, TexlipseProperties.PARTIAL_BUILD_PROPERTY) != null);
            run(action);
        }
    }
}
