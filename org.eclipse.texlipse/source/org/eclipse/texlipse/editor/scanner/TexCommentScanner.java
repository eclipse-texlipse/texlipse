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

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.texlipse.editor.ColorManager;


/**
 * TexCommentScanner is used for "__tex_commentPartition" -content type. 
 * It uses predefined rules to detect sequences and it returns the
 * specified token that satisfies the rule. The token defines how the
 * characters are presented.
 * @see org.eclipse.texlipse.editor.partitioner.FastLaTeXPartitionScanner
 * @author Antti Pirinen 
 */ 
public class TexCommentScanner extends RuleBasedScanner {
    /**
     * A default constructor. 
     * @param manager
     */
    public TexCommentScanner(ColorManager manager) {					
        List<IRule> rules = new ArrayList<IRule>();
        rules.add(new WhitespaceRule(new WhitespaceDetector()));		
        IRule[] result = new IRule[rules.size()];
        rules.toArray(result);
        setRules(result);		
    }
}