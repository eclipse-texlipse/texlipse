/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.texlipse.PathUtils;
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.MarkerUtilities;


/**
 * Helper methods for external program runners.
 * 
 * @author Kimmo Karlsson
 */
public abstract class AbstractProgramRunner implements ProgramRunner {
    
    // the currently running program
    private ExternalProgram extrun;
    
    /**
     * Create a new program runner.
     * @param project the project holding the properties
     * @param propertyName name of the property containing the name of the program
     */
    protected AbstractProgramRunner() {
        extrun = new ExternalProgram();
    }

    /**
     * @return the name of the program runner arguments -preference in the plugin preferences
     */
    private String getArgumentsPreferenceName() {
        return getClass() + "_args";
    }
    
    /**
     * @return the name of the program runner path -preference in the plugin preferences
     */
    private String getCommandPreferenceName() {
        return getClass() + "_prog";
    }
    
    /**
     * @return the program path and filename from the preferences
     */
    public String getProgramPath() {
        return TexlipsePlugin.getPreference(getCommandPreferenceName());
    }
    
    /**
     * @param path the program path and filename for the preferences
     */
    public void setProgramPath(String path) {
        TexlipsePlugin.getDefault().getPreferenceStore().setValue(getCommandPreferenceName(), path);
    }
    
    /**
     * Read the command line arguments for the program from the preferences.
     * The input filename is marked with a "%input" and the output file name
     * is marked with a "%output".
     * @return the command line arguments for the program
     */
    public String getProgramArguments() {
        return TexlipsePlugin.getPreference(getArgumentsPreferenceName());
    }
    
    /**
     * @param args the program arguments for the preferences
     */
    public void setProgramArguments(String args) {
        TexlipsePlugin.getDefault().getPreferenceStore().setValue(getArgumentsPreferenceName(), args);
    }
    
    /**
     *
     */
    public void initializeDefaults(IPreferenceStore pref, String path) {
        pref.setDefault(getCommandPreferenceName(), path);
        pref.setDefault(getArgumentsPreferenceName(), getDefaultArguments());
    }
    
    /**
     * Returns the default value for the program arguments -preference.
     * @return the program arguments
     */
    protected String getDefaultArguments() {
        return "%input";
    }
    
    /**
     * @return the name of the executable program
     */
    public String getProgramName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.indexOf("windows") >= 0) {
            return getWindowsProgramName();
        } else {
            return getUnixProgramName();
        }
    }
    
    protected abstract String getWindowsProgramName();
    protected abstract String getUnixProgramName();
    
    /**
     * @param resource the input file to be processed
     * @return the arguments to give to the external program
     */
    protected String getArguments(IResource resource) {
        String args = getProgramArguments();
        if (args == null) {
            return null;
        }
        String ext = resource.getFileExtension();
        String name = resource.getName();
        String baseName = name.substring(0, name.length() - ext.length());
        String inputName = baseName + getInputFormat();
        String outputName = baseName + getOutputFormat();
        if (baseName.indexOf(' ') >= 0) {
            inputName = "\"" + inputName + "\"";
            outputName = "\"" + outputName + "\"";
        }
        
        if (args.indexOf("%input") >= 0) {
            args = args.replaceAll("%input", inputName);
        }
        if (args.indexOf("%output") >= 0) {
            args = args.replaceAll("%output", outputName);
        }
        if (args.indexOf("%fullinput") >= 0) {
            args = args.replaceAll("%fullinput",
                    resource.getParent().getLocation().toFile().getAbsolutePath()
                    + File.separator + inputName);
        }
        if (args.indexOf("%fulloutput") >= 0) {
            args = args.replaceAll("%fulloutput",
                    resource.getParent().getLocation().toFile().getAbsolutePath()
                    + File.separator + outputName);
        }
        return args;
    }
    
    /**
     * Parse errors from the output of an external program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected abstract boolean parseErrors(IResource resource, String output);
    
    /**
     * Check to see if this program is ready for operation.
     * @return true if this program exists
     */
    public boolean isValid() {
        if (getProgramPath() == null) {
            return false;
        }
        File f = new File(getProgramPath());
        return f.exists() && f.isFile();
    }
    
    /**
     * The main method.
     * 
     * @param resource the input file to feed to the external program
     * @throws CoreException if the external program is not found
     *                       or if there was an error during the build
     */
    public void run(IResource resource) throws CoreException {
        
        File sourceDir = resource.getLocation().toFile().getParentFile();
        
        // find executable file
        String programPath = getProgramPath();
        File exec = new File(programPath);
        if (!exec.exists()) {
            throw new CoreException(TexlipsePlugin.stat("External program (" + programPath + ") not found"));
        }
        
        // split command into array
        ArrayList list = new ArrayList();
        list.add(exec.getAbsolutePath());
        PathUtils.tokenizeEscapedString(getArguments(resource), list);
        String[] command =  (String[]) list.toArray(new String[0]);
        
        // check if we are using console
        String console = null;
        if (TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.BUILDER_CONSOLE_OUTPUT)) {
            console = getProgramName();
        }
        extrun.setup(command, sourceDir, console);
        
        String output = null;
        try {
            
            String[] query = getQueryString();
            if (query != null) {
                output = extrun.run(query);
            } else {
                output = extrun.run();
            }
            
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, TexlipsePlugin.getPluginId(),
                    IStatus.ERROR, "Building the project: ", e));
        } finally {
            extrun.stop();
        }

        if (parseErrors(resource, output)) {
            throw new BuilderCoreException(TexlipsePlugin.stat("Errors during build. See the problems dialog."));
        }
    }

    /**
     * Kill the external program if it is running.
     */
    public void stop() {
        if (extrun != null) {
            extrun.stop();
        }
    }

    /**
     * Returns a special query string that indicates that this program is waiting an input from the user.
     * @return the query string to look for in the output of the program
     */
    protected String[] getQueryString() {
        return null;
    }
    
    /**
     * Create a layout warning marker to the given resource.
     *
     * @param resource the file where the problem occurred
     * @param message error message
     * @param lineNumber line number
     * @param markerType
     * @param severity Severity of the error
     */
    @SuppressWarnings("unchecked")
	protected static void createMarker(IResource resource, 
    		Integer lineNumber, String message, String markerType, int severity) {
    	int lineNr = -1;
    	if (lineNumber != null) {
    		lineNr = lineNumber;
    	}
    	IMarker marker = AbstractProgramRunner.findMarker(resource, lineNr, message, markerType);
    	if (marker == null) {
    		try {
    			HashMap map = new HashMap();
    			map.put(IMarker.MESSAGE, message);
    			map.put(IMarker.SEVERITY, new Integer (severity));

    			if (lineNumber != null)
    				map.put(IMarker.LINE_NUMBER, lineNumber);

    			MarkerUtilities.createMarker(resource, map, markerType);
    		} catch (CoreException e) {
    			throw new RuntimeException(e);
    		}
    	}
    }
    
    /**
     * Create a layout warning marker to the given resource.
     *
     * @param resource the file where the problem occured
     * @param message error message
     * @param lineNumber line number
     */
    public static void createLayoutMarker(IResource resource, Integer lineNumber, String message) {
        String markerType = TexlipseBuilder.LAYOUT_WARNING_TYPE;
        int severity = IMarker.SEVERITY_WARNING;
        createMarker(resource, lineNumber, message, markerType, severity);
    }
    
    /**
     * Create a marker to the given resource.
     * 
     * @param resource the file where the problem occured
     * @param message error message
     * @param lineNumber line number
     * @param severity severity of the marker
     */
    public static void createMarker(IResource resource, Integer lineNumber, String message, int severity) {
        String markerType = TexlipseBuilder.MARKER_TYPE;
        createMarker(resource, lineNumber, message, markerType, severity);
    }

    /**
     * Create a marker to the given resource. The marker's severity will be "ERROR".
     * 
     * @param resource the file where the problem occured
     * @param message error message
     * @param lineNumber line number
     */
    public static void createMarker(IResource resource, Integer lineNumber, String message) {
        createMarker(resource, lineNumber, message, IMarker.SEVERITY_ERROR);
    }
    
    /**
     * Checks pre-existance of marker.
     * 
     * @param resource Resource in which marker will searched
     * @param lineNr IMarker.LINE_NUMBER of the marker
     * @param message Message for marker
     * @param type The type of the marker to find
     * @return pre-existance of marker or null if no marker was found
     */
    public static IMarker findMarker(IResource resource, int lineNr, String message, String type) {
        
        try {
        	IMarker[] tasks = resource.findMarkers(type, true, IResource.DEPTH_ZERO);
            for (IMarker marker : tasks) {
            	Object lNrObj = marker.getAttribute(IMarker.LINE_NUMBER);
            	int lNr = -1;
            	if (lNrObj != null) {
            		lNr = ((Integer) lNrObj);
            	}
				if (lNr == lineNr
						&& marker.getAttribute(IMarker.MESSAGE).equals(message)) {
					return marker;
				}
					
			}
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
