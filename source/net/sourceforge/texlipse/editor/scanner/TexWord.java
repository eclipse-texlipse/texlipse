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
 * A class detecs command words. These strings begin with '\' character
 * that is followed by some string. Note that we define a list of special 
 * characters that are not allowed to be found from the string.
 * 
 * @author Antti Pirinen
 */
public class TexWord implements IWordDetector {

	/**
	 * Tests is the character '\' character
	 * @param c		the character to test
	 * @return 		<code>true</code> if the character is '\',
	 * 				<code>false</code> otherwise. 
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	 */
	public boolean isWordStart(char c) {
		if (c == '\\')
			return true;
		return false;
	}

	/**
	 * Tests is character one of these:
	 * ' ', '[', '{', '\n', '\\' or '}' 
	 * @param c 	the character to test
	 * @return 		<code>true</code> if the character is NOT the one of
	 * 				the listed,
	 * 				<code>false</code> otherwise 
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
	 */
	public boolean isWordPart(char c) {
		if (c != ' ' && c != '[' && c != '{' && c != '\n' && c != '\\'
			&& c != '}')
			return true;
		return false;
	}
}

