/*
 * $Id
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.texparser;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

/**
 * This class has some static functions which are often needed when parsing
 * latex files
 * 
 * @author Boris von Loesch
 */
public class LatexParserUtils {
    
    // Indicate the anchor value "right"
    public final static int RIGHT = ICharacterPairMatcher.RIGHT;
    // Indicate the anchor value "left"
    public final static int LEFT = ICharacterPairMatcher.LEFT;

    
    /**
     * Checks whether the character at position <code>index</code> is escaped
     * by a backslash or not.
     * @param input
     * @param index
     * @return
     */
    public static boolean isEscaped (String input, int index) {
        while (index > 0) {
            index--;
            if (input.charAt(index) != '\\') return false;
            else if (index == 0 || input.charAt(index - 1) != '\\') return true;
            index--;
        }
        return false;
    }
    
    /**
     * Returns the index of the first character of the line
     * where <code>index</code> is located. Legal line delimeters are
     * \n, \r, \r\n. 
     * @param input
     * @param index
     * @return
     */
    public static int getStartofLine(String input, int index) {
        int pos = index;
        char c;
        c = input.charAt(pos);
        while (pos > 0 && c != '\r' && c != '\n') {
            c = input.charAt(--pos);
        }
        if (pos == 0) return pos;
        else return pos + 1;
    }
    
    /**
     * Checks whether position at <code>index</code> is inside a LaTeX comment.
     * @param input the text
     * @param index 
     * @return 
     * @throws BadLocationException if index is out of bounds
     */
    public static boolean isInsideComment(String input, int index){
        int lastLine = getStartofLine(input, index);
        int p = lastLine;
            while (p < index) {
                char c = input.charAt(p);
                if (c == '%') {
                    return true;
                } else if (c == '\\') {
                    //Ignore next character
                    p += 2;
                } else {
                    p++;
                }
            }
        return false;
    }

    /**
     * Tests if the command at the given index is a correct command.
     * 
     * @param input
     * @param commandLength The length of the command
     * @param index The index where the command occurs
     * @return
     * @throws BadLocationException if index is out of bounds
     */
    private static boolean testForCommand(String input, int commandLength, int index){

        if (isEscaped(input, index)) return false;
        // Check the character after the command
        if (index + commandLength == input.length() || 
                !Character.isLetter(input.charAt(index + commandLength))) {

            if (!isInsideComment(input, index)) return true;
        }
        return false;
    }

    /**
     * Returns the position of the first occurence of the command starting at
     * the specified index
     * 
     * @param input
     * @param command The Latex command starting with a backslash (\)
     * @param fromIndex The index from where to start the search
     * @return The position of the command, or -1 if the command is not
     *         contained in the String
     */
    public static int findCommand(String input, String command, int fromIndex) {
        int pos = input.indexOf(command, fromIndex);
        while (pos != -1) {
            if (testForCommand(input, command.length(), pos))
                return pos;
            pos = input.indexOf(command, pos + command.length());
        }
        return -1;
    }

    /**
     * Returns the position of the last occurence of the command backward
     * starting at the specified index
     * 
     * @param input
     * @param command The Latex command starting with a backslash (\)
     * @param fromIndex The index from which to backward start the search
     * @return The position of the command, or -1 if the command is not
     *         contained in the String
     */
    private static int findLastCommand(String input, String command, int fromIndex) {
        int pos = input.lastIndexOf(command, fromIndex);
        while (pos != -1) {
            if (testForCommand(input, command.length(), pos))
                return pos;
            pos = input.lastIndexOf(command, pos-1);
        }
        return -1;
    }


    /**
     * Finds the peercharacter for opening character (can be either "left" or
     * "right" character. The direction of the search is determined by the achor
     * (i.e. anchor==LEFT -> forward search and opening character is "left", or
     * anchor==RIGHT -> backward search and opening character is "right")
     * 
     * @param input
     * @param offset
     * @param anchor Must be either <code>LEFT</code> or <code>RIGHT</code>
     * @param opening
     * @param closing matching character for opening
     * @return index of the matching closing character, or -1 if the search
     *         failed
     */
    public static int findPeerChar(String input,
            int offset, int anchor, char opening, char closing) {
        int stack = 1, index;
        index = offset;
        while (stack > 0) {
            if (anchor == LEFT) {
                index++;
            } else {
                index--;
            }
            if ((index < 0) || (index >= input.length())) {
                index = -1;
                break;
            }
            char c = input.charAt(index);
            if (c == closing 
                    && (!isEscaped(input, index)) && (!isInsideComment(input, index)))
                stack--;
            else if (c == opening 
                    && (!isEscaped(input, index)) && (!isInsideComment(input, index)))
                stack++;
        }
        return index;
    }

    /**
     * Returns the first mandatory argument of the command
     * 
     * @param input
     * @param index The index at or after the beginning of the command and before the
     *            argument
     * @return The argument without braces, null if there is no valid argument
     * @throws BadLocationException if index is out of bounds
     */
    public static IRegion getCommandArgument(String input, int index){
        int pos = index;
        final int length = input.length();
        if (input.charAt(index) == '\\')
            pos++;
        while (pos < length && Character.isLetter(input.charAt(pos)))
            pos++;
        while (pos < length && Character.isWhitespace(input.charAt(pos)))
            pos++;
        if (pos == length) return null;
        if (input.charAt(pos) == '{') {
            int end = findPeerChar(input, pos + 1, LEFT, '{', '}');
            if (end == -1)
                return null;
            return new Region (pos + 1, end - pos - 1);
        }
        return null;
    }
    
    /**
     * Gets the command at the specified index. It returns the command if the index position is either
     * inside the command string or inside the first mandatory argument
     * @param input
     * @param index
     * @return null if it could not find any command
     * @throws BadLocationException
     */
    public static IRegion getCommand (String input, int index){
        if ("".equals(input)) return null;

        int pos = index;
        if (pos >= input.length()) {
            pos = input.length() - 1;
        }
        if (pos < 0) return null;
        
            if (isInsideComment(input, pos)) return null;
        boolean whiteSpace = false;
        if (pos > 0 && input.charAt(pos) == '}') pos--;
        char c = input.charAt(pos);
        while (!((pos <= 0 || c == '\\' || c == '{' || c == '}' || c == '%') 
                && (!isEscaped(input, pos)))) {
            if (Character.isWhitespace(c)) whiteSpace = true;
            pos--;
            c = input.charAt(pos);
        }
        if (c == '\\' && whiteSpace == false) {
            int l = 1;
            while (pos + l < input.length() && Character.isLetter(input.charAt(pos + l)))
                l++;
            //A command consist of a \ and at least one letter
            if (l == 1) return null;
            return new Region(pos, l);
        }
        if (c == '{') {
            if (pos == 0) return null;
            int l = -1;
            int ws = 0;
            c = input.charAt(pos + l);
            while (pos + l > 0 && Character.isWhitespace(c)) {
                ws--;
                l--;
                c = input.charAt(pos + l);
            }
            while (pos + l > 0 && Character.isLetter(c)) {
                l--;
                c = input.charAt(pos + l);
            }
            if (pos + l >= 0 && c == '\\' && (!isEscaped(input, pos+l))) { 
                return new Region(pos + l, -l+ws);
            }
        }
        return null; 
    }

    private static IRegion findEnvironment(String input, String envName, String command, int fromIndex) {
        int pos = input.indexOf("{" + envName + "}", fromIndex + command.length());
        while (pos != -1) {
            int end = pos + envName.length() + 2;
            // Search for the command
            int beginStart = findLastCommand(input, command, pos);
            if (beginStart != -1 && beginStart >= fromIndex) {
                // Check for whitespaces between \begin and {...}
                    while (pos != beginStart + command.length() && Character.isWhitespace(input.charAt(--pos)))
                        ;
                if (pos == beginStart + command.length()) {
                    return new Region(beginStart, end - beginStart);
                }
            }
            pos = input.indexOf("{" + envName + "}", pos + envName.length() + 2);
        }
        return null;
    }

    private static IRegion findLastEnvironment(String input, String envName, String command, int fromIndex) {
        int pos = input.lastIndexOf("{" + envName + "}", fromIndex);
        while (pos != -1) {
            int end = pos + envName.length() + 2;
            // Search for the command
            int beginStart = findLastCommand(input, command, pos);
            if (beginStart != -1 && beginStart <= fromIndex) {
                // Check for whitespaces between \command and {...}
                    while (pos != beginStart + command.length() && Character.isWhitespace(input.charAt(--pos)))
                        ;
                if (pos == beginStart + command.length()) {
                    return new Region(beginStart, end - beginStart);
                }
            }
            pos = input.lastIndexOf("{" + envName + "}", pos-1);
        }
        return null;
    }
    /**
     * Returns the region (offset & length) of \begin{envName}
     * 
     * @param input
     * @param envName name of the environment
     * @param fromIndex The index from which to start the search
     * @return
     */
    public static IRegion findBeginEnvironment(String input, String envName, int fromIndex) {
        return findEnvironment(input, envName, "\\begin", fromIndex);
    }

    /**
     * Returns the region (offset & length) of \end{envName}
     * 
     * @param input
     * @param envName name of the environment
     * @param fromIndex The index from which to start the search
     * @return
     */
    public static IRegion findEndEnvironment(String input, String envName, int fromIndex) {
        return findEnvironment(input, envName, "\\end", fromIndex);
    }


    /**
     * Finds for a \begin{env} the matching \end{env}.
     * @param input
     * @param envName       Name of the environment, e.g. "itemize"
     * @param beginIndex    Must be at the start or inside of \begin{env}
     * @return  The region of the \end{env} command or null if the end was not found
     */
    public static IRegion findMatchingEndEnvironment(String input, String envName, int beginIndex) {
        int pos = beginIndex + 1;
        IRegion nextEnd, nextBegin;
        int level = 0;
        
        do {
            nextEnd = findEndEnvironment(input, envName, pos);
            nextBegin = findBeginEnvironment(input, envName, pos);
            if (nextEnd == null) return null;
            if (nextBegin == null) {
                level--;
                pos = nextEnd.getOffset() + envName.length() + 6;
            } else {
                if (nextBegin.getOffset() > nextEnd.getOffset()) level--;
                else level++;
                pos = nextBegin.getOffset() + envName.length() + 8;
            }
        } while (level >= 0);
        return nextEnd;
    }
    
    /**
     * Finds for an \end{env} the matching \begin{env}.
     * @param input
     * @param envName       Name of the environment, e.g. "itemize"
     * @param beginIndex    Must be at the start of \end{env}
     * @return  The region of the \begin{env} command or null if the end was not found
     */
    public static IRegion findMatchingBeginEnvironment(String input, String envName, int beginIndex) {
        int pos = beginIndex;
        IRegion nextEnd, nextBegin;
        int level = 0;
        
        do {
            nextEnd = findLastEnvironment(input, envName, "\\end", pos);
            nextBegin = findLastEnvironment(input, envName, "\\begin", pos);
            if (nextBegin == null) return null;
            if (nextEnd == null) {
                level--;
                pos = nextBegin.getOffset();
            } else {
                if (nextEnd.getOffset() > nextBegin.getOffset()) level++;
                else level--;
                pos = nextEnd.getOffset();
            }
        } while (level >= 0);
        return nextBegin;
    }

}
