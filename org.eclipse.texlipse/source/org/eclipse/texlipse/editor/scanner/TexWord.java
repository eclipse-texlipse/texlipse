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

