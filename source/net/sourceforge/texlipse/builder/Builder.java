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
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Builder interface for document builders.
 * 
 * @author Kimmo Karlsson
 */
public interface Builder {
    /**
     * Check to see if this builder is valid.
     * @return true, if this builder is ready for operation
     */
    public boolean isValid();

    /**
     * @return id number for the registry
     */
    public int getId();
    
    /**
     * Resets the builder to be ready for a new build.
     */
    public void reset(final IProgressMonitor mon);

    /**
     * @return the name of the format this builder outputs
     */
    public String getOutputFormat();

    /**
     * @return the build sequence for the user interface, e.g. "latex+dvips+ps2pdf"
     */
    public String getSequence();
    
    /**
     * Stops the execution of the whole building process.
     */
    public void stopBuild();

    /**
     * Starts the exceution of the whole build process.
     * 
     * @param resource the input file to build
     * @throws CoreException if the build fails
     */
    public void build(IResource resource) throws CoreException;

    /**
     * Starts the execution of the program runner sequence, but not the progress monitor listener.
     * @param resource the project main file
     */
    public void buildResource(IResource resource) throws CoreException;

    /**
     * Stops the execution of the program runners, but not the progress monitor listener.
     */
    public void stopRunners();
}
