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

import org.eclipse.jface.text.Position;

/**
 * A class for containing LaTeX references (\label and BibTeX)
 * 
 * @author Oskar Ojala
 */
public final class ReferenceEntry extends AbstractEntry {

    /**
     * A descriptive text of the reference
     */
    public String info;
    /**
     * The end line of the reference declaration (used for BibTeX editing)
     */
    public int endLine;
    /**
     * The document position of the reference declaration (used for BibTeX editing)
     */
    public Position position;

    /**
     * The filename where the reference lives in
     */
    public String fileName;
    
    /**
     * Constructs a new entry with the given key (reference key/name)
     * 
     * @param key Reference key
     */
    public ReferenceEntry(String key) {
        this.key = key;
    }
    
    /**
     * Constructs a new entry with the given key (Reference key/name)
     * and a descriptive text telling something about the reference
     * (used for BibTeX).
     * 
     * @param key Reference key
     * @param info A descriptive text of the reference
     */
    public ReferenceEntry(String key, String info) {
        this.key = key;
        this.info = info;
    }
        
    /**
     * Sets the document position of this entry.
     * 
     * @param docOffset Offset from the document start
     * @param length Length of the position
     */
    public void setPosition(int docOffset, int length) {
        this.position = new Position(docOffset, length);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return key;
    }
}
