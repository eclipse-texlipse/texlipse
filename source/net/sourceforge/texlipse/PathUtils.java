/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Helper methods for environment variable handling.
 * 
 * @author Kimmo Karlsson
 */
public class PathUtils {

	/**
	 * This class needs not to be instantiated.
	 */
	private PathUtils() {}
	
	/**
	 * Convert a properties object to a list of "key=value" Strings.
	 * 
	 * @param prop
	 * @return
	 */
	public static String[] getStrings(Properties prop) {
	    
	    String[] env = new String[prop.size()];
	    int i = 0;
	    
	    Enumeration enum = prop.keys();
	    while (enum.hasMoreElements()) {
	        String key = (String) enum.nextElement();
	        env[i++] = key + '=' + prop.getProperty(key);
	    }
	    
	    return env;
	}

	/**
	 * Splits the given string into tokens so that 
	 * sections of the string that are enclosed into quotes will
	 * form one token (without the quotes).
	 * 
	 * E.g. string = "-editor \"echo %f:%l\" -q"
	 *      tokens = { "-editor", "echo %f:%l", "-q" }
	 * 
	 * @param args the string
	 * @param list tokens will be added to the end of this list
	 *             in the order they are extracted
	 */
	public static void tokenizeEscapedString(String args, ArrayList list) {
	    StringTokenizer st = new StringTokenizer(args, " ");
	    while (st.hasMoreTokens()) {
	        String token = st.nextToken();
	        if (token.charAt(0) == '"') {
	            StringBuffer sb = new StringBuffer();
	            sb.append(token.substring(1));
	            token = st.nextToken();
	            while (!token.endsWith("\"") && st.hasMoreTokens()) {
	                sb.append(' ');
	                sb.append(token);
	                token = st.nextToken();
	            }
	            sb.append(' ');
	            sb.append(token.substring(0, token.length()-1));
	            list.add(sb.toString());
	        } else {
	            list.add(token);
	        }
	    }
	}

	/**
	 * Read the operating system environment variables.
	 * 
	 * @see http://www.rgagnon.com/javadetails/java-0150.html
	 * @return operating system environment variables and their values in a properties object
	 *         or empty properties object if an error occurs
	 */
	public static Properties getEnv() {
	    
	    Properties envVars = new Properties();
	    try {
	        
	        String os = System.getProperty("os.name").toLowerCase();
	        Process p = null;
	        Runtime r = Runtime.getRuntime();
	        
	        if (os.indexOf("windows 9") > -1) {
	            p = r.exec("command.com /c set");
	            PathUtils.loadEscaping(envVars, p.getInputStream());
	        } else if ((os.indexOf("windows nt") > -1)
	                || (os.indexOf("windows 20") > -1)
	                || (os.indexOf("windows xp") > -1)) {
	            p = r.exec("cmd.exe /c set");
	            PathUtils.loadEscaping(envVars, p.getInputStream());
	        } else {
	            p = r.exec("env");
	            envVars.load(p.getInputStream());
	        }
	        
	        p.waitFor();
	        
	    } catch(Exception e) {
	    }
	    
	    return envVars;
	}

	/**
	 * Loads the given properties from the given input stream.
	 * Escapes the backslashes while reading.
	 * @param prop properties object
	 * @param stream the stream
	 * @throws IOException if a read error occurs
	 */
	public static void loadEscaping(Properties prop, InputStream stream) throws IOException {
	    
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    while (true) {
	        int b = stream.read();
	        if (b == -1) {
	            break;
	        } else if (b == '\\') {
	            // write extra backslash to escape them
	            bos.write('\\');
	        }
	        
	        bos.write(b);
	    }
	    
	    StringTokenizer st = new StringTokenizer(bos.toString(), "\r\n");
	    while (st.hasMoreTokens()) {
	        String token = st.nextToken();
	        int index = token.indexOf('=');
	        if (index > 0) {
	            String key = token.substring(0, index);
	            String value = token.substring(index+1);
	            prop.setProperty(key, value);
	        }
	    }
	}

	/**
	 * Finds the property that represents the "path" environment variable.
	 * This has to be done as a search, because environment variables are
	 * case insensitive in Windows and property keys are case sensitive.
	 * 
	 * @param prop the environment variables
	 * @return the key of path environment variable
	 */
	public static String findPathKey(Properties prop) {
	    Enumeration enum = prop.keys();
	    while (enum.hasMoreElements()) {
	        String key = (String) enum.nextElement();
	        if (key.toLowerCase().equals("path")) {
	            return key;
	        }
	    }
	    return "PATH";
	}
}
