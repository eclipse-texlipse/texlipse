/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibeditor;

import java.util.List;

import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;


/**
 * An outline page for the BibTeX-editor.
 * 
 * @author Oskar Ojala
 */
public class BibOutlinePage extends ContentOutlinePage {

    public final static String SEGMENTS = "__bib_segments";
    
    protected List content;
    protected ITextEditor editor;
    
    /**
     * Constructs a new outline page
     * 
     * @param textEditor The editor that this outline is associated with
     */
    public BibOutlinePage(ITextEditor textEditor) {
        super();
        this.editor = textEditor;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        
        super.createControl(parent);
        TreeViewer viewer = getTreeViewer();        
        viewer.setContentProvider(new BibContentProvider(editor.getDocumentProvider().getDocument(editor.getEditorInput())));
        viewer.setLabelProvider(new LabelProvider());
        viewer.addSelectionChangedListener(this);
        
        if (this.content != null) {
            viewer.setInput(this.content);
        }
    }
    
    /**
     * Updates the outline with new content.
     * 
     * @param content The new content of the outline
     */
    public void update(List content) {
        this.content = content;
        
        TreeViewer viewer = getTreeViewer();
        
        if (viewer != null) {
            Control control= viewer.getControl();
            if (control != null && !control.isDisposed()) {
                control.setRedraw(false);
                viewer.setInput(this.content);
                control.setRedraw(true);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    public void selectionChanged(SelectionChangedEvent event) {
        super.selectionChanged(event);

        ISelection selection = event.getSelection();
        if (selection.isEmpty()) {
            editor.resetHighlightRange();
        } else {
            ReferenceEntry be = (ReferenceEntry) ((IStructuredSelection) selection).getFirstElement();
            Position position = be.position;
            try {
                editor.setHighlightRange(position.getOffset(), position.getLength(), true);
//                editor.setHighlightRange(start, length, true);
            } catch (IllegalArgumentException x) {
                editor.resetHighlightRange();
            }
        }        
    }
    
}
