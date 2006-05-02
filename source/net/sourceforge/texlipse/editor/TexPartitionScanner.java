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

import net.sourceforge.texlipse.editor.scanner.TexEnvironmentRule;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

/**
 * 
 * @author Antti Pirinen
 * @author Boris von Loesch
 */
public class TexPartitionScanner extends RuleBasedPartitionScanner {
	
	public final static String TEX_DEFAULT = "__tex_default";
	public final static String TEX_COMMENT = "__tex_commentPartition";
	public static final String TEX_MATH = "__tex_mathPartition"; 
	public static final String TEX_CURLY_BRACKETS = "__tex_curlyBracketPartition";
	public static final String TEX_SQUARE_BRACKETS = "__tex_squareBracketPartition";
    public static final String TEX_VERBATIM = "__tex_VerbatimPartition";
    
	public static final String[] TEX_PARTITION_TYPES = new String[] {
			IDocument.DEFAULT_CONTENT_TYPE, 
			TEX_COMMENT,
			TEX_MATH,
			TEX_CURLY_BRACKETS,
			TEX_SQUARE_BRACKETS,
            TEX_VERBATIM};
	
	public TexPartitionScanner() {
        super();
		IToken math 			= new Token(TEX_MATH);
		IToken texComment 		= new Token(TEX_COMMENT);
        IToken texVerbatim      = new Token(TEX_VERBATIM);
		//IToken curly_bracket 	= new Token(TEX_CURLY_BRACKETS);
		//IToken square_bracket	= new Token(TEX_SQUARE_BRACKETS);
		
		List rules = new ArrayList();
				
		//TODO: mark \\ Token.Undefined
		rules.add(new SingleLineRule("\\%"," ", Token.UNDEFINED)); //no comment when using "\%" in LaTeX 
		rules.add(new EndOfLineRule("%", texComment));
        rules.add(new TexEnvironmentRule("comment", texComment));

        //verbatim style environments (is not 100% correct because of \end {verbatim} is not valid)
        rules.add(new TexEnvironmentRule("verbatim", false, texVerbatim));
        rules.add(new TexEnvironmentRule("Verbatim", false, texVerbatim));
        rules.add(new TexEnvironmentRule("lstlisting", false, texVerbatim));
        rules.add(new SingleLineRule("\\verb+", "+", texVerbatim));

        rules.add(new SingleLineRule("\\\\[","]", Token.UNDEFINED));  //no math when using "\\[]" line breaks
		
		rules.add(new SingleLineRule("\\$", " ", Token.UNDEFINED)); // not a math equation \$

        //This bosh rule is necessary to fix a bug in RuleBasedPartitionScanner
		rules.add(new TexEnvironmentRule("qqfdshfkhsd", false, math));
        rules.add(new MultiLineRule("\\[","\\]", math)); 
        rules.add(new MultiLineRule("$$", "$$", math));
        rules.add(new MultiLineRule("$", "$", math));
        rules.add(new TexEnvironmentRule("equation", true, math));
        rules.add(new TexEnvironmentRule("eqnarray", true, math));
        rules.add(new TexEnvironmentRule("math", false, math));
        rules.add(new TexEnvironmentRule("displaymath", false, math));
        //AMSMath environments
        rules.add(new TexEnvironmentRule("align", true, math));
        rules.add(new TexEnvironmentRule("alignat", true, math));
        rules.add(new TexEnvironmentRule("flalign", true, math));
        rules.add(new TexEnvironmentRule("multline", true, math));
        rules.add(new TexEnvironmentRule("gather", true, math));
        
		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
