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

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object ae) {
        return key.compareTo(((AbstractEntry) ae).key);
    }
}
