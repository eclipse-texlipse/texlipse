/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor.scanner;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.texlipse.editor.ColorManager;

import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.WhitespaceRule;


/**
 * TexCommentScanner is used for "__tex_commentPartition" -content type. 
 * It uses predefined rules to detect sequences and it returns the
 * specified token that satisfies the rule. The token defines how the
 * characters are presented.
 * @see net.sourceforge.texlipse.editor.partitioner.FastLaTeXPartitionScanner
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