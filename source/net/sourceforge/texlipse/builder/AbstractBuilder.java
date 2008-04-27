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
 * Generic builder.
 * 
 * @author Kimmo Karlsson
 */
public abstract class AbstractBuilder implements Runnable, Builder {

    // the current progress monitor
    protected IProgressMonitor monitor;
    
    // true, when the build process is running
    protected volatile boolean buildRunning;

    // builder id in the builder registry
    protected int id;

    /**
     * Create a new builder.
     * @param mon
     */
    protected AbstractBuilder(int i) {
        id = i;
        monitor = null;
        buildRunning = false;
    }
    
    /**
     * @return id number
     */
    public int getId() {
        return id;
    }
    
    /**
     * Check to see if this builder is valid.
     * @return true, if this builder is ready for operation
     */
    public abstract boolean isValid();
    
    /**
     * Resets the builder to be ready for a new build.
     */
    public void reset(final IProgressMonitor mon) {
        monitor = mon;
        buildRunning = false;
    }
    
    /**
     * @return the name of the format this builder outputs
     */
    public abstract String getOutputFormat();
    
    public abstract String getSequence();
    
    /**
     * Run the build monitor. If the user interrupts the build, stop the execution.
     */
    public void run() {
        while(buildRunning) {
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
            }
            if (monitor.isCanceled()) {
                stopBuild();
            }
        }
    }

    /**
     * Stops the execution of the external programs.
     */
    public abstract void stopRunners();
    
    /**
     * Stops the execution of the building process.
     */
    public void stopBuild() {
        buildRunning = false;
        stopRunners();
    }
    
    /**
     * The main build method. This runs latex program once at the given directory.
     * @throws CoreException
     */
    public abstract void buildResource(IResource resource) throws CoreException;
    
    /**
     * The main method.
     * 
     * @param resource the input file to compile
     * @throws CoreException if the build fails
     */
    public void build(IResource resource) throws CoreException {
        
        if (monitor == null) {
            throw new IllegalStateException();
        }
        
        buildRunning = true;
        Thread buildThread = new Thread(this);
        buildThread.start();
        
        try {
            buildResource(resource);
        } finally {
        	   
	        buildRunning = false;
	        
	        try {
	            buildThread.join();
	        } catch (InterruptedException e) {
	            Thread.interrupted();
	            monitor.setCanceled(true);
	            stopBuild();
	        }
        }
    }
}
