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

import org.eclipse.jface.text.rules.IWordDetector;

/**
 * A class to find ordinary words. Note that we define a list of special characters
 * that are not allowed to be found from the string. 
 *
 * @author Antti Pirinen
 * @author Boris von Loesch 
 */
public class TextWords implements IWordDetector {

	public TextWords() {
		super();
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
	    if (Character.isLetter(c)) return true;
		return false;
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
	    if (Character.isLetter(c) || c == '-') return true;
        return false;
	}

}