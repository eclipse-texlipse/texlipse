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

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;


/**
 * This class handles the line wrapping.
 * 
 * @author Antti Pirinen
 * @author Oskar Ojala
 */
public class HardLineWrap {
    //private int lineLength = 80;
    
    private boolean lastCharOfLine; // True if character is the last of the current line
    private boolean lastCharOfDoc;  // True if character is the last character of document
    private int carPos;	 // The position of the caret
    private int wsLast;  // The last white space position before MAX
    private int wsFirst; // The first white space position after MAX
    private int indentLength;    // The length of the indentation of the current line
    private int nextLineLength;  // The length of next line 
    private int currLineLength;  // The length of current line
    private int MAX;             // The maximum length of line
    private String currLineTxt;  // The text of current line before modification
    private String ld = "\n";    // The default line delimiter
    private TexEditorTools tools;
    
    public HardLineWrap(){
        this.tools = new TexEditorTools();
    }
    /**
     * The actual wrapping method. Based on the <code>IDocument d</code>
     * and <code>DocumentCommand c</code> the method determines how the
     * line must be wrapped. 
     * <p>
     * If there is more than <code>MAX_LENGTH</code>
     * characters at the line, the method tries to detect the last white
     * space before <code> MAX_LENGTH</code>. In case there is none, the 
     * method finds the first white space after <code> MAX_LENGTH</code>.
     * If there is enoght room at the next line, the end of current line 
     * will be added to the next line. Otherwise the new line is added.
     * 
     * @param d 			IDocument
     * @param c 			DocumentCommand
     * @param MAX_LENGTH 	How many characters are allowed at one line.
     */
    public void doWrap(IDocument d, DocumentCommand c, int MAX_LENGTH) {
        //int counter = 0;
        MAX = MAX_LENGTH; 
        //counter = tools.getLineLength(d,c,false);
        currLineLength = tools.getLineLength(d,c,false,0);
        nextLineLength = tools.getLineLength(d,c,false,1);
        indentLength   = tools.getIndentation(d,c).length();
        lastCharOfLine = (tools.getIndexAtLine(d,c, false) == -1 ? true: false);			
        
        if (currLineLength >= MAX && (tools.isWhiteSpace(c) || (
                tools.isLineDelimiter(d,c) && lastCharOfLine))) {
            
            ld = tools.getLineDelimiter(d,c);
            currLineTxt = tools.getStringAt(d,c,true);
            carPos = tools.getIndexAtLine(d,c,true);
            wsLast = tools.getLastWSPosition(currLineTxt, MAX);
            wsFirst = tools.getFirstWSPosition(currLineTxt, MAX);
            lastCharOfDoc  = (tools.getIndexAtLine(d,c, true) == -1 ? true: false);
            if( lastCharOfDoc) {
                carPos = d.getLength();
                try {
                    d.replace(d.getLength(), 0, "\n");
                } catch(BadLocationException e) {
                    System.out.println(e); // FIXME
                }
            }
            
            if ((wsLast < carPos && carPos <= MAX && carPos > indentLength) 
                    ||
                    (wsLast < indentLength && carPos <= wsFirst && carPos > indentLength)
                    ||		
                    (wsLast < indentLength && carPos >= MAX)){
                /*                 MAX  
                 * >###a#CaaaaaaaaaC|aaaa#aaaa
                 *       ^~~~~~~~~~^|  
                 *
                 *  OR
                 *               MAX  
                 * >###aCaaaaaaaaa|aaaC#aaaa
                 *      ^~~~~~~~~~|~~~^
                 * 
                 *  OR
                 *              MAX  
                 * >aaaaaaaaaaaaa|aaaCa#aaaa
                 *               |   ^
                 *    
                 */
                
                wrapCase1(d,c,carPos);
            } else if (carPos <= wsLast){
                /*                 MAX  
                 * >###CaaaaaaaaC#a|aaaa#aaaa
                 *     ^~~~~~~~~^  |  
                 */
                
                wrapCase2(d,c,carPos, wsLast);
            } else if(wsLast > indentLength && carPos > MAX ){
                /*               MAX  
                 * >###aaaaa#aaaaa|Caaa#aaaCaa
                 *                 ^~~~~~~~^
                 */
                
                wrapCase3(d, c, wsLast, carPos);
            } else {
                /*               MAX  
                 * >###aaaaaaaaaaa|a#Caa#aCaaaa
                 *                   ^~~~~^
                 */
                
                wrapCase3(d,c,wsFirst,carPos);
            }
        }	
    }
    

    public void doWrapB(IDocument d, DocumentCommand c, int MAX_LENGTH) {
        try {
            // Get the line of the command excluding delimiter
            IRegion commandRegion = d.getLineInformationOfOffset(c.offset);
            String line = d.get(commandRegion.getOffset(), commandRegion.getLength());
                        
            // TODO check whitespace -> just len == 1 && char == ' ' should do
            //if (line.length() < MAX_LENGTH || !Character.isWhitespace(c.text.charAt(0))) {
            if (line.length() < MAX_LENGTH || !(" ".equals(c.text) || "\t".equals(c.text))) {
                return;
            }
            
            String delim = tools.getLineDelimiter(d, c);
            //String indent = tools.getIndentation(d, c); // TODO check if inside comment
            String indent = tools.getIndentationWithComment(line);
            int breakpos = tools.getLastWSPosition(line, MAX_LENGTH);
            String nextline = tools.getStringAt(d, c, false, 1).trim();
            
            StringBuffer buf = new StringBuffer();

            // Check if the cursor is not at the end of the line
            boolean caretMidLine = false;
            int cursorOnLine = c.offset - commandRegion.getOffset();
            if (cursorOnLine != commandRegion.getLength()) {
                caretMidLine = true;
                if (cursorOnLine <= breakpos) {
                    // We put more text to the buffer and change c.offset, nothing more
                    buf.append(c.text);
                    buf.append(line.substring(cursorOnLine, breakpos)); // trim?
                } else {
                    // We modify the line and everything is handled automatically
                    String startLine = line.substring(0, cursorOnLine);
                    line = startLine + c.text + line.substring(cursorOnLine);
                }
            }

            // Break the line
            if (breakpos < indent.length()) {
                buf.append(delim);
                buf.append(indent);
                breakpos = tools.getLastWSPosition(line, line.length());
                // Check if the line can be broken after limit
                if (breakpos >= indent.length()) {
                    buf.append(line.substring(breakpos + 1).trim()); // TODO trim?
                }
            } else {
                buf.append(delim);
                buf.append(indent);
                buf.append(line.substring(breakpos + 1).trim()); // TODO trim?
                //buf.append(" ");
            }
            
            // If the cursor was at the end of the line, add the typed text
            if (!caretMidLine) {
                buf.append(c.text);
            }

            int indentLengthCorrection = 0;
            
            // Append next line or insert newline
            if (nextline.length() > 0
                    && !tools.isLineCommandLine(nextline)
                    && !tools.isLineCommentLine(nextline)
                    && !tools.isLineItemLine(nextline)
                    && indent.indexOf('%') == -1) {
                if (caretMidLine) {
                    buf.append(" ");
                }
                buf.append(nextline);
            } else {
                buf.append(delim);
                indentLengthCorrection = indent.length();
            }
            
            // Modify the document command so that the wrap is inserted
            // No exceptions can occur here, so if everything went smoothly,
            // Then all of the below will be executed
            c.shiftsCaret = false;
            //c.caretOffset = c.offset + delim.length() + indent.length();

            //c.offset = d.getLineOffset(d.getLineOfOffset(c.offset)) + breakpos;
            if (cursorOnLine > breakpos) {
                c.caretOffset = c.offset + delim.length() + indent.length();
                if (breakpos > indent.length()) {
                    c.offset = commandRegion.getOffset() + breakpos;
                } else {
                    //right size if newline is inserted, too short is next line is appended
                    indentLengthCorrection += delim.length();
                }
            } else {
                c.caretOffset = c.offset + delim.length();
            }
            
            //c.length = buf.length() - 1 - indent.length();
            c.length = buf.length() - delim.length() - indentLengthCorrection;
            //c.caretOffset = newCaretOffset;
            c.text = buf.toString();
            
        } catch(BadLocationException e) {
            // FIXME
       }
    }
    
    /**                MAX  
     * >###a#CaaaaaaaaaC|aaaa#aaaa
     *       ^~~~~~~~~~^|  
     *
     *  OR
     *               MAX  
     * >###aCaaaaaaaaa|aaaC#aaaa
     *      ^~~~~~~~~~|~~~^  
     *
     * @param d
     * @param c
     */
    private void wrapCase1(IDocument d, DocumentCommand c, int txtStart) {
        int newCaretOffset;
        c.shiftsCaret = false;
        StringBuffer buf = new StringBuffer();
        
        String endLine = currLineTxt.substring(txtStart);
        if (carPos >= (MAX-1)){
            newCaretOffset = getNewCaretOffset(d, c);
        } else {
            newCaretOffset = c.offset +1;
            buf.append(" ");
        }
        buf.append(ld);
        buf.append(tools.getIndentation(d,c));
        String nextLineTxt = tools.getStringAt(d,c,false,1);
        
        if ( nextLineTxt.trim().length() == 0 ||
                (nextLineLength +1+ endLine.trim().length()) > MAX  ||
                tools.isLineCommandLine(d,c,1) || 
                tools.isLineCommentLine(d,c,1) ||
                tools.isLineItemLine(d,c,1)) {
            // Create new line
            buf.append(endLine);
            c.length = endLine.length();
            
        } else {
            // Add the text to the next line
            nextLineTxt = tools.getStringAt(d,c,false,1);
            nextLineTxt = tools.trimBegin(nextLineTxt);
            buf.append(endLine.trim());
            buf.append(" ");
            buf.append(nextLineTxt);
            c.length = endLine.length()+nextLineLength;
        }
        c.caretOffset = newCaretOffset;
        c.text = buf.toString();
    }
    
    /**                 MAX  
     * >###CaaaaaaaaC#a|aaaa#aaaa
     *     ^~~~~~~~~^  |  
     */
    private void wrapCase2(IDocument d, DocumentCommand c, int txtStart, int lineEnd){
        int newCaretOffset;
        c.shiftsCaret = false;
        StringBuffer buf = new StringBuffer();
        String midLine = currLineTxt.substring(txtStart,lineEnd);
        String endLine = currLineTxt.substring(lineEnd+1);
        newCaretOffset = c.offset +1;
        buf.append(" ");
        buf.append(midLine);
        buf.append(ld);
        buf.append(tools.getIndentation(d,c));
        
        String nextLineTxt = tools.getStringAt(d,c,false,1);
        if ( nextLineTxt.trim().length() == 0 ||  
                (nextLineLength +1+ endLine.trim().length()) > MAX  ||
                tools.isLineCommandLine(d,c,1) || 
                tools.isLineCommentLine(d,c,1) ||
                tools.isLineItemLine(d,c,1)) {
            // Create new line
            buf.append(endLine);
            c.length = tools.getLineLength(d,c,true)-carPos;
            
        } else {
            // Add the text to the next line
            nextLineTxt = tools.getStringAt(d,c,false,1);
            nextLineTxt = tools.trimBegin(nextLineTxt);
            buf.append(endLine.trim());
            buf.append(" ");
            buf.append(nextLineTxt);
            c.length = tools.getLineLength(d,c,true)-carPos +nextLineLength;
        }
        c.caretOffset = newCaretOffset;
        c.text = buf.toString();
    }
    
    /**              MAX  
     * >###aaaaa#aaaaa|Caaa#aaaCaa
     *                 ^~~~~~~~^
     */
    private void wrapCase3(IDocument d, DocumentCommand c, int txtStart, int lineEnd){
        int newCaretOffset;
        c.shiftsCaret = false;
        StringBuffer buf = new StringBuffer();
        String line    = currLineTxt.substring(txtStart);
        String midLine = currLineTxt.substring(txtStart+1,lineEnd);
        String endLine = currLineTxt.substring(lineEnd);
        newCaretOffset = getNewCaretOffset(d,c);
        
        buf.append(ld);
        buf.append(tools.getIndentation(d,c));
        
        String nextLineTxt = tools.getStringAt(d,c,false,1);
        if ( nextLineTxt.trim().length() == 0 ||  
                (nextLineLength +1+ line.trim().length()) > MAX      ||
                tools.isLineCommandLine(d,c,1) || 
                tools.isLineCommentLine(d,c,1) ||
                tools.isLineItemLine(d,c,1)){
            // Create new line
            buf.append(midLine);
            buf.append(" ");
            buf.append(endLine);
            c.length = line.length();
            //System.out.println("No new line");
            
        } else {
            // Add the text to the next line
            nextLineTxt = tools.getStringAt(d,c,false,1);
            nextLineTxt = tools.trimBegin(nextLineTxt);
            buf.append(midLine);
            buf.append(" ");
            buf.append(endLine.trim());
            buf.append(" ");
            buf.append(nextLineTxt);
            c.length = line.length() +nextLineLength;			
        }
        
        try{
            c.offset = d.getLineOffset(d.getLineOfOffset(c.offset))+txtStart;
        }catch(BadLocationException e){
            TexlipsePlugin.log("HardLineWrap.wrapCase3", e);
        }
        c.caretOffset = newCaretOffset;
        c.text = buf.toString();
    }
    
    /**
     * The method return the offset of the caret after 
     * modification.
     * @param d
     * @param c
     * @return
     */
    private int getNewCaretOffset(IDocument d, DocumentCommand c) {
        return c.offset + ld.length() + tools.getIndentation(d,c).length();
    }
    
}
