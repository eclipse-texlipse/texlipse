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

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * BibTeX string rule for "..." -style strings. 
 * Takes into account that { and } must be matched
 * inside a string.
 * 
 * @author Oskar Ojala
 */
public class BibStringRule implements IPredicateRule {

    /** The token to be returned when this rule is successful */
    protected IToken fToken;
    
    /**
     * Creates a new string rule matcher.
     *  
     * @param token The token to return on a successful match
     */
    public BibStringRule(IToken token) {
        Assert.isNotNull(token);
        fToken = token;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.rules.IPredicateRule#getSuccessToken()
     */
    public IToken getSuccessToken() {
        return fToken;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner, boolean)
     */
    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
        return evaluate(scanner);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
     */
    public IToken evaluate(ICharacterScanner scanner) {
        int count = 0;
        int charsRead = 0;
        int c = scanner.read();
        if (((char) c) == '"') {
                do {
                    c = scanner.read();
                    charsRead++;
                    if (c == ICharacterScanner.EOF) {
                        unwind(scanner, charsRead);
                        return Token.UNDEFINED;
                    } else if (((char) c) == '{') {
                        count++;
                    } else if (((char) c) == '}') {
                        if (count == 0) {
                            unwind(scanner, charsRead);
                            return Token.UNDEFINED;
                        }
                        count--;
                    }
                } while (((char) c) != '"');
                return fToken;
        }
        scanner.unread();
        return Token.UNDEFINED;
    }
    
    /**
     * Unreads <code>numChars</code> characters from <code>scanner</code>.
     * 
     * @param scanner The scanner
     * @param numChars The number of characters to unread
     */
    private void unwind(ICharacterScanner scanner, int numChars) {
        for (int i = 0; i < numChars; i++)
            scanner.unread();
    }
}
