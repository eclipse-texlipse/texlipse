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

import java.util.HashSet;

import net.sourceforge.texlipse.model.OutlineNode;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;


/**
 * Filter for the TeX document outline. Controls which type of 
 * OutlineNodes and which ones of the type ENVIRONMENT are visible
 * in the tree.
 * 
 * Internally contains two HashMaps, one for visible OutlineNode types 
 * and one for visible environment names.
 * 
 * @author Taavi Hupponen
 */
public class TexOutlineFilter extends ViewerFilter {
    
    private HashSet visibleTypes;
    private HashSet visibleEnvironments;
    
    public TexOutlineFilter() {
        super();
        visibleTypes = new HashSet();
        visibleEnvironments = new HashSet();
    }
    
    /**
     * Returns whether the given element is visible.
     * 
     * @return true if given element should be visible in the tree
     *
     * @see org.eclipse.jface.viewers.ViewerFilter#select(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        OutlineNode node = (OutlineNode) element;
        Integer nodeType = new Integer(node.getType());
        
        // visible type
        if (visibleTypes.contains(nodeType)) {
            
            // other than floating environment
            if (nodeType.intValue() != OutlineNode.TYPE_ENVIRONMENT) {
                return true;
            }
            
            // visible floating environment
            else if (visibleEnvironments.contains(node.getName())) {
                return true;
            }
            
            // hidden floating environment
            else {
                return false;
            }
        } 
        // hidden type
        else {
            return false;
        }
    }
    
    /**
     * Toggles a given OutlineNode type visible or hidden. 
     * 
     * @param nodeType the OutlineNode type
     * @param visible True if type should be visible, false if hidden
     */    
    public void toggleType(int nodeType, boolean visible) {
        Integer type = new Integer(nodeType);
        if (visible) {
            visibleTypes.add(type);
        } else {
            visibleTypes.remove(type);
        }
    }
    
    /**
     * Toggles a given environment visible or hidden. 
     * 
     * @param environment (name) 
     * @param visible True if the environment should be visible, false otherwise
     */    
    public void toggleEnvironment(String environment, boolean visible) {
        
        if (visible) {
            visibleEnvironments.add(environment);
        } else {
            visibleEnvironments.remove(environment);
        }
    }
            

    /**
     * Returns whether given environment is currently visible.
     * 
     * @param environment
     * @return true if given environment is currently visible
     */
    public boolean isEnvironmentVisible(String environment) {
        if (visibleEnvironments.contains(environment)) {
            return true;
        }
        return false;
    }

    /**
     * Returns whether given OutlineNode type is currently visible.
     * 
     * @param type
     * @return true if given type is currently visible
     */
	public boolean isTypeVisible(int type) {
	    if (visibleTypes.contains(new Integer(type))) {
	        return true;
	    }
	    return false;
	}
	
	/**
	 * Resets the filter. After reset no type or environment is visible.
	 *
	 */
	public void reset() {
		this.visibleTypes.clear();
		this.visibleEnvironments.clear();
	}

}
