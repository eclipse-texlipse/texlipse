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

import org.eclipse.jface.text.Position;

/**
 * Node of the outline tree. Created during parsing of the document.
 * 
 * In addition to outline, used by code folder.
 * 
 * @author Laura Takkinen, Taavi Hupponen, Oskar Ojala
 */
public class OutlineNode {
    
    public static final int TYPE_PART = 0;
    public static final int TYPE_CHAPTER = 1;
    public static final int TYPE_SECTION = 2;
    public static final int TYPE_SUBSECTION = 3;
    public static final int TYPE_SUBSUBSECTION = 4;
    public static final int TYPE_PARAGRAPH = 5;
        
    public static final int TYPE_ENVIRONMENT = 13;
    public static final int TYPE_PREAMBLE = 14;
    //public static final int TYPE_ERROR = 99;
    
    private String name;
    private int type;
    private int beginLine, endLine;
    private OutlineNode parent;
    private ArrayList children;
    private Position position;
    
    /**
     * The constructor.
     * 
     * @param name name of the node
     * @param type type of the node
     * @param beginLine beginLine of the text of the node
     * @param parent the parent of the node
     */
    public OutlineNode(String name, int type, int beginLine, OutlineNode parent) {
        this.name = name;
        this.type = type;
        this.beginLine = beginLine;
        this.parent = parent;
    }

    /**
     * Adds a child to this node.
     * 
     * @param child the child to be added
     */
    public void addChild(OutlineNode child) {
        if (this.children == null)
            this.children = new ArrayList();
        this.children.add(child);
    }

    
    /**
     * @return Returns the children.
     */
    public ArrayList getChildren() {
        return children;
    }
    /**
     * @param children The children to set.
     */
    public void setChildren(ArrayList children) {
        this.children = children;
    }
    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }
    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return Returns the parent.
     */
    public OutlineNode getParent() {
        return parent;
    }
    /**
     * @param parent The parent to set.
     */
    public void setParent(OutlineNode parent) {
        this.parent = parent;
    }
    /**
     * @return Returns the type.
     */
    public int getType() {
        return type;
    }
    /**
     * @param type The type to set.
     */
    public void setType(int type) {
        this.type = type;
    }
    /**
     * @return Returns the beginLine.
     */
    public int getBeginLine() {
        return beginLine;
    }
    /**
     * @param beginLine The beginLine to set.
     */
    public void setBeginLine(int beginLine) {
        this.beginLine = beginLine;
    }
    /**
     * @return Returns the endLine.
     */
    public int getEndLine() {
        return endLine;
    }
    /**
     * @param endLine The endLine to set.
     */
    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }
    
    
    /**
     * @return Returns the position.
     */
    public Position getPosition() {
        return position;
    }
    /**
     * @param position The position to set.
     */
    public void setPosition(Position position) {
        this.position = position;
    }
 
    /**
     * @return String presentation of the node
     */
    public String toString() {
    	return this.getType() + " " + this.getName() + " " + 
        		this.getPosition().getOffset() + " " + this.getPosition().getLength() +
				super.toString();
    }


    /*
    public boolean likelySame(OutlineNode on) {
    	if (on.getType() != this.getType() || !this.getName().equals(on.getName())) {
    		return false;
        }
        //pos -> see if the iterator preserves order
        
        // make approximation
        
        if (!this.getPosition().equals(on.getPosition())) {
        	return false;
        }
        
        return true;
    }
    */
}
