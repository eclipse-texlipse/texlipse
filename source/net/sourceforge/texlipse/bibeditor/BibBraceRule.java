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
 * A rule for matching balanced curly-brace strings.
 * (eg. {foob{bar}})
 * 
 * @author Oskar Ojala
 */
public class BibBraceRule implements IPredicateRule {

    /** The token to be returned when this rule is successful */
    protected IToken fToken;
    
    protected boolean entry = false;

    /**
     * Creates a rule which will return the specified
     * token when either an entry or a brace string
     * is detected 
     *
     * @param e Whether to detect an entry (true) or just a string (false)
     * @param token The token to be returned
     */
    public BibBraceRule(boolean e, IToken token) {
        Assert.isNotNull(token);
        fToken = token;
        entry = e;
    }

    /**
     * Does the actual evaluation of the stream.
     * 
     * @param scanner The scanner
     * @param count The initial count of {
     * @return <code>fToken</code> on success, <code>Token.UNDEFINED</code> if
     * the match doesn't succeed
     */
    private IToken doEvaluate(ICharacterScanner scanner, int count) {
        boolean inString = false;
        int c = scanner.read();

        if (((char) c) == '{') {
            do {
                c = scanner.read();
                if (((char) c) == '{' && !inString)
                    count++;
                else if (((char) c) == '}' && !inString)
                    count--;
                else if (((char) c) == '"' && !inString)
                    inString = true;
                else if (((char) c) == '"' && inString)
                    inString = false;
                else if (c == ICharacterScanner.EOF)
                	return Token.UNDEFINED;
            } while (count > 0);
            return fToken;
        }
        scanner.unread();
        return Token.UNDEFINED;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
     */
    public IToken evaluate(ICharacterScanner scanner) {
        return doEvaluate(scanner, 1);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner, boolean)
     */
    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
        if (resume) {
            boolean inString = false;
            do {
                int c = scanner.read();
                if (((char) c) == ',' && !inString)
                    break;
                else if (((char) c) == '@') {
                    scanner.unread();
                    return Token.UNDEFINED;
                } else if (((char) c) == '"' && !inString)
                    inString = true;
                else if (((char) c) == '"' && inString)
                    inString = false;
                else if (c == ICharacterScanner.EOF)
                    return Token.UNDEFINED;
            } while (true);
        }
        return doEvaluate(scanner, 1);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.rules.IPredicateRule#getSuccessToken()
     */
    public IToken getSuccessToken() {
        return fToken;
    }
}
