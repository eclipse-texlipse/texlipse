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

package org.eclipse.texlipse.editor.scanner;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.texlipse.editor.ColorManager;

/**
 * TexArgScanner is used as a scanner for the "__tex_squareBracketPartition"
 * content type areas.
 * It uses defined rules to detect sequences and it returns the
 * specified token that satisfies a rule. The token defines how the
 * characters are presented.
 * 
 * @see org.eclipse.texlipse.editor.partitioner.FastLaTeXPartitionScanner
 * @author Matthias Erll
 */
public class TexOptArgScanner extends RuleBasedScanner {
    
    /**
     * A default constructor.
     * @param manager
     */
    public TexOptArgScanner(ColorManager manager) {
        IToken commentToken = new Token(new TextAttribute(manager
                .getColor(ColorManager.COMMENT),
                null,
                manager.getStyle(ColorManager.COMMENT_STYLE)));

        //Commands are colored in argument color with command styles 
        IToken commandToken = new Token(
                new TextAttribute(
                        manager.getColor(ColorManager.SQUARE_BRACKETS),
                        null,
                        manager.getStyle(ColorManager.COMMAND_STYLE)));

        List<IRule> rules = new ArrayList<IRule>();
        rules.add(new EndOfLineRule("%", commentToken, '\\'));
        rules.add(new WhitespaceRule(new WhitespaceDetector()));
        rules.add(new WordRule(new TexWord(), commandToken));

        IRule[] result = new IRule[rules.size()];
        rules.toArray(result);
        setRules(result);
    }
}
