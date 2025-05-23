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

package org.eclipse.texlipse.editor;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.texlipse.model.OutlineNode;


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
	 * @return The position data of this annotation
	 */
	public Position getPosition() {
		return node.getPosition();
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
	
	/**
	 * Checks whether the given offset is contained within this annotation
	 * 
	 * @param offset The offset inside the document that this annotation belongs to
	 * @return True if the offset is contained, false otherwise
	 */
	public boolean contains(int offset) {
		Position pos = node.getPosition();
		if (offset >= pos.getOffset() 
				&& offset < (pos.getOffset() + pos.getLength()))
			return true;
		return false;
	}
	
	/**
	 * Checks whether this annotation is deeper than the given annotation
	 * by comparing offsets and lengths of the annotations. 
	 * 
	 * @param tpa The annotation to compare to
	 * @return True if this annotation is deeper, false otherwise
	 */
	public boolean isDeeperThan(TexProjectionAnnotation tpa) {
		Position thisPos = node.getPosition();
		Position alienPos = tpa.getPosition();
		if (thisPos.offset > alienPos.offset
				&& (thisPos.offset + thisPos.length) <= (alienPos.offset + alienPos.length))
			return true;
		return false;
	}
	
	public boolean isBetween(int startOffset, int endOffset) {
		Position pos = node.getPosition();
		if (startOffset <= pos.offset
				&& (pos.length + pos.offset) < endOffset)
			return true;
		return false;
	}
}
