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

import java.util.HashMap;

import org.eclipse.jface.text.rules.IWordDetector;

/**
 * A class to find ordinary words. Note that we define a list of special characters
 * that are not allowed to be found from the string. 
 *
 * @author Antti Pirinen 
 */
public class TextWords implements IWordDetector {

	HashMap ignoreChars;

	public TextWords() {
		super();
		ignoreChars = new HashMap();
		makeIgnoreChars();
	}

	/**
	 * Tests if character is one of the special ones.
	 * @param c		the character that must be tested
	 * @return		<code>true</code> if the character is not
	 * 				forbidden one,
	 * 				<code>false</code> otherwise
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	 */
	public boolean isWordStart(char c) {
		if (ignoreChars.containsKey(new Character(c))) {
			return false;
		}
		return true;
	}

	/**
	 * Tests if character is one of the special ones.
	 * @param c		the character that must be tested
	 * @return		<code>true</code> if the character is not
	 * 				forbidden one,
	 * 				<code>false</code> otherwise
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
	 */
	public boolean isWordPart(char c) {
		if (ignoreChars.containsKey(new Character(c))) {
			return false;
		}
		return true;
	}

	/**
	 * A list of charcters that are not allowed to be 
	 * at normal text
	 */
	private void makeIgnoreChars() {
		ignoreChars = new HashMap();
		ignoreChars.put(new Character(' '), new Object());
		ignoreChars.put(new Character('\n'), new Object());
		ignoreChars.put(new Character('\t'), new Object());
		ignoreChars.put(new Character('\r'), new Object());
		ignoreChars.put(new Character('{'), new Object());
		ignoreChars.put(new Character('}'), new Object());
		ignoreChars.put(new Character('['), new Object());
		ignoreChars.put(new Character(']'), new Object());
		ignoreChars.put(new Character('!'), new Object());
		ignoreChars.put(new Character('.'), new Object());
		ignoreChars.put(new Character(','), new Object());
		ignoreChars.put(new Character('?'), new Object());
		ignoreChars.put(new Character('"'), new Object());
		ignoreChars.put(new Character('£'), new Object());
		ignoreChars.put(new Character('$'), new Object());
		ignoreChars.put(new Character('%'), new Object());
		ignoreChars.put(new Character('^'), new Object());
		ignoreChars.put(new Character('&'), new Object());
		ignoreChars.put(new Character('*'), new Object());
		ignoreChars.put(new Character(':'), new Object());
		ignoreChars.put(new Character(';'), new Object());
		ignoreChars.put(new Character('@'), new Object());
		ignoreChars.put(new Character('\''), new Object());
		ignoreChars.put(new Character('#'), new Object());
		ignoreChars.put(new Character('~'), new Object());
		ignoreChars.put(new Character('/'), new Object());
		//ignoreChars.put(new Character('-'), new Object());
		ignoreChars.put(new Character('+'), new Object());
		ignoreChars.put(new Character('|'), new Object());
		ignoreChars.put(new Character('\\'), new Object());
		ignoreChars.put(new Character('<'), new Object());
		ignoreChars.put(new Character('>'), new Object());
		ignoreChars.put(new Character('('), new Object());
		ignoreChars.put(new Character(')'), new Object());
		ignoreChars.put(new Character('='), new Object());
		ignoreChars.put(new Character(((char)65535)), new Object());		
	}
}