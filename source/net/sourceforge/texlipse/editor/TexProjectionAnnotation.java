/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import net.sourceforge.texlipse.model.OutlineNode;

import org.eclipse.jface.text.source.projection.ProjectionAnnotation;


/**
 * Projection annotation for LaTeX code folding.
 * 
 * @author Oskar Ojala
 */
public class TexProjectionAnnotation extends ProjectionAnnotation {

    /**
     * The OutlineNode this annotation corresponds to
     */
    private OutlineNode node;

    /**
     * Creates a new annotation for the given node
     * 
     * @param node The node for creating the annotation
     */
    public TexProjectionAnnotation(OutlineNode node) {
        super();
        this.node = node;
    }

    /**
     * Creates a new annotation for the given node
     * 
     * @param node The node for creating the annotation
     * @param isCollaped Whether this node should initially be collapsed or not
     */
    public TexProjectionAnnotation(OutlineNode node, boolean isCollaped) {
        super(isCollaped);
        this.node = node;
    }
    
    /**
     * Tests whether this annotation corresponds to the same
     * document area as the argument.
     * 
     * @param on The node to compare to
     * @return True if this object and the given entry are essentially the same
     */
    public boolean likelySame(OutlineNode on) {
        if (on.getType() != node.getType() || !node.getName().equals(on.getName())) {
            return false;
        }
        
        if (!node.getPosition().equals(on.getPosition())) {
            return false;
        }
        return true;
    }
}
