/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Container class for outline input stuff. Includes the outline tree
 * root list and lists of nodes of each OutlineNode type. In addition
 * contains the depth of the outline tree. 
 * 
 * @author Taavi Hupponen
 */
public class TexOutlineInput {

	private ArrayList rootNodes;
	private int treeDepth;
	private HashMap typeLists;
	
	/**
     * The constructor.
     * 
	 * @param rootNodes rootNode list
	 */
	public TexOutlineInput(ArrayList rootNodes) {
		this.rootNodes = rootNodes;
		typeLists = new HashMap();
        treeDepth = -1;
	}
	
    /**
     * Adds node to specific type list.
     * 
     * @param node to be added to type list
     */
	public void addNode(OutlineNode node) {
		// get list for this type
		Integer nodeType = new Integer(node.getType());
		ArrayList typeList = (ArrayList)typeLists.get(nodeType);
		
		// if no list for this type exists yet, create one
		if (typeList == null) {
			typeList = new ArrayList();
			typeLists.put(nodeType, typeList);
		}
		
		// add node to type list
		typeList.add(node);
	}
	
    /**
     * Returns a list containing all the nodes of certain type.
     * 
     * @param nodeType The type of nodes to be returned
     * @return The list containing the nodes of given type, null
     * if no nodes of that type exist.
     */
	public ArrayList getTypeList(int nodeType) {
		Integer key = new Integer(nodeType);
		if (typeLists.containsKey(key)) {
			return (ArrayList) typeLists.get(key);
		} else {
			return null;
		}
	}

    /**
	 * Returns the root node list.
     * 
     * @return the root nodes
	 */
	public ArrayList getRootNodes() {
		return rootNodes;
	}
	
    /**
	 * @param rootNodes The rootNodes to set.
	 */
	public void setRootNodes(ArrayList rootNodes) {
		this.rootNodes = rootNodes;
	}

    /**
	 * @return Returns the treeDepth.
	 */
	public int getTreeDepth() {
        //Calculate TreeDepth on the fly
		if (treeDepth == -1)
            calculateTreeDepth();
        return treeDepth;
	}
	/**
	 * @param treeDepth The treeDepth to set.
	 */
	public void setTreeDepth(int treeDepth) {
		this.treeDepth = treeDepth;
	}
    
    /**
     * Calculates and sets the depth of the tree
     *
     */
    public void calculateTreeDepth() {
        treeDepth = 0;
        for (Iterator iter = rootNodes.iterator(); iter.hasNext();) {
            OutlineNode node = (OutlineNode) iter.next();
            int localDepth = handleNode(node, 0);
            if (localDepth > treeDepth)
                treeDepth = localDepth;
        }
    }
    
    /**
     * Calculates the depth of the tree. Used recursively.
     * 
     * @param node the current node.
     * @param parentDepth the depth to the parent.
     * @return
     */
    private int handleNode(OutlineNode node, int parentDepth) {

        // iterate through the children
        List children = node.getChildren();
        int maxDepth = parentDepth + 1;
        if (children != null) {
            for (Iterator iter = children.iterator(); iter.hasNext();) {
                int localDepth = handleNode((OutlineNode) iter.next(), parentDepth + 1);
                if (localDepth > maxDepth)
                    maxDepth = localDepth;
            }
        }
        return maxDepth;
    }

}
