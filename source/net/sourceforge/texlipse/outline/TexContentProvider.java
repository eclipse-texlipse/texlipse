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

import java.util.List;

import net.sourceforge.texlipse.model.OutlineNode;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;


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
