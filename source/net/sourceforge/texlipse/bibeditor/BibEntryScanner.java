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

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;

/**
 * Syntax highlighting scanner for BibTeX-entries. This scanner
 * is used to scan the syntax inside entries.
 * 
 * @author Oskar Ojala
 */
public class BibEntryScanner extends RuleBasedScanner {

    /**
     * Detects white space.
     * 
     * @author Oskar Ojala
     */
    private class WhitespaceDetector implements IWhitespaceDetector {
        /* (non-Javadoc)
         * @see org.eclipse.jface.text.rules.IWhitespaceDetector#isWhitespace(char)
         */
        public boolean isWhitespace(char character) {
            return Character.isWhitespace(character);
        }
    }

    /**
     * Detects BibTeX keywords (ie. [a-ZA-Z]\w*).
     * 
     * @author Oskar Ojala
     */
    public class BibWordDetector implements IWordDetector {

        /* (non-Javadoc)
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
         */
        public boolean isWordPart(char character) {
            return Character.isLetterOrDigit(character);
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
         */
        public boolean isWordStart(char character) {
            return Character.isLetter(character);
        }
    }

    /**
     * Detects BibTeX separator chars (such as = and ,).
     * 
     * @author Oskar Ojala
     */
    public class BibSeparatorRule implements IRule {

        /** The token to be returned when this rule is successful */
        protected IToken fToken;

        /**
         * Creates a rule which will return the specified
         * token when a BibTeX-entry special character is encountered
         *
         * @param token the token to be returned
         */
        public BibSeparatorRule(IToken token) {
            Assert.isNotNull(token);
            fToken = token;
        }
        
        /* (non-Javadoc)
         * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
         */
        public IToken evaluate(ICharacterScanner scanner) {
            int c = scanner.read();
            if (((char) c) == '=' || ((char) c) == '#' || ((char) c) == ','
                || ((char) c) == '{' || ((char) c) == '}') {
                return fToken;
            } else if (((char) c) == '\\') {
                c = scanner.read();
                if (((char) c) == '"')
                    return fToken;
                scanner.unread();
            }
            scanner.unread();
            return Token.UNDEFINED;
        }
    }

    /**
     * Creates a BibTeX entry scanner
     */
    public BibEntryScanner(BibColorProvider provider) {

        IToken keyword = new Token(new TextAttribute(provider
                .getColor(BibColorProvider.KEYWORD)));
        IToken type = new Token(new TextAttribute(provider
                .getColor(BibColorProvider.TYPE)));
        IToken string = new Token(new TextAttribute(provider
                .getColor(BibColorProvider.STRING)));
        IToken comment = new Token(new TextAttribute(provider
                .getColor(BibColorProvider.SINGLE_LINE_COMMENT)));
        IToken other = new Token(new TextAttribute(provider
                .getColor(BibColorProvider.DEFAULT)));

        List rules = new ArrayList();

        // Add rule for single line comments.
        // Not supported inside entries.
//        rules.add(new EndOfLineRule("%", comment));

        rules.add(new BibSeparatorRule(keyword));

        // Add rule for strings and character constants.
        // Note that escaping is not possible in BibTeX.
//        rules.add(new MultiLineRule("\"", "\"", string));
//        rules.add(new MultiLineRule("\"", "\"", string, (char) 0, true));
        rules.add(new BibStringRule(string));

        // this must be preceded by # or =
//        rules.add(new BibBraceRule(false, string));

        // Add generic whitespace rule.
        rules.add(new WhitespaceRule(new WhitespaceDetector()));

        // Add word rule for keywords, types, and constants.
        WordRule wordRule = new WordRule(new BibWordDetector(), type);
//        wordRule.addWord("author", keyword);
        rules.add(wordRule);

        rules.add(new NumberRule(string));
        
        IRule[] result= new IRule[rules.size()];
        rules.toArray(result);
        setRules(result);
    }
}
