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
import java.util.List;
import java.util.Map;

/**
 * Container class for outline input stuff. Includes the outline tree
 * root list and lists of nodes of each OutlineNode type. In addition
 * contains the depth of the outline tree. 
 * 
 * @author Taavi Hupponen
 */
public class TexOutlineInput {

	private List<OutlineNode> rootNodes;
	private int treeDepth;
	private Map<Integer, List<OutlineNode>> typeLists;
	
	/**
     * The constructor.
     * 
	 * @param rootNodes rootNode list
	 */
	public TexOutlineInput(List<OutlineNode> rootNodes) {
		this.rootNodes = rootNodes;
		typeLists = new HashMap<Integer, List<OutlineNode>>();
        treeDepth = -1;
	}
	
    /**
     * Adds node to specific type list.
     * 
     * @param node to be added to type list
     */
	public void addNode(OutlineNode node) {
		// get list for this type
		List<OutlineNode> typeList = typeLists.get(node.getType());
		
		// if no list for this type exists yet, create one
		if (typeList == null) {
			typeList = new ArrayList<OutlineNode>();
			typeLists.put(node.getType(), typeList);
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
	public List<OutlineNode> getTypeList(int nodeType) {
		if (typeLists.containsKey(nodeType)) {
			return typeLists.get(nodeType);
		} else {
			return null;
		}
	}

    /**
	 * Returns the root node list.
     * 
     * @return the root nodes
	 */
	public List<OutlineNode> getRootNodes() {
		return rootNodes;
	}
	
    /**
	 * @param rootNodes The rootNodes to set.
	 */
	public void setRootNodes(List<OutlineNode> rootNodes) {
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
        for (OutlineNode node : rootNodes) {
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
        List<OutlineNode> children = node.getChildren();
        int maxDepth = parentDepth + 1;
        if (children != null) {
            for (OutlineNode child : children) {
                int localDepth = handleNode(child, parentDepth + 1);
                if (localDepth > maxDepth) {
                    maxDepth = localDepth;
                }
            }
        }
        return maxDepth;
    }

}
