/*
 * $Id$
 *
 * Copyright (c) 2005 by the TeXlipse Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions;

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

/**
 * Simple action to force a BibTex run on next build
 * @author Boris von Loesch
 *
 */
public class RunBibTeXOnNextBuildAction implements IEditorActionDelegate {

    private IEditorPart editor;

    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        editor = targetEditor;
    }

    public void run(IAction action) {
        IResource res = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        if (res == null) return;
        
        IProject project = res.getProject();
        TexlipseProperties.setSessionProperty(project, TexlipseProperties.BIBFILES_CHANGED, true);
    }

    public void selectionChanged(IAction action, ISelection selection) {
        //Nothing to do
    }

}
