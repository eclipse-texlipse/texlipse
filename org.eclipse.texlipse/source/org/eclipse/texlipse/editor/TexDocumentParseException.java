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

package org.eclipse.texlipse.editor;

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
