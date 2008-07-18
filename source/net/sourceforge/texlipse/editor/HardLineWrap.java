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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * @author Boris von Loesch
 */
public class HardLineWrap {
    
    private TexEditorTools tools;
    private static final Pattern simpleCommandPattern =
        Pattern.compile("\\\\(\\w+|\\\\)\\s*(\\[.*?\\]\\s*)*(\\{.*?\\}\\s*)*");

    public HardLineWrap(){
        this.tools = new TexEditorTools();
    }

    
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
     * This method checks, whether <i>line</i> should stay alone on one line.<br />
     * Examples:
     * <ul>
     * <li>\begin{env}</li>
     * <li>% Comments</li>
     * <li>\command[...]{...}{...}</li>
     * <li>(empty line)</li>
     * <li>\\[2em]</li>
     * </ul>
     * 
     * @param line
     * @return
     */
    private static boolean isSingleLine(String line) {
        if (line.length() == 0) return true;
        if (line.startsWith("%")) return true;
        if ((line.startsWith("\\") && line.length() == 2)) return true; // e.g. \\ or \[
        if (line.startsWith("\\item")) return true;
        Matcher m = simpleCommandPattern.matcher(line);
        if (m.matches()) return true;
        return false;
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

            //Special case if there are white spaces at the end of the line
            if (trimEnd(line).length() + c.text.length() <= MAX_LENGTH) return; 

            
            if (line.indexOf(' ') == -1) {
                //There is no whitespace
                return;
            }

            int lineNr = d.getLineOfOffset(c.offset);
            String delim = d.getLineDelimiter(lineNr);
            boolean isLastLine = false;
            if (delim == null) {
                //This is the last line in the document
                isLastLine = true;
                if (lineNr > 0) delim = d.getLineDelimiter(lineNr - 1);
                else {
                    //Last chance
                    String delims[] = d.getLegalLineDelimiters();
                    delim = delims[0];
                }
            }
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
            boolean isWithNextline = false;
            if (!isSingleLine(nextTrimLine) && indent.indexOf('%') == -1) {
                //Add the whole next line
                newLineBuf.append(' ');
                newLineBuf.append(trimBegin(nextline));
                length += nextline.length();
                isWithNextline = true;
            } else {
                newLineBuf.append(delim);
            }
            if (!isLastLine) length += delim.length(); //delim.length();
            String newLine = newLineBuf.toString();

            c.length = length;
            
            int breakpos = tools.getLastWSPosition(newLine, MAX_LENGTH);
            if (breakpos == -1) {
                breakpos = tools.getFirstWSPosition(newLine, MAX_LENGTH);
            }
            c.shiftsCaret = false;
            c.caretOffset = c.offset + c.text.length() + indent.length();
            if (breakpos > cursorOnLine + c.text.length()){ 
                c.caretOffset -= indent.length();
            }
            if (breakpos <= cursorOnLine + c.text.length()){
                //Line delimiter - one white space
                c.caretOffset += delim.length() - 1;
            }

            c.offset = commandRegion.getOffset();

            StringBuffer buf = new StringBuffer();
            buf.append(newLine.substring(0, breakpos));
            buf.append(delim);
            buf.append(indent);
            buf.append(trimBegin(newLine.substring(breakpos)));
            
            //Remove unnecessary characters from buf
            int i=0;
            while (i < line.length() && line.charAt(i) == buf.charAt(i)) {
                i++;
            }
            buf.delete(0, i);
            c.offset += i;
            c.length -= i;
            if (isWithNextline) {
                i=0;
                while (i < nextline.length() && 
                        nextline.charAt(nextline.length()-i-1) == buf.charAt(buf.length()-i-1)) {
                    i++;
                }
                buf.delete(buf.length()-i, buf.length());
                c.length -= i;
            }
            
            c.text = buf.toString();
            
        } catch(BadLocationException e) {
            TexlipsePlugin.log("Problem with hard line wrap", e);
       }
    }
    
 }
