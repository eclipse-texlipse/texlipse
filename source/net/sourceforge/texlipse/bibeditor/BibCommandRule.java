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
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * A rule for matching BibTeX -commands, '@' followed my letters.
 * 
 * @author Oskar Ojala
 */
public class BibCommandRule implements IRule {

    /** The token to be returned when this rule is successful */
    protected IToken fToken;

    /**
     * Creates a rule which will return the specified
     * token when a BibTeX-command starting with @ is detected
     *
     * @param token the token to be returned
     */
    public BibCommandRule(IToken token) {
        Assert.isNotNull(token);
        fToken = token;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
     */
    public IToken evaluate(ICharacterScanner scanner) {
        int c = scanner.read();
        if (((char) c) == '@') {
                do {
                    c = scanner.read();
                } while (Character.isLetter((char) c));
                scanner.unread();
                return fToken;
        }
        scanner.unread();
        return Token.UNDEFINED;
    }
}
