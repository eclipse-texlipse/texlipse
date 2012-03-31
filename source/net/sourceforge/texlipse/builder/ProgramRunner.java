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


/**
 * An abstraction of an external program.
 * 
 * @author Kimmo Karlsson
 */
public interface ProgramRunner {

    public String getId();
    public String getProgramName();
    public String getProgramPath();
    public String getProgramArguments();

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

}