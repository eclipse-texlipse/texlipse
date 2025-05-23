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

import java.util.List;
import java.util.Stack;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.texlipse.model.OutlineNode;


/**
 * Content provider for the TeX document outline. Contains the tree
 * (list of root nodes) of OutlineNodes gotten from TexDocumentModel.
 * 
 * @author Taavi Hupponen, Laura Takkinen
 */
public class TexContentProvider implements ITreeContentProvider {

    
	private List<OutlineNode> rootElements;	
	private ViewerFilter filter;
	
	public TexContentProvider(ViewerFilter filter) {
		this.filter = filter;
	}
	
	/** 
	 * Gets the children of the given parent node of the tree.
	 * 
     * @param parentElement parent node of the tree
	 * @return list of children 
	 */
	public Object[] getChildren(Object parentElement) {
		OutlineNode node = (OutlineNode) parentElement;
		List<OutlineNode> children = node.getChildren();
		if (children != null && children.size() != 0) {
			return children.toArray();
		} else {
			return null;
		}
	}

	/** 
	 * Gets the parent of the given tree node.
	 * 
     * @param element node of the tree
	 * @return parent node of the element 
	 */
	public Object getParent(Object element) {
		return ((OutlineNode)element).getParent(); 
	}

	/** 
	 * Checks if the given tree node has children nodes.
	 * 
     * @param element node of the tree
	 * @return true if element has children, otherwise false
	 */
	public boolean hasChildren(Object element) {
		
		OutlineNode node = (OutlineNode) element;
		List<OutlineNode> children = node.getChildren();
		
		if (children != null && children.size() != 0) {
			//Check if at least one element is not filtered
			for (OutlineNode n : children) {
				if (filter.select(null, element, n)) return true;
			}
			return false;
		} else {
			return false;
		}
	}

	/**
     * Gets the root elements of the outline tree.
     * 
     * @return the root elements of the outline tree
	 */
	public Object[] getElements(Object inputElement) {
		return this.rootElements.toArray();
	}

	/**
     * Disposes the tree that is the root element list.
     * 
	 */
	public void dispose() {
	    this.rootElements = null;
    }

	/**
	 * Tries to update the elements in the viewer without rebuilding the whole
	 * tree. This is only possible, if only the positions of the elements changed.
	 * @param viewer
	 * @param newInput
	 * @return Return true, if the update was successful. If the return value is false, the state
	 * of the ContentProvider is not in sync with the content in newInput. 
	 */
	public boolean updateElements(Viewer viewer, List<OutlineNode> newInput) {
		if (rootElements == null) {
			return false;
		}
		
		Stack<OutlineNode> stackNew = new Stack<OutlineNode>();
		Stack<OutlineNode> stackOld = new Stack<OutlineNode>();
		stackNew.addAll(newInput);
		stackOld.addAll(rootElements);
		if (stackOld.size() != stackNew.size()) return false;

		while (!stackOld.isEmpty()) {
			OutlineNode o = stackOld.pop();
			if (stackNew.isEmpty()) return false;
			OutlineNode n = stackNew.pop();
			
			//Do not update if the number of elements is different
			if (stackOld.size() != stackNew.size()) return false;
			
			//Do not update if the name or the type is different
			if (o.getType() != n.getType() || !o.getName().equals(n.getName())) return false;
			
			o.update(n);
			if (n.hasChildren()) {
				if (o.hasChildren()) {
					stackNew.addAll(n.getChildren());
					stackOld.addAll(o.getChildren());
				}
				else return false;
			}
		}
		
		return true;
	}
	
	/**
     * Replaces the root elements list with new root elements list.
     * 
     * @param viewer the assosiated viewer
     * @param oldInput the old root element list
     * @param newInput the new root element list
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.rootElements = (List<OutlineNode>)newInput;
	}
}
