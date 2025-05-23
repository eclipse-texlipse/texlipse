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

package org.eclipse.texlipse.model;

/**
 * Holds a single document reference (eg. a reference made with the \ref 
 * or \cite command).
 * 
 * @author Oskar Ojala
 */
public final class DocumentReference extends AbstractEntry {
    
    private int pos;
    private int length;
    
    /**
     * Constructs a new object for holding a document reference (eg. 
     * a reference made with the \ref or \cite command.)
     * 
     * @param key The reference key
     * @param line The line the reference occurs on
     * @param pos The position from the start of the line the reference occurs on
     * @param length The length of the reference text
     */
    public DocumentReference(String key, int line, int pos, int length) {
        this.key = key;
        this.startLine = line;
        this.pos = pos;
        this.length = length;
    }

    /**
     * @return Returns the key.
     */
    public String getKey() {
        return key;
    }
    /**
     * @return Returns the length.
     */
    public int getLength() {
        return length;
    }
    /**
     * @return Returns the line.
     */
    public int getLine() {
        return startLine;
    }
    /**
     * @return Returns the pos.
     */
    public int getPos() {
        return pos;
    }
}
