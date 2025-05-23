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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Action for uncollapsing code folds. Expands all folds contained in the
 * selection or the fold the cursor is currently at. 
 * 
 * @author Oskar Ojala
 */
public class TexUncollapseAction implements IEditorActionDelegate {
    private IEditorPart targetEditor;
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        this.targetEditor = targetEditor;
    }
    
    /**
     * This function returns the text editor.
     */
    private ITextEditor getTextEditor() {
        if (targetEditor instanceof ITextEditor) {
            return (ITextEditor) targetEditor;
        } else {
            throw new RuntimeException("Expecting text editor. Found:"+targetEditor.getClass().getName());
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        TexSelections selection = new TexSelections(getTextEditor());
        
        int firstOffset = selection.getStartLine().getOffset();
        int lastOffset = selection.getEndLine().getOffset();
        
        ProjectionAnnotationModel model = (ProjectionAnnotationModel) getTextEditor()
        .getAdapter(ProjectionAnnotationModel.class);
        
        if (model != null) {
            // the predefined method permits us to do this, even if length=0
            model.expandAll(firstOffset, lastOffset - firstOffset);
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof TextSelection) {
            action.setEnabled(true);
            return;
        }
        action.setEnabled(targetEditor instanceof ITextEditor);
    }
}
