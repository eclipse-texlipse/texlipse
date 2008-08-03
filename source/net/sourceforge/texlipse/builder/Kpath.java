/*
 * $Id$
 *
 * Copyright (c) 2008 by Christopher Hoskin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

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
