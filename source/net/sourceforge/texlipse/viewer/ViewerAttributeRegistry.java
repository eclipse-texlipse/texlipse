/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.viewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.preference.IPreferenceStore;


/**
 * Instances of this class hold a local copy of the viewer configurations.
 * 
 * @author Kimmo Karlsson
 */
public class ViewerAttributeRegistry implements Cloneable {

    // attribute suffixes
    public static final String ATTRIBUTE_COMMAND = ".command";
    public static final String ATTRIBUTE_ARGUMENTS = ".arguments";
    public static final String ATTRIBUTE_FORMAT = ".format";
    public static final String ATTRIBUTE_INVERSE_SEARCH = ".inverse";
    
    // inverse search attibute values
    public static final String INVERSE_SEARCH_NO = "no";
    public static final String INVERSE_SEARCH_RUN = "run";
    public static final String INVERSE_SEARCH_STD = "std";
    
    // property for the name of the current default viewer
    public static final String VIEWER_CURRENT = "viewerCurrent";
    
    // used to recorver full list of configured viewers from preferences
    public static final String VIEWER_NAMES = "viewerNames";
    
    // default viewers
    static final String VIEWER_NONE = "none";
    private static final String VIEWER_KDVI = "kdvi";
    private static final String VIEWER_XDVI = "xdvi";
    private static final String VIEWER_YAP = "yap";

    private static final String VIEWER_GV = "gv";
    private static final String VIEWER_ACROBAT = "acroread";
    private static final String VIEWER_ITEXMAC = "itexmac";

    // default viewer attribute values 
    private static final String DEFAULT_COMMAND_KDVI = "/usr/bin/kdvi";
    private static final String DEFAULT_ARGUMENTS_KDVI = "%file";
    private static final String DEFAULT_COMMAND_XDVI = "/usr/bin/xdvi";
    private static final String DEFAULT_ARGUMENTS_XDVI = "-editor \"echo %f:%l\" %file";
    private static final String DEFAULT_COMMAND_YAP = "C:\\texmf\\miktex\\bin\\yap.exe";
    private static final String DEFAULT_ARGUMENTS_YAP = "%file";

    private static final String DEFAULT_COMMAND_GV = "/usr/bin/gv";
    private static final String DEFAULT_ARGUMENTS_GV = "%file";
    private static final String DEFAULT_COMMAND_ACROBAT = "acroread";
    private static final String DEFAULT_ARGUMENTS_ACROBAT = "%file";
    private static final String DEFAULT_COMMAND_ITEXMAC = "/usr/bin/open";
    private static final String DEFAULT_ARGUMENTS_ITEXMAC = "-a \"/Applications/iTeXMac 1.3.15/iTeXMac.app\" %file";
    
    
    // viewer attributes
    private HashMap registry;
    
    // active viewer. This variable is not in the registry, because it is needed all the time.
    private String activeViewer;

    /**
     * Construct a new copy of the viewer attributes defined in the preference pages.
     */
    public ViewerAttributeRegistry() {
        
        registry = new HashMap();
        
        activeViewer = VIEWER_ACROBAT;
        setDefaults();
        activeViewer = VIEWER_GV;
        setDefaults();
        activeViewer = VIEWER_NONE;
        setDefaults();
        activeViewer = VIEWER_YAP;
        setDefaults();
        activeViewer = VIEWER_KDVI;
        setDefaults();
        activeViewer = VIEWER_XDVI;
        setDefaults();
        activeViewer = VIEWER_ITEXMAC;
        setDefaults();
        
        setActiveViewer(TexlipsePlugin.getPreference(VIEWER_CURRENT));
    }
    
    /**
     * Set default values to the preferences.
     * Intended to be called only once from the IPreferenceInitializer.
     * @param prefs the preferences
     */
    public static void initializeDefaults(IPreferenceStore prefs) {
    	
        // find out the probable default viewer
        String def = VIEWER_XDVI;
        String os = System.getProperty("os.name");
        if (os.indexOf("indow") > 0) {
            def = VIEWER_YAP;
        } else if (os.indexOf("OS X") > 0) {
            def = VIEWER_ITEXMAC;
        }
        prefs.setDefault(VIEWER_CURRENT, def);
        
        // list the viewers
        ArrayList vlist = new ArrayList();
        vlist.add(VIEWER_NONE);
        vlist.add(VIEWER_XDVI);
        vlist.add(VIEWER_YAP);
        vlist.add(VIEWER_ITEXMAC);
        vlist.add(VIEWER_KDVI);
        vlist.add(VIEWER_GV);
        vlist.add(VIEWER_ACROBAT);
        vlist.remove(def);
        StringBuffer sb = new StringBuffer(def); // put default on front
        for (int i = 0; i < vlist.size(); i++) {
            sb.append(',');
            sb.append((String) vlist.get(i));
        }
        prefs.setDefault(VIEWER_NAMES, sb.toString());
        
        // default values for viewer preferences
        
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_COMMAND, "");
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_ARGUMENTS, "");
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PDF);
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_NO);
        
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_COMMAND, DEFAULT_COMMAND_KDVI);
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_KDVI);
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_DVI);
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_RUN);
        
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_COMMAND, DEFAULT_COMMAND_XDVI);
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_XDVI);
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_DVI);
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_STD);
        
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_COMMAND, DEFAULT_COMMAND_YAP);
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_YAP);
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_DVI);
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_RUN);

        prefs.setDefault(VIEWER_GV + ATTRIBUTE_COMMAND, DEFAULT_COMMAND_GV);
        prefs.setDefault(VIEWER_GV + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_GV);
        prefs.setDefault(VIEWER_GV + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PS);
        prefs.setDefault(VIEWER_GV + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_NO);

        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_COMMAND, DEFAULT_COMMAND_ACROBAT);
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_ACROBAT);
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PDF);
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_NO);

        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_COMMAND, DEFAULT_COMMAND_ITEXMAC);
        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_ITEXMAC);
        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PDF);
        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_RUN);
    }

    /**
     * Set the active viewer's attributes to default values.
     */
    public void setDefaults() {
        registry.put(activeViewer + ATTRIBUTE_COMMAND, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_COMMAND));
        registry.put(activeViewer + ATTRIBUTE_ARGUMENTS, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_ARGUMENTS));
        registry.put(activeViewer + ATTRIBUTE_FORMAT, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_FORMAT));
        registry.put(activeViewer + ATTRIBUTE_INVERSE_SEARCH, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_INVERSE_SEARCH));
    }

    /**
     * Load the values of viewer attributes from preferences.
     * @param pref plugin preferences
     */
    public void load(IPreferenceStore pref) {

        String list = pref.getString(VIEWER_NAMES);
        if (list != null && list.indexOf(',') > 0) {
            String[] names = list.split(",");
            // String.split can't return null
            for (int i = 0; i < names.length; i++) {
                registry.put(names[i] + ATTRIBUTE_COMMAND, pref.getString(names[i] + ATTRIBUTE_COMMAND));
                registry.put(names[i] + ATTRIBUTE_ARGUMENTS, pref.getString(names[i] + ATTRIBUTE_ARGUMENTS));
                registry.put(names[i] + ATTRIBUTE_FORMAT, pref.getString(names[i] + ATTRIBUTE_FORMAT));
                registry.put(names[i] + ATTRIBUTE_INVERSE_SEARCH, pref.getString(names[i] + ATTRIBUTE_INVERSE_SEARCH));
            }
            if (names.length > 0) {
                activeViewer = names[0];
            }
        } else if (list != null && list.length() > 0) {
            activeViewer = list;
            setCommand(pref.getString(list + ATTRIBUTE_COMMAND));
            setArguments(pref.getString(list + ATTRIBUTE_ARGUMENTS));
            setFormat(pref.getString(list + ATTRIBUTE_FORMAT));
            setInverse(pref.getString(list + ATTRIBUTE_INVERSE_SEARCH));
        }
    }
    
    /**
     * Save the values of registry to preferences.
     * @param pref plugin preferences
     * @param viewers the currently configured viewers in the correct order
     */
    public void save(IPreferenceStore pref, String[] viewers) {
        
        StringBuffer sb = new StringBuffer();
        // we can't read viewer names directly from VIEWER_NAMES preference,
        // because that is not in sync
        for (int i = 0; i < viewers.length; i++) {
            
            String name = (String) viewers[i];
            String cmdKey = name + ATTRIBUTE_COMMAND;
            String argKey = name + ATTRIBUTE_ARGUMENTS;
            String formatKey = name + ATTRIBUTE_FORMAT;
            String invKey = name + ATTRIBUTE_INVERSE_SEARCH;
            
            pref.setValue(cmdKey, (String) registry.get(cmdKey));
            pref.setValue(argKey, (String) registry.get(argKey));
            pref.setValue(formatKey, (String) registry.get(formatKey));
            pref.setValue(invKey, (String) registry.get(invKey));
            
            sb.append(name);
            sb.append(',');
        }
        
        pref.setValue(VIEWER_CURRENT, activeViewer);
        // remove last comma
        sb.delete(sb.length()-1,sb.length());
        pref.setValue(VIEWER_NAMES, sb.toString());
    }
    
    /**
     * Scroll through the registry and find names of viewers.
     * @return list of viewer names
     */
    public String[] getViewerList() {
        ArrayList list = new ArrayList();
        Iterator iter = registry.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key.endsWith(ATTRIBUTE_COMMAND)) {
                String name = key.substring(0, key.indexOf(ATTRIBUTE_COMMAND));
                list.add(name);
            }
        }
        String[] arr = (String[]) list.toArray(new String[0]);
        Arrays.sort(arr);
        return arr;
    }

    /**
     * @return index of the active viewer in the given list 
     */
    public int getActiveViewerIndex(String[] list) {
        for (int i = 0; i < list.length; i++) {
            if (activeViewer.equals(list[i])) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * @return the active viewer
     */
    public String getActiveViewer() {
        return activeViewer;
    }
    
    /**
     * @param activeViewer new active viewer
     */
    public void setActiveViewer(String activeViewer) {
        this.activeViewer = activeViewer;
    }
    
    /**
     * @return the current viewer's program location
     */
    public String getCommand() {
        String value = (String) registry.get(activeViewer + ATTRIBUTE_COMMAND);
        if (value == null) {
            value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's program name and location.
     * @param cmd program's full path
     */
    public void setCommand(String cmd) {
        registry.put(activeViewer + ATTRIBUTE_COMMAND, cmd);
    }
    
    /**
     * @return the current viewer's command line arguments
     */
    public String getArguments() {
        String value = (String) registry.get(activeViewer + ATTRIBUTE_ARGUMENTS);
        if (value == null) {
            value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's command line arguments.
     * @param args command line arguments
     */
    public void setArguments(String args) {
        registry.put(activeViewer + ATTRIBUTE_ARGUMENTS, args);
    }

    /**
     * @return the current viewer's input file format
     */
    public String getFormat() {
        String value = (String) registry.get(activeViewer + ATTRIBUTE_FORMAT);
        if (value == null) {
            value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's input file format.
     * @param format file format (the file extension)
     */
    public void setFormat(String format) {
        registry.put(activeViewer + ATTRIBUTE_FORMAT, format);
    }

    /**
     * @return the current viewer's inverse search support
     */
    public String getInverse() {
        String value = (String) registry.get(activeViewer + ATTRIBUTE_INVERSE_SEARCH);
        if (value == null) {
            value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's inverse search support.
     * @param inv inverse search support
     */
    public void setInverse(String inv) {
        registry.put(activeViewer + ATTRIBUTE_INVERSE_SEARCH, inv);
    }

    /**
     * Overwrite the attributes of this registry with the given values.
     * @param regMap hashmap containing new values for viewer attributes
     */
    public void setValues(Map regMap) {
        registry.putAll(regMap);
        activeViewer = (String) regMap.get(VIEWER_CURRENT);
    }
    
    /**
     * @return a copy of the attributes of this registry
     */
    public Map asMap() {
        HashMap map = new HashMap();
        map.putAll(registry);
        map.put(VIEWER_CURRENT, activeViewer);
        return map;
    }
    
    /**
     * Clone this registry.
     * @return a copy of this registry
     */
    public Object clone() {
        ViewerAttributeRegistry reg = new ViewerAttributeRegistry();
        reg.registry = new HashMap();
        reg.setValues(asMap());
        return reg;
    }

    /**
     * Removes a viewer from registry.
     * @param item the viewer name to remove
     */
    public void remove(String item) {
        registry.remove(item + ATTRIBUTE_COMMAND);
        registry.remove(item + ATTRIBUTE_ARGUMENTS);
        registry.remove(item + ATTRIBUTE_FORMAT);
        registry.remove(item + ATTRIBUTE_INVERSE_SEARCH);
    }

    /**
     * @return list of output formats supported by these viewers
     */
    public String[] getFormatList() {
        return new String[] {
                TexlipseProperties.OUTPUT_FORMAT_DVI,
                TexlipseProperties.OUTPUT_FORMAT_PS,
                TexlipseProperties.OUTPUT_FORMAT_PDF
        };
    }
}
