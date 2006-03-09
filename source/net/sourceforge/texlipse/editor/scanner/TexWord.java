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
 * Detects command words. These strings begin with '\' character
 * that is followed by some string consisting solely of letters.
 * 
 * @author Antti Pirinen
 * @author Oskar Ojala
 */
public class TexWord implements IWordDetector {

	/**
	 * Tests if the character is the '\' character
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
	 * Tests if the character is part of a command, i.e. letter
	 * 
	 * @param c 	the character to test
	 * @return 		<code>true</code> if the character is a letter
	 * 				<code>false</code> otherwise 
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
	 */
	public boolean isWordPart(char c) {
		if (Character.isLetter(c))
			return true;
		return false;
	}
}

