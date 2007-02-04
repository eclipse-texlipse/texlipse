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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;


/**
 * This class handles the line wrapping.
 * 
 * @author Antti Pirinen
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public class HardLineWrap {
    
    private TexEditorTools tools;
    
    public HardLineWrap(){
        this.tools = new TexEditorTools();
    }

/*    public void doWrapB(IDocument d, DocumentCommand c, int MAX_LENGTH) {
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
            int carretCorrection = 0;
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
                    carretCorrection += line.length() - cursorOnLine - line.substring(cursorOnLine).trim().length();
                    line = startLine + c.text + line.substring(cursorOnLine);
                }
            }
            else{
                line = line + c.text;
            }
                
            int trimCorrection = 0;
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
                String newText = line.substring(breakpos + 1).trim(); // TODO trim?
                buf.append(newText); 
                trimCorrection -= line.length() - breakpos - 1 - newText.trim().length();
                //buf.append(" ");
            }
            
            // If the cursor was at the end of the line, add the typed text
            carretCorrection += trimCorrection;
            if (!caretMidLine && (c.text.endsWith(" ") || c.text.endsWith("\t"))) {
                buf.append(c.text.charAt(c.text.length()-1));
                carretCorrection++;
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
                    carretCorrection++;
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
                c.caretOffset = c.offset + delim.length() + indent.length() + carretCorrection;
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
            c.length = buf.length() - delim.length() - indentLengthCorrection - trimCorrection;
            //c.caretOffset = newCaretOffset;
            c.text = buf.toString();
            
        } catch(BadLocationException e) {
            // FIXME
       }
    }*/
    
    /**
     * Removes all whitespaces from the beginning of the String
     * @param str The string to wrap
     * @return trimmed version of the string
     */
    private static String trimBegin (final String str) {
        int i = 0;
        //while (i < str.length() && (str.charAt(i) == ' ' || str.charAt(i) == '\t')) 
       while (i < str.length() && (Character.isWhitespace(str.charAt(i)))) 
            i++;
        return str.substring(i);
    }
    
    /**
     * Removes all whitespaces from the end of the String
     * @param str The string to wrap
     * @return trimmed version of the string
     */
    private static String trimEnd (final String str) {
        int i = str.length() - 1;
        //while (i >= 0 && (str.charAt(i) == ' ' || str.charAt(i) == '\t')) 
        while (i >= 0 && (Character.isWhitespace(str.charAt(i)))) 
            i--;
        return str.substring(0, i + 1);
    }

    /**
     * New line wrapping strategy.    
     * The actual wrapping method. Based on the <code>IDocument d</code>
     * and <code>DocumentCommand c</code> the method determines how the
     * line must be wrapped. 
     * <p>
     * If there is more than <code>MAX_LENGTH</code>
     * characters at the line, the method tries to detect the last white
     * space before <code> MAX_LENGTH</code>. In case there is none, the 
     * method finds the first white space after <code> MAX_LENGTH</code>.
     * Normally it adds the rest of the currentline to the next line. 
     * Exceptions are empty lines, commandlines, commentlines, and special lines like \\ or \[.
     * TODO Make the length of c as small as possible, because otherwise this can delete markers.
     * 
     * @param d             IDocument
     * @param c             DocumentCommand
     * @param MAX_LENGTH    How many characters are allowed at one line.
     */
    public void doWrapB(IDocument d, DocumentCommand c, int MAX_LENGTH) {
        try {
            // Get the line of the command excluding delimiter
            IRegion commandRegion = d.getLineInformationOfOffset(c.offset);
            
            // Ignore texts with line endings
            if (commandRegion.getLength() + c.text.length() <= MAX_LENGTH || 
                    c.text.indexOf("\n") >= 0 || c.text.indexOf("\r") >= 0) return;
            
            String line = d.get(commandRegion.getOffset(), commandRegion.getLength());
            int lineNr = d.getLineOfOffset(c.offset);
            String delim = d.getLineDelimiter(lineNr);
            //String indent = tools.getIndentation(d, c); // TODO check if inside comment
            String indent = tools.getIndentationWithComment(line);
            String nextline = tools.getStringAt(d, c, false, 1);
            final int cursorOnLine = c.offset - commandRegion.getOffset();
            
            //Create the newLine, we rewrite the whole currentline
            StringBuffer newLineBuf = new StringBuffer();
            int length = line.length();
            
            newLineBuf.append(line.substring(0, cursorOnLine));
            newLineBuf.append (c.text);
            newLineBuf.append(trimEnd(line.substring(cursorOnLine)));
            String nextTrimLine = nextline.trim(); 
            
            if (nextTrimLine.length() > 0
                    && !tools.isLineCommandLine(nextTrimLine)
                    && !tools.isLineCommentLine(nextTrimLine)
                    && !tools.isLineItemLine(nextTrimLine)
                    && !((nextTrimLine.startsWith("\\") && nextTrimLine.length() == 2)) // e.g. \\ or \[
                    && indent.indexOf('%') == -1) {
                //Add the whole next line
                newLineBuf.append(' ');
                newLineBuf.append(trimBegin(nextline));
                length += nextline.length();
            } else {
                newLineBuf.append(delim);
            }
            length += delim.length(); //delim.length();
            String newLine = newLineBuf.toString();

            c.length = length;
            
            int breakpos = tools.getLastWSPosition(newLine, MAX_LENGTH);
            c.shiftsCaret = false;
            c.caretOffset = c.offset + c.text.length() + indent.length();
            if (breakpos > cursorOnLine){ 
                c.caretOffset -= indent.length();
            }
            if (breakpos < cursorOnLine){
                //Line delimiter - one white space
                c.caretOffset += delim.length() - 1;
            }

            c.offset = commandRegion.getOffset();

            StringBuffer buf = new StringBuffer();
            buf.append(newLine.substring(0, breakpos));
            buf.append(delim);
            buf.append(indent);
            buf.append(trimBegin(newLine.substring(breakpos)));
            
            c.text = buf.toString();
            
        } catch(BadLocationException e) {
            // FIXME
       }
    }
    
 }
