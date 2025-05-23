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
 * A document parse error message.
 * 
 * @author Oskar Ojala
 */
public class ParseErrorMessage {
    
    private int line;
    private int pos;
    private int length;
    private String msg;
    private int severity;
    
    /**
     * Constructs a new error message.
     * 
     * @param line The line the error occurs on
     * @param pos The position of the error on that line
     * @param length The length of the erroneous input (from <code>pos</code> onwards)
     * @param msg The error message
     * @param severity The severity of the error (as defined in <code>IMarker</code>)
     */
    public ParseErrorMessage(int line, int pos, int length, String msg, int severity) {
        this.line = line;
        this.pos = pos;
        this.length = length;
        this.msg = msg;
        this.severity = severity;
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
        return line;
    }
    /**
     * @return Returns the msg.
     */
    public String getMsg() {
        return msg;
    }
    /**
     * @return Returns the pos.
     */
    public int getPos() {
        return pos;
    }
    
    /**
     * @return Returns the severity.
     */
    public int getSeverity() {
        return severity;
    }
}
