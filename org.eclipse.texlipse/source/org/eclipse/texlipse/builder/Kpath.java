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

package org.eclipse.texlipse.builder;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is really little more than a tuple for returning the processed results of kpsewhich
 * 
 * @author Christopher Hoskin
 */
public class Kpath {
	public File path;
	public boolean searchChildren;
	public boolean lsR;
	
	/**
	 * @param kpath - Search path as returned by kpsewhich
	 * 				  [!!]/path/[/]
	 * 				  !! = search ls-R rather than file system
	 * 				  /  = search subfolders
	 */
	public Kpath(String kpath) {
		
		Pattern pattern = Pattern.compile("^(!!)?(.*?)(//)?$");
        Matcher matcher = pattern.matcher(kpath);
        
        matcher.find();

        if (matcher.groupCount()!=3)
        	throw new IllegalArgumentException("Invalid path");

        lsR = (matcher.group(1)!=null);
        searchChildren = (matcher.group(3)!=null);
        
        path = new File(matcher.group(2));
	}

	@Override
	public String toString() {
		return path.toString();
	}

}
