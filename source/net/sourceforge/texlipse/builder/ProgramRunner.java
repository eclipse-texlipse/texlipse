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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * An abstraction of an external program.
 * 
 * @author Kimmo Karlsson
 */
public interface ProgramRunner {
    
    /**
     * @return the name of the executable program
     */
    public String getProgramName();

    /**
     * initialize default preferences
     */
    public void initializeDefaults(IPreferenceStore pref, String path);
    
    public String getProgramPath();
    public void setProgramPath(String path);
    public String getProgramArguments();
    public void setProgramArguments(String args);

    /**
     * @return a human-readable description of the this program
     */
    public String getDescription();

    /**
     * Check to see if this program is ready for operation.
     * @return true if this program exists
     */
    public boolean isValid();
    
    /**
     * The main method.
     * 
     * @param resource the input file to feed to the external program
     * @throws CoreException if the external program is not found
     *                       or if there was an error during the build
     */
    public void run(IResource resource) throws CoreException;

    /**
     * Kill the external program if it is running.
     */
    public void stop();

    /**
     * Returns the input file format of this program. The formats are
     * specified in TexlipseProperties class as OUTPUT_FORMAT_* -named fields.
     *  
     * @return the input file format
     */
    public String getInputFormat();

    /**
     * @return the output file format of this program
     */
    public String getOutputFormat();
    
}