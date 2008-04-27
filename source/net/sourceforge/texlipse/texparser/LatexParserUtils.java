/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.texparser;

import java.util.regex.Pattern;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
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

    Pattern ENVIRONMENT_PATTERN = Pattern.compile("^((?:[^%\\n\\r]*[^\\\\%])|(?:))\\\\((?:begin)|(?:end))\\s*\\{([^\\}]+)\\}");
    
    /**
     * This is a short interface which implements all methods
     * which are needed for our textsearch
     * 
     * @author Boris von Loesch
     */
    private interface ILatexText {
        int length();
        char charAt (int offset) throws BadLocationException;
        int indexOf (String search, int fromIndex);
        int lastIndexOf (String search, int fromIndex);
        int indexOf (char search, int fromIndex);
        int lastIndexOf (char search, int fromIndex);
    }
    
    /**
     * Wrapper class for IDocument
     *
     */
    private static class DocumentLatexText implements ILatexText{
        IDocument fDocument;
        FindReplaceDocumentAdapter fFindReplace;
        
        DocumentLatexText(IDocument document){
            fDocument = document;
        }
        
        public int length() {
            return fDocument.getLength();
        }
        
        public char charAt(int arg0) throws BadLocationException{
            return fDocument.getChar(arg0);
        }
        
        public int indexOf(String arg0, int arg1) {
            if (fFindReplace == null) 
                fFindReplace = new FindReplaceDocumentAdapter(fDocument);
            IRegion region;
            try {
                region = fFindReplace.find(arg1, arg0, true, true, false, false);
            } catch (BadLocationException e) {
                return -1;
            }
            if (region == null || region.getOffset() < arg1)
                return -1;
            else
                return region.getOffset();
        }

        public int lastIndexOf(String arg0, int arg1){
            if (fFindReplace == null) 
                fFindReplace = new FindReplaceDocumentAdapter(fDocument);
            IRegion region;
            try {
                region = fFindReplace.find(arg1, arg0, false, true, false, false);
            } catch (BadLocationException e) {
                return -1;
            }
            if (region == null || region.getOffset() > arg1)
                return -1;
            else
                return region.getOffset();
        }
        
        public int indexOf (char arg0, int arg1) {
            int index = arg1;
            if (index < 0 || index >= fDocument.getLength()) {
                return -1;
            }
            try {
                do {
                    if (fDocument.getChar(index) == arg0)
                        return index;
                } while (++index < fDocument.getLength());
            } catch (BadLocationException e) {
                // Should not happen
                TexlipsePlugin.log("Error", e);
            }
            return -1;
        }

        public int lastIndexOf (char arg0, int arg1) {
            int index = arg1;
            if (index < 0 || index >= fDocument.getLength()) {
                return -1;
            }
            try {
                do {
                    if (fDocument.getChar(index) == arg0)
                        return index;
                } while (--index >= 0);
            } catch (BadLocationException e) {
                // Should not happen
                TexlipsePlugin.log("Error", e);
            }
            return -1;
        }
    }
    
    /**
     * Wrapper class for String
     *
     */
    private static class StringLatexText implements ILatexText{
        String fString;
        
        StringLatexText(String string) {
            fString = string;
        }
        
        public int length() {
            return fString.length();
        }
        
        public char charAt(int arg0) throws BadLocationException {
            try {
                return fString.charAt(arg0);
            } catch (IndexOutOfBoundsException e) {
                throw new BadLocationException(e.getMessage());
            }
        }
        
        public int indexOf(String arg0, int arg1) {
            return fString.indexOf(arg0, arg1);
        } 

        public int lastIndexOf(String arg0, int arg1) {
            return fString.lastIndexOf(arg0, arg1);
        } 

        public int indexOf(char arg0, int arg1) {
            return fString.indexOf(arg0, arg1);
        } 

        public int lastIndexOf(char arg0, int arg1) {
            return fString.lastIndexOf(arg0, arg1);
        } 
    }
    
    // Indicate the anchor value "right"
    public final static int RIGHT = ICharacterPairMatcher.RIGHT;
    // Indicate the anchor value "left"
    public final static int LEFT = ICharacterPairMatcher.LEFT;

    
    /**
     * Returns the index of the first character of the line
     * where <code>index</code> is located. Legal line delimeters are
     * \n, \r, \r\n. 
     * @param input
     * @param index
     * @return
     */
    private static int getStartofLine(ILatexText input, int index) {
        int lastCR = input.lastIndexOf('\r', index);
        int lastLine = input.lastIndexOf('\n', index);
        if (lastCR > lastLine) {
            //Mac
            lastLine = lastCR;
        }
        return lastLine + 1;
    }
    
    public static int getStartofLine(String input, int index) {
        return getStartofLine(new StringLatexText(input), index);
    }
    
    /**
     * Checks whether index is inside a comment
     * @param input
     * @param index
     * @return 
     * @throws BadLocationException if index is out of bounds
     */
    private static boolean isInsideComment(ILatexText input, int index) throws BadLocationException{
        int lastLine = getStartofLine(input, index);
        int p = lastLine;
            while (p < index) {
                if (input.charAt(p) == '%') {
                    return true;
                } else if (input.charAt(p) == '\\') {
                    //Ignore next character
                    p += 2;
                } else {
                    p++;
                }
            }
        return false;
    }

    public static boolean isInsideComment(String input, int index) throws BadLocationException {
        return isInsideComment(new StringLatexText(input), index);
    }
    
    public static boolean isInsideComment(IDocument input, int index) throws BadLocationException {
        return isInsideComment(new DocumentLatexText(input), index);
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
    private static boolean testForCommand(ILatexText input, int commandLength, int index) 
        throws BadLocationException{

        // Check for a backslash before the command
        if (index == 0 || input.charAt(index - 1) != '\\') {
            // Check the character after the command
            if (index + commandLength == input.length() || 
                    !Character.isLetter(input.charAt(index + commandLength))) {
                
                if (!isInsideComment(input, index)) return true;
            }
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
    private static int findCommand(ILatexText input, String command, int fromIndex) {
        int pos = input.indexOf(command, fromIndex);
        while (pos != -1) {
            try {
                if (testForCommand(input, command.length(), pos))
                    return pos;
            } catch (BadLocationException e) {
                //never happens
                TexlipsePlugin.log("Error", e);
            }
            pos = input.indexOf(command, pos + command.length());
        }
        return -1;
    }

    public static int findCommand(String input, String command, int fromIndex) {
        return findCommand (new StringLatexText(input), command, fromIndex);
    }

    public static int findCommand(IDocument input, String command, int fromIndex) {
        return findCommand (new DocumentLatexText(input), command, fromIndex);
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
    private static int findLastCommand(ILatexText input, String command, int fromIndex) {
        int pos = input.lastIndexOf(command, fromIndex);
        while (pos != -1) {
            try {
                if (testForCommand(input, command.length(), pos))
                    return pos;
            } catch (BadLocationException e) {
                //never happens
                TexlipsePlugin.log("Error", e);                
            }
            pos = input.lastIndexOf(command, pos-1);
        }
        return -1;
    }


    public static int findLastCommand(String input, String command, int fromIndex) {
        return findCommand (new StringLatexText(input), command, fromIndex);
    }

    public static int findLastCommand(IDocument input, String command, int fromIndex) {
        return findCommand (new DocumentLatexText(input), command, fromIndex);
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
    private static int findPeerChar(ILatexText input,
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
            try{
                if (input.charAt(index) == closing 
                        && (index == 0 || input.charAt(index - 1) != '\\') && (!isInsideComment(input, index)))
                    stack--;
                else if (input.charAt(index) == opening 
                        && (index == 0 || input.charAt(index - 1) != '\\') && (!isInsideComment(input, index)))
                    stack++;
            } catch (BadLocationException e) {
                //never happens
                TexlipsePlugin.log("Error", e);
            }
        }
        return index;
    }

    public static int findPeerChar(String input, int offset, int anchor, char opening, char closing) {
        return findPeerChar(new StringLatexText(input), offset, anchor, opening, closing);
    }

    public static int findPeerChar(IDocument input, int offset, int anchor, char opening, char closing) {
        return findPeerChar(new DocumentLatexText(input), offset, anchor, opening, closing);
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
    private static IRegion getCommandArgument(ILatexText input, int index) throws BadLocationException{
        int pos = index;
        if (input.charAt(index) == '\\')
            pos++;
        while (pos < input.length() && Character.isLetter(input.charAt(pos)))
            pos++;
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos)))
            pos++;
        if (pos == input.length()) return null;
        if (input.charAt(pos) == '{') {
            int end = findPeerChar(input, pos + 1, LEFT, '{', '}');
            if (end == -1)
                return null;
            return new Region (pos + 1, end - pos - 1);
        }
        return null;
    }

    public static IRegion getCommandArgument(String input, int index) throws BadLocationException {
        return getCommandArgument(new StringLatexText(input), index);
    }
    
    public static IRegion getCommandArgument(IDocument input, int index) throws BadLocationException {
        return getCommandArgument(new DocumentLatexText(input), index);
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
        int pos = index;
        if ("".equals(input)) return null;
        while (pos >= input.length()) pos--;
        try {
            if (isInsideComment(input, pos)) return null;
        } catch (BadLocationException ex) {
            //Does not happen
            TexlipsePlugin.log("Error", ex);
        }
        boolean whiteSpace = false;
        if (pos > 0 && input.charAt(pos) == '}') pos--;
        while (!((pos <= 0 || input.charAt(pos) == '\\' || input.charAt(pos) == '{' || input.charAt(pos) == '}' || input.charAt(pos) == '%') 
                && (pos == 0 || input.charAt(pos-1) != '\\'))) {
            if (Character.isWhitespace(input.charAt(pos))) whiteSpace = true;
            pos--;
        }
        if (pos < 0) return null;
        if (input.charAt(pos) == '\\' && whiteSpace == false) {
            int l = 1;
            while (pos + l < input.length() && Character.isLetter(input.charAt(pos + l)))
                l++;
            //A command consist of a \ and at least one letter
            if (l == 1) return null;
            return new Region(pos, l);
        }
        if (input.charAt(pos) == '{') {
            int l = -1;
            int ws = 0;
            while (pos + l >= 0 && Character.isWhitespace(input.charAt(pos + l))) {
                ws--;
                l--;
            }
            while (pos + l >= 0 && Character.isLetter(input.charAt(pos + l)))
                l--;
            if (pos + l >= 0 && input.charAt(pos + l) == '\\') return new Region(pos + l, -l+ws);
        }
        return null; 
    }

    private static IRegion findEnvironment(ILatexText input, String envName, String command, int fromIndex) {
        int pos = input.indexOf("{" + envName + "}", fromIndex + command.length());
        while (pos != -1) {
            int end = pos + envName.length() + 2;
            // Search for the command
            int beginStart = findLastCommand(input, command, pos);
            if (beginStart != -1 && beginStart >= fromIndex) {
                // Check for whitespaces between \begin and {...}
                try {
                    while (pos != beginStart + command.length() && Character.isWhitespace(input.charAt(--pos)))
                        ;
                } catch (BadLocationException e) {
                    //never happens
                    TexlipsePlugin.log("Error", e);
                }
                if (pos == beginStart + command.length()) {
                    return new Region(beginStart, end - beginStart);
                }
            }
            pos = input.indexOf("{" + envName + "}", pos + envName.length() + 2);
        }
        return null;
    }

    private static IRegion findLastEnvironment(ILatexText input, String envName, String command, int fromIndex) {
        int pos = input.lastIndexOf("{" + envName + "}", fromIndex);
        while (pos != -1) {
            int end = pos + envName.length() + 2;
            // Search for the command
            int beginStart = findLastCommand(input, command, pos);
            if (beginStart != -1 && beginStart <= fromIndex) {
                // Check for whitespaces between \command and {...}
                try {
                    while (pos != beginStart + command.length() && Character.isWhitespace(input.charAt(--pos)))
                        ;
                } catch (BadLocationException e) {
                    //never happens
                    TexlipsePlugin.log("Error", e);
                }
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
    private static IRegion findBeginEnvironment(ILatexText input, String envName, int fromIndex) {
        return findEnvironment(input, envName, "\\begin", fromIndex);
    }

    public static IRegion findBeginEnvironment(String input, String envName, int fromIndex) {
        return findBeginEnvironment(new StringLatexText(input), envName, fromIndex);
    }

    public static IRegion findBeginEnvironment(IDocument input, String envName, int fromIndex) {
        return findBeginEnvironment(new DocumentLatexText(input), envName, fromIndex);
    }
    /**
     * Returns the region (offset & length) of \end{envName}
     * 
     * @param input
     * @param envName name of the environment
     * @param fromIndex The index from which to start the search
     * @return
     */
    private static IRegion findEndEnvironment(ILatexText input, String envName, int fromIndex) {
        return findEnvironment(input, envName, "\\end", fromIndex);
    }


    public static IRegion findEndEnvironment(String input, String envName, int fromIndex) {
        return findEndEnvironment(new StringLatexText(input), envName, fromIndex);
    }

    public static IRegion findEndEnvironment(IDocument input, String envName, int fromIndex) {
        return findEndEnvironment(new DocumentLatexText(input), envName, fromIndex);
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
            nextEnd = findLastEnvironment(new StringLatexText(input), envName, "\\end", pos);
            nextBegin = findLastEnvironment(new StringLatexText(input), envName, "\\begin", pos);
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
