/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.outline;

import net.sourceforge.texlipse.model.OutlineNode;

import org.eclipse.jface.viewers.IElementComparer;


/**
 * For now we use OutlineNode.getName() for comparing the nodes and
 * for determining the hashCode for a node. Obviously this is not very 
 * smart and has some unwanted side effects.
 * 
 * The better solution would be to use the positions instead of node 
 * names. However for some unknown reason, the offset and length of the
 * old positions do not get updated and thus the position is not equal
 * to the new position.
 * 
 * TODO Try to get the Position matching to work.
 * 
 * @author Taavi Hupponen
 */
public class TexOutlineNodeComparer implements IElementComparer {

	/**
	 * Compares the names of OutlineNodes.
     * 
     * @see org.eclipse.jface.viewers.IElementComparer#equals(java.lang.Object, java.lang.Object)
	 */
	public boolean equals(Object a, Object b) {
		
		if (a instanceof OutlineNode && b instanceof OutlineNode) {
			OutlineNode node1 = (OutlineNode)a;
			OutlineNode node2 = (OutlineNode)b;

			if (node1.getName().equals(node2.getName())) {
				return true;
			} else {
                return false;
			}
		}
		return a.equals(b);
	}

	/**
     * Uses the OutlineNode name for getting the hashCode.
	 * 
	 * @see org.eclipse.jface.viewers.IElementComparer#hashCode(java.lang.Object)
	 */
	public int hashCode(Object element) {
		
		if (element instanceof OutlineNode) {
			OutlineNode node = (OutlineNode)element;
			return ((OutlineNode)element).getName().hashCode();
		}
		return element.hashCode();
	}
}
