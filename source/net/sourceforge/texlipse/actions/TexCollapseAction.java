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

import java.util.Iterator;

import net.sourceforge.texlipse.editor.TexProjectionAnnotation;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Action for collapsing code folds. Collapses either the fold the cursor is
 * currently in or all the folds contained in the selected area.
 * 
 * @author Oskar Ojala
 */
public class TexCollapseAction implements IEditorActionDelegate {
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
        // get the selection and determine if we want to collapse the current fold
        // or all folds contianed by the current selection
        TexSelections selection = new TexSelections(getTextEditor());
        
        int firstOffset = selection.getStartLine().getOffset();
        int lastOffset = selection.getEndLine().getOffset();
        
        ProjectionAnnotationModel model = (ProjectionAnnotationModel) getTextEditor()
        .getAdapter(ProjectionAnnotationModel.class);
        
        if (model != null) {
            if (firstOffset == lastOffset) {
                collapseDeepestMatching(model, firstOffset);
            } else {
                collapseAllContained(model, firstOffset, lastOffset);
            }
        }
    }
    
    /**
     * Collapses the deepest annotation that contains the given offset
     * 
     * @param model The annotation model to use
     * @param offset The offset inside the document
     */
    private void collapseDeepestMatching(ProjectionAnnotationModel model, int offset) {
        TexProjectionAnnotation toCollapse = null;
        for (Iterator iter = model.getAnnotationIterator(); iter.hasNext();) {
            TexProjectionAnnotation tpa = (TexProjectionAnnotation) iter.next();
            if (tpa.contains(offset)) {
                if (toCollapse != null) {
                    if (tpa.isDeeperThan(toCollapse))
                        toCollapse = tpa;
                } else {
                    toCollapse = tpa;
                }
            }
        }
        if (toCollapse != null) {
            model.collapse(toCollapse);
        }
    }
    
    /**
     * Collapses all annotations that are completely contained in the interval
     * defined by <code>startOffset</code> and <code>endOffset</code>.
     * 
     * @param model The annotation model to use
     * @param startOffset The document offset of the start of the interval
     * @param endOffset The document offset of the end of the interval
     */
    private void collapseAllContained(ProjectionAnnotationModel model,
            int startOffset, int endOffset) {
        for (Iterator iter = model.getAnnotationIterator(); iter.hasNext();) {
            TexProjectionAnnotation tpa = (TexProjectionAnnotation) iter.next();
            if (tpa.isBetween(startOffset, endOffset)) {
                model.collapse(tpa);
            }
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
