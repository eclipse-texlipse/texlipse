/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.texlipse.properties.TexlipseProperties;
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
