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
