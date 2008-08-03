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

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;

import net.sourceforge.texlipse.builder.Kpath;

/**
 * Access functionality of the kpathsea library via kpsewhich
 * (Easier than implementing a JNI interface, if more clumsy).
 * The ProgramRunner interface isn't a perfect match because
 * kpathsea doesn't have input and output filetypes etc, but
 * this lets us integrate with other parts of TeXlipse with
 * minimal effort.
 * 
 * @author Christopher Hoskin
 *
 */
public class KpsewhichRunner implements ProgramRunner {

    // the currently running program
    private ExternalProgram extrun;
    
    public KpsewhichRunner() {
        extrun = new ExternalProgram();
    }
	


	public String getDescription() {
		return "Kpsewhich program";
	}

	public String getInputFormat() {
		// Kpsewhich doesn't have an input format
		return null;
	}

	public String getOutputFormat() {
		// Kpsewhich doesn't have an output format
		return null;
	}

	public String getProgramArguments() {
		// Not really applicable to us
		return "(Not applicable)";
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

    /**
     * @return the program path and filename from the preferences
     */
    public String getProgramPath() {
        return TexlipsePlugin.getPreference(getCommandPreferenceName());
    }

	public void initializeDefaults(IPreferenceStore pref, String path) {
        pref.setDefault(getCommandPreferenceName(), path);
        //Not sure default argument makes sense for kpsewhich
        //pref.setDefault(getArgumentsPreferenceName(), getDefaultArguments());
	}

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

	public void run(IResource resource) throws CoreException {
		// This method isn't really applicable for kpsewhich
		
	}

	public void setProgramArguments(String args) {
		// This method isn't really applicable for kpsewhich
		
	}

    /**
     * @param path the program path and filename for the preferences
     */
    public void setProgramPath(String path) {
        TexlipsePlugin.getDefault().getPreferenceStore().setValue(getCommandPreferenceName(), path);
    }

	public void stop() {
        if (extrun != null) {
            extrun.stop();
        }
	}
	
    /**
     * @return the name of the program runner path -preference in the plugin preferences
     */
    private String getCommandPreferenceName() {
        return getClass() + "_prog";
    }
    
    protected String getWindowsProgramName() {
        return "kpsewhich.exe";
    }
    
    protected String getUnixProgramName() {
        return "kpsewhich";
    }
    
    /**
     * 
     * @param command The command string to execute
     * @param resource The path to run the command in
     * @return Output from the command
     * @throws CoreException Thrown if running the external command generates an exception
     */
	protected String run(String[] command, IResource resource) throws CoreException {
	    // check if we are using console
        String console = null;
        if (TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.BUILDER_CONSOLE_OUTPUT)) {
            console = getProgramName();
        }
        
		extrun.setup(command, resource.getLocation().toFile().getParentFile(), console);
		
        String output = null;
        try {
                output = extrun.run();
            
        } catch (Exception e) {
            throw new CoreException(new Status(IStatus.ERROR, TexlipsePlugin.getPluginId(),
                    IStatus.ERROR, "Building the project: ", e));
        } finally {
            extrun.stop();
        }
        
        return output;
		
	}
	
	/**
	 * Get the path to a file
	 * 
	 * @param resource folder to run kpsewhich in
	 * @param filename Name of the file to find
	 * @param progname Name of the calling program (path searched may depend on this)
	 * @return the path to the file or an empty string if no path was found
	 */
	public String getFile(IResource resource, String filename, String progname) throws CoreException {
	    
        String[] command = {getProgramPath(),"-progname="+progname, filename};
        
		String output = run(command, resource);
		       
        String[] outList = output.split("\r\n|\r|\n");
		return outList[0];
	}
	
	/**
	 * Gets the paths Kpathsea will search for a particular type of file
	 * @param resource Directory to run kpsewhich in
	 * @param ext The extension to search for
	 * @return An array of Kpath objects, representing the search paths.
	 * @throws CoreException Thrown if running kpsewhich throws an exception
	 */
	public Kpath[] getSearchPaths(IResource resource, String ext) throws CoreException {
		String[] command = {getProgramPath(), "-show-path", ext};
		String output = run(command, resource);
	    
		if (output.startsWith("warning: kpsewhich: Ignoring unknown file type")) {
			return null;
		} else {
			String[] outList = output.split(java.io.File.pathSeparator+"|\r\n|\r|\n");
			Kpath[] kpaths = new Kpath[outList.length];
			for(int i=0; i<outList.length;i++) {
				String unpack = outList[i];
				kpaths[i] = new Kpath(unpack);
			}
			return kpaths;
		}
	}

}
	  	 
