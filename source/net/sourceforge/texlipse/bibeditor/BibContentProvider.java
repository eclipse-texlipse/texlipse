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

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


/**
 * BibTeX-editor content outline content provider.
 * 
 * @author Oskar Ojala
 */
public class BibContentProvider implements ITreeContentProvider {
    
    private IPositionUpdater fPositionUpdater = new DefaultPositionUpdater(BibOutlinePage.SEGMENTS);
    private List rootElements = new ArrayList(0);
    private IDocument document;
    
    /**
     * Creates a new content provider for the given document.
     * 
     * @param document The document associated with this editor view
     */
    public BibContentProvider(IDocument document) {
        this.document = document;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput != null) {
            this.rootElements = (List)newInput;
            if (document != null)
                document.addPositionUpdater(fPositionUpdater);
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
        if (rootElements != null) {
            rootElements.clear();
            rootElements = null;
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object element) {
        return rootElements.toArray();
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
        return element == rootElements;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
        if (element instanceof ReferenceEntry)
            return rootElements;
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object element) {
        if (element == rootElements)
            return rootElements.toArray();
        return new Object[0];
    }
}
