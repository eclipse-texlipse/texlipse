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
 * TexTikzScanner is used as a scanner for the "__tex_TikzPartition"
 * content type areas.
 * It uses defined rules to detect sequences and it returns the
 * specified token that satisfies a rule. The token defines how the
 * characters are presented.
 * 
 * @see org.eclipse.texlipse.editor.partitioner.FastLaTeXPartitionScanner
 * @author Matthias Erll
 */
public class TexTikzScanner extends RuleBasedScanner {
    
    /**
     * A default constructor.
     * @param manager
     */
    public TexTikzScanner(ColorManager manager) {
        IToken commandToken = new Token(
                new TextAttribute(
                        manager.getColor(ColorManager.COMMAND),
                        null,
                        manager.getStyle(ColorManager.COMMAND_STYLE)));

        IToken specialCharToken = new Token(new TextAttribute(manager
                .getColor(ColorManager.TEX_SPECIAL),
                null,
                manager.getStyle(ColorManager.TEX_SPECIAL_STYLE)));

        IToken numberToken = new Token(new TextAttribute(manager
                .getColor(ColorManager.TEX_NUMBER),
                null,
                manager.getStyle(ColorManager.TEX_NUMBER_STYLE)));

        IToken commentToken = new Token(new TextAttribute(manager
                .getColor(ColorManager.COMMENT),
                null,
                manager.getStyle(ColorManager.COMMENT_STYLE)));

        IToken argToken = new Token(
                new TextAttribute(
                        manager.getColor(ColorManager.CURLY_BRACKETS),
                        null,
                        manager.getStyle(ColorManager.CURLY_BRACKETS_STYLE)));

        IToken optArgToken = new Token(
                new TextAttribute(
                        manager.getColor(ColorManager.SQUARE_BRACKETS),
                        null,
                        manager.getStyle(ColorManager.SQUARE_BRACKETS_STYLE)));

        List<IRule> rules = new ArrayList<IRule>();
        rules.add(new TexSpecialCharRule(specialCharToken));
        rules.add(new WordRule(new TexWord(), commandToken));
        rules.add(new NumberRule(numberToken));
        rules.add(new EndOfLineRule("%", commentToken, '\\'));
        rules.add(new WhitespaceRule(new WhitespaceDetector()));
        rules.add(new MultiLineRule("{", "}", argToken, '\\'));
        rules.add(new MultiLineRule("[", "]", optArgToken, '\\'));

        IRule[] result = new IRule[rules.size()];
        rules.toArray(result);
        setRules(result);
    }
}
