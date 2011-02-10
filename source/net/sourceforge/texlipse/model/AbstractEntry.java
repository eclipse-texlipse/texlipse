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
 * A superclass for the different types of entries that can occur
 * in LaTeX file, eg. command definitions or reference declarations.
 * 
 * This is essentially handled somewhat like a struct in C, due to
 * efficiency resons.
 * 
 * @author Oskar Ojala
 */
public abstract class AbstractEntry implements Comparable {

    /**
     * The key (ie. the name) of the entry
     */
    public String key;
    /**
     * The line where the entry is declared
     */
    public int startLine;
    /**
     * The document position of the reference declaration (used for BibTeX editing)
     */
    public Position position;
    /**
     * The filename where the reference lives in
     */
    public String fileName;

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object ae) {
        return key.compareTo(((AbstractEntry) ae).key);
    }
    
    public boolean equals(AbstractEntry ae) {
    	return (key.equals(ae.key));
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
    
    public String getkey(boolean lowerCase) {
        if (lowerCase) return key.toLowerCase();
        return key;
    }
}
