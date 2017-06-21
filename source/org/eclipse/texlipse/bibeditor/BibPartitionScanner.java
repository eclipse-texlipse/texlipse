/*******************************************************************************
 * Copyright (c) 2017 the TeXlipse team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/
package org.eclipse.texlipse.bibeditor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

/**
 * BibTeX partition scanner. Divides the BibTeX -document into two
 * types of partitions: one for inside the entries and one outside.
 * 
 * @author Oskar Ojala
 */
public class BibPartitionScanner extends RuleBasedPartitionScanner {

    public final static String BIB_ENTRY = "__bib_entry";
    public final static String[] BIB_PARTITION_TYPES = new String[] { BIB_ENTRY };


    /**
     * Creates the partitioner and sets up the appropriate rules.
     */
    public BibPartitionScanner() {
        super();

        IToken bibEntry = new Token(BIB_ENTRY);

        List rules = new ArrayList();

        // Add rule for single line comments.
        // rules.add(new EndOfLineRule("//", Token.UNDEFINED));

        // Add rule for strings and character constants.
        // rules.add(new SingleLineRule("\"", "\"", Token.UNDEFINED, '\\'));
        // rules.add(new SingleLineRule("'", "'", Token.UNDEFINED, '\\'));

        // Add rules for BibTeX entries
        //rules.add(new MultiLineRule("{", "}", bibEntry, (char) 0, false));
        rules.add(new BibBraceRule(true, bibEntry));;

        IPredicateRule[] result= new IPredicateRule[rules.size()];
        rules.toArray(result);
        setPredicateRules(result);
    }
}
