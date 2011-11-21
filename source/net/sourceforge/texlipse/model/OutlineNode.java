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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.Position;

/**
 * Node of the outline tree. Created during parsing of the document.
 * 
 * In addition to outline, used by code folder.
 * 
 * @author Laura Takkinen, Taavi Hupponen, Oskar Ojala
 */
public class OutlineNode {
    
    // These should be allocated between -1 to 100
    public static final int TYPE_DOCUMENT = -1;
    public static final int TYPE_PART = 0;
    public static final int TYPE_CHAPTER = 1;
    public static final int TYPE_SECTION = 2;
    public static final int TYPE_SUBSECTION = 3;
    public static final int TYPE_SUBSUBSECTION = 4;
    public static final int TYPE_PARAGRAPH = 5;
        
    public static final int TYPE_ENVIRONMENT = 13;
    public static final int TYPE_PREAMBLE = 14;
    public static final int TYPE_LABEL = 20;
    //public static final int TYPE_ERROR = 99;
    public static final int TYPE_INPUT = 45;
    
    private String name;
    private int type;
    private int beginLine, endLine;
    private int offsetOnLine;
    private int declarationLength;
    private OutlineNode parent;
    private ArrayList<OutlineNode> children;
    private Position position;
    private IFile file;

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
     * The constructor.
     * 
     * @param name name of the node
     * @param type type of the node
     * @param beginLine beginLine of the text of the node
     * @param offset The offset on the starting line
     * @param length The length of the command starting this node
     */
    public OutlineNode(String name, int type, int beginLine, int offset, int length) {
        this.name = name;
        this.type = type;
        this.beginLine = beginLine;
        this.offsetOnLine = offset;
        this.declarationLength = length;
    }

    public OutlineNode copy(IFile texFile) {
        OutlineNode on = new OutlineNode(name, type, beginLine, offsetOnLine, declarationLength);
        on.endLine = endLine;
        on.position = position;
        on.file = texFile;
        return on;
    }
    
    /**
     * Adds a child to this node.
     * 
     * @param child the child to be added
     */
    public void addChild(OutlineNode child) {
        if (this.children == null)
            this.children = new ArrayList<OutlineNode>();
        this.children.add(child);
    }

    /**
     * Adds a child to this node at the specified position.
     * 
     * @param child the child to be added
     * @param index the index of the child
     */
    public void addChild(OutlineNode child, int index) {
        if (this.children == null)
            this.children = new ArrayList<OutlineNode>();
        this.children.add(index, child);
    }
    
    /**
     * Deletes a child from the node
     * @param child the child to delete
     * @return true if the child exists, else false
     */
    public boolean deleteChild (OutlineNode child) {
        return this.children.remove(child);
    }
    
    /**
     * @return Returns the children.
     */
    public ArrayList<OutlineNode> getChildren() {
        return children;
    }
    /**
     * @param children The children to set.
     */
    public void setChildren(ArrayList<OutlineNode> children) {
        this.children = children;
    }
    
    /**
     * @return true, if the node has at least one children
     */
    public boolean hasChildren() {
        return (this.children != null && this.children.size() > 0);
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
     * @return the file reference this node belongs to.
     */
    public IFile getIFile() {
        return file;
    }
    
    /**
     * 
     * @param file the file reference this node belongs to.
     */
    public void setIFile(IFile file) {
        this.file = file;
    }

    /**
     * @return Returns the declarationLength.
     */
    public int getDeclarationLength() {
        return declarationLength;
    }

    /**
     * @return Returns the offsetOnLine.
     */
    public int getOffsetOnLine() {
        return offsetOnLine;
    }

    /**
     * @return String presentation of the node
     */
    public String toString() {
        if (this.position == null) {
            return this.type + " " + this.name + " (null position) " +
                super.toString();
        }
        else {
            return this.type + " " + this.name + " " + 
            this.position.getOffset() + " " + this.position.getLength() +
            super.toString();
        }
    }

    /**
     * Returns one type smaller (=more important) than the
     * given type.
     * 
     * @param type The current type
     * @return A smaller (=more important) type
     */
    public static int getSmallerType(int type) {
        // TODO think about the non-hierarchical types
        if (type <= TYPE_PARAGRAPH) {
            return type - 1;
        }
        switch (type) {
        case TYPE_ENVIRONMENT:
            return TYPE_PARAGRAPH;
        case TYPE_PREAMBLE:
            return TYPE_ENVIRONMENT;
        case TYPE_INPUT:
            return TYPE_PREAMBLE;
        }
        return TYPE_DOCUMENT;
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
