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
import java.util.Iterator;
import java.util.ListIterator;

import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;


/**
 * Does code-folding for the BibTeX-editor.
 * 
 * @author Oskar Ojala
 */
public class BibCodeFolder {

    private BibEditor editor;
    private ProjectionAnnotationModel model;
    private ArrayList oldNodes;
//    private ArrayList<TexProjectionAnnotation> oldNodes;
    
    /**
     * Creates a new code folder.
     * 
     * @param editor The editor to which this folder is associated
     */
    public BibCodeFolder(BibEditor editor) {
        this.editor = editor;
    }
    
    /**
     * Updates the code folds.
     * 
     * @param outline The outline data structure containing the document positions
     */
    public void update(ArrayList outline) {
        model = (ProjectionAnnotationModel)editor.getAdapter(ProjectionAnnotationModel.class);
        if (model != null) {
            this.addMarks(outline);
        }
    }
    
    /**
     * Manages adding and removing the folding marks from the editor.
     * 
     * @param outline The outline data structure containing the document positions
     */
    private void addMarks(ArrayList outline) {
        oldNodes = new ArrayList();

        // save old nodes
        for (Iterator iter = model.getAnnotationIterator(); iter.hasNext();) {
            oldNodes.add((BibProjectionAnnotation) iter.next());
        }

        markTreeNodes(outline);
        
        BibProjectionAnnotation[] deletes = new BibProjectionAnnotation[oldNodes.size()];
        oldNodes.toArray(deletes);
        model.modifyAnnotations(deletes, null, null);
    }

    /**
     * Adds new folding markers for positions that do not yet have markers.
     * 
     * @param outline The outline data structure containing the document positions
     */
    private void markTreeNodes(ArrayList outline) {
        markloop:
        for (ListIterator iter = outline.listIterator(); iter.hasNext();) {
            ReferenceEntry re = (ReferenceEntry) iter.next();

            Position pos = re.position;
            for (ListIterator li2 = oldNodes.listIterator(); li2.hasNext();) {
                BibProjectionAnnotation cAnnotation = (BibProjectionAnnotation) li2.next();
                if (cAnnotation.isSame(re)) {
                    oldNodes.remove(cAnnotation);
//                    model.modifyAnnotationPosition(cAnnotation, pos);
                    continue markloop;
                }
            }
            model.addAnnotation(new BibProjectionAnnotation(re), pos);
        }
    }
}
