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
import net.sourceforge.texlipse.viewer.ViewerManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;


/**
 * Preview action runs the previewer (that is configured in preferences)
 * on the current project.
 * 
 * @author Kimmo Karlsson
 */
public class PreviewAction implements IWorkbenchWindowActionDelegate, IEditorActionDelegate {
    
	/**
     * 
	 */
	public void run(IAction action) {
        try {
            ViewerManager.preview();
        } catch (CoreException e) {
            TexlipsePlugin.log("Launching viewer", e);
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
	}

    /**
     * 
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    }
}
