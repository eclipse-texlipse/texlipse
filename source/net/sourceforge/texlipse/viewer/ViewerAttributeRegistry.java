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
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.PathUtils;
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.preference.IPreferenceStore;


/**
 * Instances of this class hold a local copy of the viewer configurations.
 * 
 * @author Kimmo Karlsson
 * @author Tor Arne Vestbø
 */
public class ViewerAttributeRegistry implements Cloneable {

    // attribute suffixes
    public static final String ATTRIBUTE_COMMAND = ".command";
    public static final String ATTRIBUTE_ARGUMENTS = ".arguments";
    public static final String ATTRIBUTE_DDE_VIEW_COMMAND = ".ddeViewCommand";
    public static final String ATTRIBUTE_DDE_VIEW_SERVER = ".ddeViewServer";
    public static final String ATTRIBUTE_DDE_VIEW_TOPIC = ".ddeViewTopic";
    public static final String ATTRIBUTE_DDE_CLOSE_COMMAND = ".ddeCloseCommand";
    public static final String ATTRIBUTE_DDE_CLOSE_SERVER = ".ddeCloseServer";
    public static final String ATTRIBUTE_DDE_CLOSE_TOPIC = ".ddeCloseTopic";
    public static final String ATTRIBUTE_FORMAT = ".format";
    public static final String ATTRIBUTE_INVERSE_SEARCH = ".inverse";
    public static final String ATTRIBUTE_FORWARD_SEARCH = ".forward";
    
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
    private static final String VIEWER_SUMATRA = "sumatra PDF";
    private static final String VIEWER_ITEXMAC = "itexmac";

    // default viewer attribute values 
    private static final String DEFAULT_ARGUMENTS_KDVI = "%file";
    private static final String DEFAULT_ARGUMENTS_XDVI = "-editor \"echo %f:%l\" -sourceposition \"%line %texfile\" %file";
    private static final String DEFAULT_DIR_YAP = "C:\\texmf\\miktex\\bin";
    private static final String DEFAULT_ARGUMENTS_YAP = "-1 -s \"%line %texfile\" %file";

    private static final String DEFAULT_ARGUMENTS_GV = "%file";
    private static final String DEFAULT_ARGUMENTS_SUMATRA = "-reuse-instance %file";
    private static final String DEFAULT_ARGUMENTS_ACROBAT = "%file";
    private static final String DEFAULT_ARGUMENTS_ITEXMAC = "-a \"/Applications/iTeXMac 1.3.15/iTeXMac.app\" %file";
    
    
    // viewer attributes
    private HashMap<String, String> registry;
    
    // active viewer. This variable is not in the registry, because it is needed all the time.
    private String activeViewer;

    // All available viewers in the preferences, sorted by "priority"
    private String[] allViewers;
    
    /**
     * Construct a new copy of the viewer attributes, based on the
     * default viewer defined in the preference pages (first on the list).
     */
    public ViewerAttributeRegistry() {
        
        registry = new HashMap<String, String>();
        
        load(TexlipsePlugin.getDefault().getPreferenceStore());
    }
    
    /**
     * Finds a file from all of the directories listed in the
     * "path" environment variable. Unix default is assumed to be "/usr/bin"
     * 
     * @param filename filename in unix-based systems
     * @param winFilename filename in windows systems
     * @param winPath default path in windows systems
     * @return
     */
    private static String findFromEnvPath(String filename, String winFilename, String winPath) {
        if (filename.length() == 0) {
            filename = winFilename;
        } else if (winFilename.length() == 0) {
            winFilename = filename;
        }
        return PathUtils.findEnvFile(filename, "/usr/bin", winFilename, winPath);
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
        List<String> vlist = new ArrayList<String>();
        vlist.add(VIEWER_XDVI);
        vlist.add(VIEWER_YAP);
        vlist.add(VIEWER_ITEXMAC);
        vlist.add(VIEWER_KDVI);
        vlist.add(VIEWER_GV);
        vlist.add(VIEWER_SUMATRA);
        vlist.add(VIEWER_ACROBAT);
        vlist.add(VIEWER_NONE);
        vlist.remove(def);
        StringBuilder sb = new StringBuilder(def); // put default on front
        for (String viewer : vlist) {
            sb.append(',');
            sb.append(viewer);
        }
        prefs.setDefault(VIEWER_NAMES, sb.toString());
        
        // default values for viewer preferences
        
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_COMMAND, "");
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_ARGUMENTS, "");
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PDF);
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_NO);
        prefs.setDefault(VIEWER_NONE + ATTRIBUTE_FORWARD_SEARCH, "false");
        
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_COMMAND, findFromEnvPath("kdvi", "", ""));
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_KDVI);
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_DVI);
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_RUN);
        prefs.setDefault(VIEWER_KDVI + ATTRIBUTE_FORWARD_SEARCH, "true");
        
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_COMMAND, findFromEnvPath("xdvi", "", ""));
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_XDVI);
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_DVI);
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_STD);
        prefs.setDefault(VIEWER_XDVI + ATTRIBUTE_FORWARD_SEARCH, "true");
        
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_COMMAND, findFromEnvPath("", "yap.exe", DEFAULT_DIR_YAP));
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_YAP);
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_DVI);
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_RUN);
        prefs.setDefault(VIEWER_YAP + ATTRIBUTE_FORWARD_SEARCH, "true");

        prefs.setDefault(VIEWER_GV + ATTRIBUTE_COMMAND, findFromEnvPath("gv", "ghostview.exe", ""));
        prefs.setDefault(VIEWER_GV + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_GV);
        prefs.setDefault(VIEWER_GV + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PS);
        prefs.setDefault(VIEWER_GV + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_NO);
        prefs.setDefault(VIEWER_GV + ATTRIBUTE_FORWARD_SEARCH, "false");

        prefs.setDefault(VIEWER_SUMATRA + ATTRIBUTE_COMMAND, findFromEnvPath("SumatraPDF", "SumatraPDF.exe", "C:\\Program Files\\SumatraPDF"));
        prefs.setDefault(VIEWER_SUMATRA + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_SUMATRA);
        prefs.setDefault(VIEWER_SUMATRA + ATTRIBUTE_DDE_VIEW_COMMAND, "[ForwardSearch(\"%file\",\"%texfile\",%line,0)]"); 
        prefs.setDefault(VIEWER_SUMATRA + ATTRIBUTE_DDE_VIEW_SERVER, "SUMATRA");
        prefs.setDefault(VIEWER_SUMATRA + ATTRIBUTE_DDE_VIEW_TOPIC, "control");
        prefs.setDefault(VIEWER_SUMATRA + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PDF);
        prefs.setDefault(VIEWER_SUMATRA + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_NO);
        prefs.setDefault(VIEWER_SUMATRA + ATTRIBUTE_FORWARD_SEARCH, "true");

        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_COMMAND, findFromEnvPath("acroread", "acroread.exe", ""));
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_ACROBAT);
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_DDE_VIEW_COMMAND, "[DocOpen(\"%fullfile\")][FileOpen(\"%fullfile\")]"); 
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_DDE_VIEW_SERVER, "acroview");
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_DDE_VIEW_TOPIC, "control");
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_DDE_CLOSE_COMMAND, "[DocClose(\"%fullfile\")]");
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_DDE_CLOSE_SERVER, "acroview");
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_DDE_CLOSE_TOPIC, "control");
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PDF);
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_NO);
        prefs.setDefault(VIEWER_ACROBAT + ATTRIBUTE_FORWARD_SEARCH, "false");

        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_COMMAND, findFromEnvPath("open", "", ""));
        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_ARGUMENTS, DEFAULT_ARGUMENTS_ITEXMAC);
        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PDF);
        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_INVERSE_SEARCH, INVERSE_SEARCH_RUN);
        prefs.setDefault(VIEWER_ITEXMAC + ATTRIBUTE_FORWARD_SEARCH, "false");
    }

    /**
     * Set the active viewer's attributes to default values.
     */
    public void setDefaults() {
        registry.put(activeViewer + ATTRIBUTE_COMMAND, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_COMMAND));
        registry.put(activeViewer + ATTRIBUTE_ARGUMENTS, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_ARGUMENTS));
        
        registry.put(activeViewer + ATTRIBUTE_DDE_VIEW_COMMAND, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_DDE_VIEW_COMMAND));
        registry.put(activeViewer + ATTRIBUTE_DDE_VIEW_SERVER, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_DDE_VIEW_SERVER));
        registry.put(activeViewer + ATTRIBUTE_DDE_VIEW_TOPIC, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_DDE_VIEW_TOPIC));
        registry.put(activeViewer + ATTRIBUTE_DDE_CLOSE_COMMAND, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_DDE_CLOSE_COMMAND));
        registry.put(activeViewer + ATTRIBUTE_DDE_CLOSE_SERVER, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_DDE_CLOSE_SERVER));
        registry.put(activeViewer + ATTRIBUTE_DDE_CLOSE_TOPIC, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_DDE_CLOSE_TOPIC));
    
        registry.put(activeViewer + ATTRIBUTE_FORMAT, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_FORMAT));
        registry.put(activeViewer + ATTRIBUTE_INVERSE_SEARCH, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_INVERSE_SEARCH));
        registry.put(activeViewer + ATTRIBUTE_FORWARD_SEARCH, TexlipsePlugin.getPreference(activeViewer + ATTRIBUTE_FORWARD_SEARCH));
    }

    /**
     * Load the values of viewer attributes from preferences.
     * @param pref plugin preferences
     */
    public void load(IPreferenceStore pref) {

        String list = pref.getString(VIEWER_NAMES);
        if (list != null && list.indexOf(',') > 0) {
            String[] names = list.split(",");
            allViewers = names;
            // String.split can't return null
            for (int i = 0; i < names.length; i++) {
                registry.put(names[i] + ATTRIBUTE_COMMAND, pref.getString(names[i] + ATTRIBUTE_COMMAND));
                registry.put(names[i] + ATTRIBUTE_ARGUMENTS, pref.getString(names[i] + ATTRIBUTE_ARGUMENTS));
                registry.put(names[i] + ATTRIBUTE_DDE_VIEW_COMMAND, pref.getString(names[i] + ATTRIBUTE_DDE_VIEW_COMMAND));                
                registry.put(names[i] + ATTRIBUTE_DDE_VIEW_SERVER, pref.getString(names[i] + ATTRIBUTE_DDE_VIEW_SERVER));
                registry.put(names[i] + ATTRIBUTE_DDE_VIEW_TOPIC, pref.getString(names[i] + ATTRIBUTE_DDE_VIEW_TOPIC));
                registry.put(names[i] + ATTRIBUTE_DDE_CLOSE_COMMAND, pref.getString(names[i] + ATTRIBUTE_DDE_CLOSE_COMMAND));                
                registry.put(names[i] + ATTRIBUTE_DDE_CLOSE_SERVER, pref.getString(names[i] + ATTRIBUTE_DDE_CLOSE_SERVER));
                registry.put(names[i] + ATTRIBUTE_DDE_CLOSE_TOPIC, pref.getString(names[i] + ATTRIBUTE_DDE_CLOSE_TOPIC));                
                registry.put(names[i] + ATTRIBUTE_FORMAT, pref.getString(names[i] + ATTRIBUTE_FORMAT));
                registry.put(names[i] + ATTRIBUTE_INVERSE_SEARCH, pref.getString(names[i] + ATTRIBUTE_INVERSE_SEARCH));
                registry.put(names[i] + ATTRIBUTE_FORWARD_SEARCH, pref.getString(names[i] + ATTRIBUTE_FORWARD_SEARCH));
            }
            if (names.length > 0) {
                activeViewer = names[0];
            }
        } else if (list != null && list.length() > 0) {
            activeViewer = list;
            allViewers = new String[]{list};
            setCommand(pref.getString(list + ATTRIBUTE_COMMAND));
            setArguments(pref.getString(list + ATTRIBUTE_ARGUMENTS));
            setDDEViewCommand(pref.getString(list + ATTRIBUTE_DDE_VIEW_COMMAND));
            setDDEViewServer(pref.getString(list + ATTRIBUTE_DDE_VIEW_SERVER));
            setDDEViewTopic(pref.getString(list + ATTRIBUTE_DDE_VIEW_TOPIC));
            setDDEViewCommand(pref.getString(list + ATTRIBUTE_DDE_CLOSE_COMMAND));
            setDDEViewServer(pref.getString(list + ATTRIBUTE_DDE_CLOSE_SERVER));
            setDDEViewTopic(pref.getString(list + ATTRIBUTE_DDE_CLOSE_TOPIC));
            setFormat(pref.getString(list + ATTRIBUTE_FORMAT));
            setInverse(pref.getString(list + ATTRIBUTE_INVERSE_SEARCH));
            setForward(pref.getString(list + ATTRIBUTE_FORWARD_SEARCH));
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
            
            String name = viewers[i];
            String cmdKey = name + ATTRIBUTE_COMMAND;
            String argKey = name + ATTRIBUTE_ARGUMENTS;
            String ddeViewCommandKey = name + ATTRIBUTE_DDE_VIEW_COMMAND;            
            String ddeViewServerKey = name + ATTRIBUTE_DDE_VIEW_SERVER;
            String ddeViewTopicKey = name + ATTRIBUTE_DDE_VIEW_TOPIC;
            String ddeCloseCommandKey = name + ATTRIBUTE_DDE_CLOSE_COMMAND;            
            String ddeCloseServerKey = name + ATTRIBUTE_DDE_CLOSE_SERVER;
            String ddeCloseTopicKey = name + ATTRIBUTE_DDE_CLOSE_TOPIC;
            String formatKey = name + ATTRIBUTE_FORMAT;
            String invKey = name + ATTRIBUTE_INVERSE_SEARCH;
            String frwKey = name + ATTRIBUTE_FORWARD_SEARCH;
            
            pref.setValue(cmdKey, registry.get(cmdKey));
            pref.setValue(argKey, registry.get(argKey));
            pref.setValue(ddeViewCommandKey, registry.get(ddeViewCommandKey));
            pref.setValue(ddeViewServerKey, registry.get(ddeViewServerKey));
            pref.setValue(ddeViewTopicKey, registry.get(ddeViewTopicKey));
            pref.setValue(ddeCloseCommandKey, registry.get(ddeCloseCommandKey));
            pref.setValue(ddeCloseServerKey, registry.get(ddeCloseServerKey));
            pref.setValue(ddeCloseTopicKey, registry.get(ddeCloseTopicKey));
            pref.setValue(formatKey, registry.get(formatKey));
            pref.setValue(invKey, registry.get(invKey));
            pref.setValue(frwKey, registry.get(frwKey));
            
            sb.append(name);
            sb.append(',');
        }
        
        pref.setValue(VIEWER_CURRENT, activeViewer);
        // remove last comma
        sb.delete(sb.length()-1,sb.length());
        pref.setValue(VIEWER_NAMES, sb.toString());
    }
    
    /**
     * Merges this registry with another registry
     * 
     * @param reg The registry to merge with
     */
    public void mergeWith(ViewerAttributeRegistry reg) {
	    	registry.putAll(reg.asMap());
    }
    
    /**
     * Scroll through the registry and find names of viewers.
     * @return list of viewer names
     */
    public String[] getViewerList() {
        ArrayList<String> list = new ArrayList<String>();
        Iterator<String> iter = registry.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            if (key.endsWith(ATTRIBUTE_COMMAND)) {
                String name = key.substring(0, key.indexOf(ATTRIBUTE_COMMAND));
                list.add(name);
            }
        }
        String[] arr = list.toArray(new String[0]);
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
     * Gets the preferred previewer for a given output format
     * 
     * @param format The target output format
     * @return The first matching previewer that supports the given format
     */
    public String getPreferredViewer(String format) {
        // Find first match
        for (int i = 0; i < allViewers.length; i++) {
            String viewerOutputFormat = registry.get(allViewers[i] + ATTRIBUTE_FORMAT);           
            if (viewerOutputFormat.equals(format)) {
                return allViewers[i];
            }
        } 
        
        return null;
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
        String value = registry.get(activeViewer + ATTRIBUTE_COMMAND);
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
        String value = registry.get(activeViewer + ATTRIBUTE_ARGUMENTS);
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
     * @return the current viewer's dde view command
     */
    public String getDDEViewCommand() {
        String value = registry.get(activeViewer + ATTRIBUTE_DDE_VIEW_COMMAND);
        if (value == null) {
        	value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's dde view command
     * @param command command line arguments
     */
    public void setDDEViewCommand(String command) {
        registry.put(activeViewer + ATTRIBUTE_DDE_VIEW_COMMAND, command);
    }
    
    /**
     * @return the current viewer's dde view server
     */
    public String getDDEViewServer() {
        String value = registry.get(activeViewer + ATTRIBUTE_DDE_VIEW_SERVER);
        if (value == null) {
        	value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's dde view server
     * @param command command line arguments
     */
    public void setDDEViewServer(String server) {
        registry.put(activeViewer + ATTRIBUTE_DDE_VIEW_SERVER, server);
    }
    
    /**
     * @return the current viewer's dde view topic
     */
    public String getDDEViewTopic() {
        String value = registry.get(activeViewer + ATTRIBUTE_DDE_VIEW_TOPIC);
        if (value == null) {
        	value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's dde view topic
     * @param command command line arguments
     */
    public void setDDEViewTopic(String topic) {
        registry.put(activeViewer + ATTRIBUTE_DDE_VIEW_TOPIC, topic);
    }
    
    
    /**
     * @return the current viewer's dde Close command
     */
    public String getDDECloseCommand() {
        String value = registry.get(activeViewer + ATTRIBUTE_DDE_CLOSE_COMMAND);
        if (value == null) {
        	value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's dde close command
     * @param command command line arguments
     */
    public void setDDECloseCommand(String command) {
        registry.put(activeViewer + ATTRIBUTE_DDE_CLOSE_COMMAND, command);
    }
    
    /**
     * @return the current viewer's dde close server
     */
    public String getDDECloseServer() {
        String value = registry.get(activeViewer + ATTRIBUTE_DDE_CLOSE_SERVER);
        if (value == null) {
        	value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's dde close server
     * @param command command line arguments
     */
    public void setDDECloseServer(String server) {
        registry.put(activeViewer + ATTRIBUTE_DDE_CLOSE_SERVER, server);
    }
    
    /**
     * @return the current viewer's dde close topic
     */
    public String getDDECloseTopic() {
        String value = registry.get(activeViewer + ATTRIBUTE_DDE_CLOSE_TOPIC);
        if (value == null) {
        	value = "";
        }
        return value;
    }
    
    /**
     * Set the current viewer's dde view topic
     * @param command command line arguments
     */
    public void setDDECloseTopic(String topic) {
        registry.put(activeViewer + ATTRIBUTE_DDE_CLOSE_TOPIC, topic);
    }
    

    /**
     * @return the current viewer's input file format
     */
    public String getFormat() {
        String value = registry.get(activeViewer + ATTRIBUTE_FORMAT);
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
        String value = registry.get(activeViewer + ATTRIBUTE_INVERSE_SEARCH);
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
     * @return the current viewer's forward search support
     */
    public boolean getForward() {
        String value = registry.get(activeViewer + ATTRIBUTE_FORWARD_SEARCH);
        if (value == null) {
            return false;
        }
        return value.equals("true");
    }
    
    /**
     * Set the current viewer's forward search support.
     * @param forward forward search support
     */
    public void setForward(String forward) {
        registry.put(activeViewer + ATTRIBUTE_FORWARD_SEARCH, forward);
    }

    /**
     * Set the current viewer's forward search support.
     * @param forward forward search support
     */
    public void setForward(boolean forward) {
        registry.put(activeViewer + ATTRIBUTE_FORWARD_SEARCH, forward + "");
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
    public Map<String, String> asMap() {
        HashMap<String, String> map = new HashMap<String, String>();
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
        reg.registry = new HashMap<String, String>();
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
        registry.remove(item + ATTRIBUTE_DDE_VIEW_COMMAND);
        registry.remove(item + ATTRIBUTE_DDE_VIEW_SERVER);
        registry.remove(item + ATTRIBUTE_DDE_VIEW_TOPIC);
        registry.remove(item + ATTRIBUTE_DDE_CLOSE_COMMAND);
        registry.remove(item + ATTRIBUTE_DDE_CLOSE_SERVER);
        registry.remove(item + ATTRIBUTE_DDE_CLOSE_TOPIC);
        registry.remove(item + ATTRIBUTE_FORMAT);
        registry.remove(item + ATTRIBUTE_INVERSE_SEARCH);
        registry.remove(item + ATTRIBUTE_FORWARD_SEARCH);
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
