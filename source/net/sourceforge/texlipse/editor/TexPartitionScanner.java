/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

/**
 * @author Antti Pirinen
 *
 */
public class TexPartitionScanner extends RuleBasedPartitionScanner {
	
	public final static String TEX_DEFAULT = "__tex_default";
	public final static String TEX_COMMENT = "__tex_commentPartition";
	public static final String TEX_MATH = "__tex_mathParitition"; 
	public static final String TEX_CURLY_BRACKETS = "__tex_curlyBracketPartition";
	public static final String TEX_SQUARE_BRACKETS = "__tex_squareBracketPartition";
	public static final String[] TEX_PARTITION_TYPES = new String[] {
			TEX_DEFAULT, 
			TEX_COMMENT,
			TEX_MATH,
			TEX_CURLY_BRACKETS,
			TEX_SQUARE_BRACKETS };
		
	public TexPartitionScanner() {
		IToken math 			= new Token(TEX_MATH);
		IToken texComment 		= new Token(TEX_COMMENT);
		IToken curly_bracket 	= new Token(TEX_CURLY_BRACKETS);
		IToken square_bracket	= new Token(TEX_SQUARE_BRACKETS);
		
		List rules= new ArrayList();
				
		rules.add(new SingleLineRule("\\%"," ", Token.UNDEFINED)); //no comment when using "\%" in LaTeX 
		rules.add(new EndOfLineRule("%", texComment));
		rules.add(new MultiLineRule("\\begin{comment}","\\end{comment}",texComment));
		rules.add(  new SingleLineRule("\\\\[","]", Token.UNDEFINED));  //no math when using "\\[]" line breaks
		rules.add( new MultiLineRule("\\[","\\]", math)); 
		
		rules.add(new SingleLineRule("\\$"," ",Token.UNDEFINED)); // not a math equation \$
		rules.add(new MultiLineRule("\\begin{equation}","\\end{equation}", math)); 
		rules.add(new MultiLineRule("\\begin{equation*}","\\end{equation*}", math)); 
		rules.add(new MultiLineRule("$","$", math)); 
				
		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
		
	}
}
