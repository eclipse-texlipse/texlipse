/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

/**
 * A BibTeX code scanner for performing syntax highlighting. Scans the area
 * outside entries.
 * 
 * @author Oskar Ojala
 */
public class BibCodeScanner extends RuleBasedScanner {

    /**
     * Class for detecting whitespace.
     * 
     * @author Oskar Ojala
     */
    private class WhitespaceDetector implements IWhitespaceDetector {
        public boolean isWhitespace(char character) {
            return Character.isWhitespace(character);
        }
    }

    /**
     * class for detecting BibTeX words.
     * 
     * @author Oskar Ojala
     */
    private class BibWordDetector implements IWordDetector {

        public boolean isWordPart(char character) {
            return Character.isJavaIdentifierPart(character);
        }

        public boolean isWordStart(char character) {
            return Character.isJavaIdentifierStart(character);
        }
    }

    /**
     * Creates a BibTeX document scanner
     * 
     * @param provider The color provider for syntax highlighting
     */
    public BibCodeScanner(BibColorProvider provider) {

        IToken keyword = new Token(new TextAttribute(provider
                .getColor(BibColorProvider.KEYWORD)));
        IToken comment = new Token(new TextAttribute(provider
                .getColor(BibColorProvider.SINGLE_LINE_COMMENT)));
        IToken other = new Token(new TextAttribute(provider
                .getColor(BibColorProvider.DEFAULT)));

        List rules = new ArrayList();

        // Add rule for single line comments.
        rules.add(new EndOfLineRule("%", comment));

        rules.add(new BibCommandRule(keyword));

        // Add generic whitespace rule.
        rules.add(new WhitespaceRule(new WhitespaceDetector()));

        // Add word rule for keywords, types, and constants.
        WordRule wordRule = new WordRule(new BibWordDetector(), other);
        rules.add(wordRule);

        IRule[] result = new IRule[rules.size()];
        rules.toArray(result);
        setRules(result);
    }
}