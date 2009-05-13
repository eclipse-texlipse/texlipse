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
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * A class to find special character strings. These strings begin with '\'
 * and it is followed by one charecter. These two charactes are presented as
 * one character at the target text.
 * 
 * @author Antti Pirinen 
 * @author Boris von Loesch
 */
public class TexSpecialCharRule implements IPredicateRule {
	private IToken successToken;
	private char startChar = '\\';
	private List<Character> endChars;
	//private HashMap endChars; 
	
	
	public TexSpecialCharRule(IToken token){
		this.successToken = token;
		initChars();
	}
	
	/**
	 * Defines the charactes that are handled as a special
	 * character afrer '\'-character.
	 */
	private void initChars(){
	    this.endChars = new ArrayList<Character>();
	    endChars.add('\\');	        
	    endChars.add('_');
	    endChars.add('$');
	    endChars.add(' ');
	    endChars.add('\n');
	    endChars.add('\t');
	    endChars.add('\r');
	    endChars.add('{');
	    endChars.add('}');
	    //endChars.add('[');
	    //endChars.add(']');
	    endChars.add('!');
	    endChars.add('.');
	    endChars.add(',');
	    endChars.add('?');
	    endChars.add('"');
	    endChars.add('£');
	    endChars.add('%');
	    endChars.add('^');
	    endChars.add('&');
	    endChars.add('*');
	    endChars.add(':');
	    endChars.add(';');
	    endChars.add('@');
	    endChars.add('\'');
	    endChars.add('#');
	    endChars.add('~');
	    endChars.add('/');
	    //endChars.add('-');
	    endChars.add('+');
	    endChars.add('|');
	    endChars.add('<');
	    endChars.add('>');
	    //endChars.add('(');
	    //endChars.add(')');
	    endChars.add('=');
	    endChars.add((char)65535);
	    Collections.sort(endChars);
	}
	
	/**  
	 * @see org.eclipse.jface.text.rules.IPredicateRule#getSuccessToken()
	 */
	public IToken getSuccessToken() {
		return this.successToken;
	}

	/**
	 * Tests if the current character is '\' character. If it is
	 * calls test for next character.  
	 * @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner, boolean)
	 * @param scanner 	the scanner to read characters
	 * @param resume	shall method start from next character
	 * @return 			the success token if "\X" (X is one of the predefined 
	 * 					characters) matches, Token.UNDEFINED otherwise
	 */
	public IToken evaluate(ICharacterScanner scanner, boolean resume) {
		
		if(resume){
			if (evaluateNext(scanner)){
				return successToken;
			}else{
				return Token.UNDEFINED;				
			}				
		}else{
			return evaluate(scanner);
		}
	}

	/**
	 * Tests if the current character is '\' character. If it is
	 * calls test for next character. 
	 * @see org.eclipse.jface.text.rules.IRule#evaluate(org.eclipse.jface.text.rules.ICharacterScanner)
	 * @param scanner 	the scanner to read characters
	 * @return 			the success token if "\X" (X is one of the predefined 
	 * 					characters) matches, Token.UNDEFINED otherwise
	 */
	public IToken evaluate(ICharacterScanner scanner) {
		int c= scanner.read();
		if (c == startChar){
			if (evaluateNext(scanner)) return successToken;
		}
		scanner.unread();
		return Token.UNDEFINED;
	}

	/**
	 * Tests if the next character is one of the special characters. 
	 * @param scanner
	 * @return <code>true</code> if the character is at the predefined list, 
	 *		   <code>false</code> otherwise.
	 */
	private boolean evaluateNext(ICharacterScanner scanner) {
		int c = scanner.read();
		
		if(c != ICharacterScanner.EOF){
		    if (Collections.binarySearch(endChars, (char)c) >= 0) {
		        return true;
		    }
			else{
				scanner.unread();
			}
		}		
		return false;
	}
	
}
