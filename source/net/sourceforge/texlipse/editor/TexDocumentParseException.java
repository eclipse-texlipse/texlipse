/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

/**
 * An exception for throwing when parsing a document fails.
 *  
 * @author Oskar Ojala
 */
public class TexDocumentParseException extends Exception {
    
    /**
     * Constructs a new exception.
     */
    public TexDocumentParseException() {
        super();
    }
    
    /**
     * Constructs a new exception.
     * 
     * @param message A message explaining the reason for this exception
     */
    public TexDocumentParseException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception.
     * 
     * @param cause The exception that caused this one
     */
    public TexDocumentParseException(Throwable cause) {
        super(cause);
    }    
}
