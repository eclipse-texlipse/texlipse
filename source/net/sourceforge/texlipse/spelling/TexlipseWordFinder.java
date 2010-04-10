/*
 * $Id$
 * 
 * Copyright (c) 2010 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.spelling;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.swabunga.spell.event.AbstractWordFinder;
import com.swabunga.spell.event.Word;
import com.swabunga.spell.event.WordNotFoundException;

/**
 * Finds the non TeX words in a given text. Ignores arguments 
 * of some special commands like \ref, \label, \begin,...
 * 
 * @author Boris von Loesch
 *
 */
public class TexlipseWordFinder extends AbstractWordFinder {

    private final static Pattern MAND_ARG = Pattern.compile("\\A\\s*\\{[^\\}]+\\}");
    private final static Pattern OPT_MAND_ARG = Pattern.compile("\\A\\s*(\\[[^\\]]+\\])?\\s*\\{[^\\}]+\\}");
    
    private boolean IGNORE_COMMENTS = true;
    private boolean IGNORE_MATH = true;
    

    public TexlipseWordFinder(String st) {
        super(st);
    }
    
    public TexlipseWordFinder() {
        super();
    }

    /**
     * This method scans the text from the end of the last word, and returns a
     * new Word object corresponding to the next word.
     *
     * @return the next word.
     * @throws WordNotFoundException search string contains no more words.
     */
    @Override
    public Word next() {

        if (!hasNext())
            throw new WordNotFoundException("No more words found.");

        currentWord.copy(nextWord);
        setSentenceIterator(currentWord);


        int i = currentWord.getEnd();
        boolean finished = false;
        boolean started = false;

        while (i < text.length() && !finished) {

            if (!started && isWordChar(i)) {
                nextWord.setStart(i++);
                started = true;
                continue;
            } else if (started) {
                if (isWordChar(i)) {
                    i++;
                    continue;
                } else {
                    nextWord.setText(text.substring(nextWord.getStart(), i));
                    finished = true;
                    break;
                }
            } 

            // Ignores should be in order of importance and then specificity.
            int j = i;
            if (IGNORE_COMMENTS) j = ignore(j, '%', '\n');

            if (IGNORE_MATH) {
                //FIXME: Is not working correctly when parsing just a single line
                //j = ignore(j, '$', '$');
            }

            if (j < text.length() && text.charAt(j) == '\\') { 
                // Ignore certain command parameters.
                j = ignore(j, "\\documentclass", OPT_MAND_ARG);
                j = ignore(j, "\\usepackage", OPT_MAND_ARG);
                j = ignore(j, "\\newcounter", MAND_ARG);
                j = ignore(j, "\\setcounter", MAND_ARG);
                j = ignore(j, "\\addtocounter", MAND_ARG);
                j = ignore(j, "\\value", MAND_ARG);
                j = ignore(j, "\\arabic", MAND_ARG);
                j = ignore(j, "\\stepcounter", MAND_ARG);
                j = ignore(j, "\\newenvironment", MAND_ARG);
                j = ignore(j, "\\renewenvironment", MAND_ARG);
                j = ignore(j, "\\ref", MAND_ARG);
                j = ignore(j, "\\vref", MAND_ARG);
                j = ignore(j, "\\eqref", MAND_ARG);
                j = ignore(j, "\\pageref", MAND_ARG);
                j = ignore(j, "\\label", MAND_ARG);
                j = ignore(j, "\\cite", OPT_MAND_ARG);
                j = ignore(j, "\\tag", MAND_ARG);

                // Ignore environment names.
                j = ignore(j, "\\begin", MAND_ARG);
                j = ignore(j, "\\end", MAND_ARG);        

                // Ignore commands.
                j = ignore(j, '\\');
            }
            
            if (i != j){
                i = j;
                continue;
            }
            i++;
        }

        if (!started) {
            nextWord = null;
        } else if (!finished) {
            nextWord.setText(text.substring(nextWord.getStart(), i));
        }

        return currentWord;
    }

    /**
     * Define if comments contents are ignored during spell checking
     * @param ignore an indication if comments content is to be ignored
     */
    public void setIgnoreComments(boolean ignore) {
        IGNORE_COMMENTS = ignore;
    }

    public void setIgnoreMath(boolean ignore) {
        IGNORE_MATH = ignore;
    }
    
    /**
     * Ignores a command string
     * @param index 
     * @param command The command with leading backslash
     * @param p Regexp pattern for the command arguments
     * @return new index
     */
    public int ignore(int index, String command, Pattern p) {
        int i = 0;
        //Is this the right command
        while (i < command.length()) {
            if (index + i >= text.length()) return index;
            if (command.charAt(i) != text.charAt(i + index)) return index;
            i++;
        }
        i = i + index;
        Matcher m = p.matcher(text.substring(i));
        if (m.find()) 
            return i + m.end() - 1;
        return index;
    }
}
