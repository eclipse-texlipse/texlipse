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

package org.eclipse.texlipse.texparser;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;

import org.eclipse.texlipse.texparser.lexer.LexerException;
import org.eclipse.texlipse.texparser.node.EOF;
import org.eclipse.texlipse.texparser.node.TArgument;
import org.eclipse.texlipse.texparser.node.TCchapter;
import org.eclipse.texlipse.texparser.node.TCcite;
import org.eclipse.texlipse.texparser.node.TCommentline;
import org.eclipse.texlipse.texparser.node.TCparagraph;
import org.eclipse.texlipse.texparser.node.TCpart;
import org.eclipse.texlipse.texparser.node.TCsection;
import org.eclipse.texlipse.texparser.node.TCssection;
import org.eclipse.texlipse.texparser.node.TCsssection;
import org.eclipse.texlipse.texparser.node.TOptargument;
import org.eclipse.texlipse.texparser.node.TStar;
import org.eclipse.texlipse.texparser.node.TWhitespace;
import org.eclipse.texlipse.texparser.node.TWord;
import org.eclipse.texlipse.texparser.node.Token;


/**
 * A LaTeX word counting parser. Counts the likely printed words from
 * the given string, ie. all normal words and the contents of sectioning 
 * commands are counted. Cite-references are counted as one word.
 * 
 * @author Oskar Ojala
 */
public class LatexWordCounter {

    private String selection;

    /**
     * Creates new word counter with a string with words to count.
     * 
     * @param selection The string to use for counting words
     */
    public LatexWordCounter(String selection) {
        this.selection = selection;
    }

    /**
     * Counts the number of (LaTeX) words in the string that this
     * object contains.
     * 
     * @return The number of words or -1 on an error
     */
    public int countWords() {
        try {
            LatexLexer lexer = new LatexLexer(new PushbackReader(new StringReader(selection), 4096));

            int words = 0;
            boolean expectArg = false;
            for (Token t = lexer.next(); !(t instanceof EOF); t = lexer.next()) {
                if (expectArg) {
                    if (t instanceof TArgument) {
                        words += t.getText().split("\\s+").length;
                        expectArg = false;
                    } else if (!(t instanceof TOptargument) && !(t instanceof TWhitespace)
                            && !(t instanceof TStar) && !(t instanceof TCommentline)) {
                        // this is an error state, but we'll skip it
                        expectArg = false;
                    }
                } else {
                    if (t instanceof TWord || t instanceof TCcite) {
                        if (!"&".equals(t.getText()))
                            words++;
                    } else if (t instanceof TWhitespace) { // make the common case fast
                        continue;
                    } else if (t instanceof TCpart || t instanceof TCchapter 
                            || t instanceof TCsection || t instanceof TCssection 
                            || t instanceof TCsssection || t instanceof TCparagraph)
                        expectArg = true;
                }
            }
            return words;
        } catch (IOException e) {
            return -1;
        } catch (LexerException e) {
            return -1;
        }
    }
}
