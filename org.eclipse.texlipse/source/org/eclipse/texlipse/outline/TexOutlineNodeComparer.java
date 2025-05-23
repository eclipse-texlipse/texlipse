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

package org.eclipse.texlipse.outline;

import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.texlipse.model.OutlineNode;


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
		if (a == b) return true;
		
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
