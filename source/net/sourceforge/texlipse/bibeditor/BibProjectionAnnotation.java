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

import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.jface.text.source.projection.ProjectionAnnotation;


/**
 * Projection annotation for BibTeX code folding.
 * 
 * @author Oskar Ojala
 */
public class BibProjectionAnnotation extends ProjectionAnnotation {

    private ReferenceEntry node;
    
    /**
     * Creates a new annotation for the given node
     * 
     * @param node The node for creating the annotation
     */
    public BibProjectionAnnotation(ReferenceEntry node) {
        super();
        this.node = node;
    }

    /**
     * Creates a new annotation for the given node
     * 
     * @param node The node for creating the annotation
     * @param isCollaped Whether this node should initially be collapsed or not
     */
    public BibProjectionAnnotation(ReferenceEntry node, boolean isCollapsed) {
        super(isCollapsed);
        this.node = node;
    }

	
    /**
     * Tests whether this annotation corresponds to the same
     * document area as the argument.
     * 
     * @param re The entry to compare to
     * @return True if this object and the given entry are essentially the same
     */
    public boolean isSame(ReferenceEntry re) {
        if (node.key.equals(re.key))
            return true;
        return false;
    }
}
