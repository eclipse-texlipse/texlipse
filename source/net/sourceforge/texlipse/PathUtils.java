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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import net.sourceforge.texlipse.properties.StringListFieldEditor;

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
     * Merge the given environment variables to the additional environment variables
     * that are defined in the preferences. The variables defined in the preferences
     * override the variables given as a parameter.
     * 
     * @param env current environment variables
     * @param prefName preference name where to read additional environment variable map
     * @return merged environment variables as a suitable array for the Process.exec() -method
     */
    public static String[] mergeEnvFromPrefs(Properties envProp, String prefName) {
        
        HashMap environment = new HashMap();
        environment.putAll(envProp);
        Map prefsMap = getPreferenceMap(prefName);
        String[] keys = (String[]) prefsMap.keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            String value = (String) prefsMap.get(keys[i]);
            value = replaceVar(envProp, value);
            environment.put(keys[i], value);
        }
        return getStrings(environment);
    }
    
    /**
     * Replaces environment variable names with their values in the given string.
     * 
     * @param envProp environment variables
     * @param str value for a new environment variable
     * @return the given value with variables expanded
     */
    private static String replaceVar(Properties envProp, String str) {
        // TODO: perform variable expansion
        return str;
    }

    /**
     * Convert a Map object to an array of "key=value" -Strings.
     * 
     * @param map key/value mappings as a Map
     * @return array of "key=value" Strings
     */
    public static String[] getStrings(Map map) {
        
        String[] array = new String[map.size()];
        String[] keys = (String[]) map.keySet().toArray(new String[0]);
        
        for (int i = 0; i < keys.length; i++) {
            array[i] = keys[i] + '=' + (String) map.get(keys[i]);
        }
        
        return array;
    }

    /**
     * Load a Map from preferences. The map is encoded into a String object
     * by separating keys from values with a '=' and key/value-pairs with a ','.
     * E.g.: "key1=val1,key2=val2"
     * 
     * @param name preference name
     * @return always non-null Map of String to String -mappings
     */
    public static Map getPreferenceMap(String name) {
        Map map = new HashMap();
        
        String str = TexlipsePlugin.getPreference(name);
        if (str == null) {
            return map;
        }
        
        String[] binds = str.split(StringListFieldEditor.SEPARATOR);
        if (binds == null) {
            return map;
        }
        
        for (int i = 0; i < binds.length; i++) {
            
            int index = binds[i].indexOf('=');
            if (index <= 0) {
                continue;
            }
            
            map.put(binds[i].substring(0, index),
                    binds[i].substring(index+1));
        }
        
        return map;
    }
    
	/**
	 * Convert a Properties object to an array of "key=value" -Strings.
	 * 
	 * @param prop key/value -mappings as properties
	 * @return an array of "key=value" -Strings.
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
