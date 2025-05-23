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

import org.eclipse.jface.text.rules.IWhitespaceDetector;

/**
 * Class uses a method <code>isWhitespace</code> from 
 * <code>Character</code> class to test is the character
 * a white space character.
 *  
 * @author Antti Pirinen
 */
public class WhitespaceDetector implements IWhitespaceDetector {

	/**
	 * Detects is the given character a white space character.
	 * @param c 	a character to test
	 * @return 		<code>true</code> if the character is a white space character, 
	 * 				<code>false</code> otherwise.  
	 * @see org.eclipse.jface.text.rules.IWhitespaceDetector#isWhitespace(char)
	 */
	public boolean isWhitespace(char c) {
		return Character.isWhitespace(c);
	}	
}
