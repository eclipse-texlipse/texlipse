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

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;


/**
 * Launch a document viewer program using the given launch configuration.
 * 
 * @author Kimmo Karlsson
 */
public class TexLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

    /**
     * Constructor, does nothing.
     */
    public TexLaunchConfigurationDelegate() {
    }
    
    /**
     * @return list of projects which should be built before viewing
     */
    protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
        IProject project = TexlipsePlugin.getCurrentProject();
        if (project == null) {
            return null;
        }
        return new IProject[] { project };
    }

    /**
     * Launch the viewer.
     */
    public void launch(ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException {

        Map regMap = configuration.getAttributes();
        ViewerAttributeRegistry registry = new ViewerAttributeRegistry();
        registry.setValues(regMap);
        
        Map addEnv = configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap());
        
        Process process = ViewerManager.preview(registry, addEnv);

        // if this process is added to launcer, the output will not be parsed correctly
        //launch.addProcess(DebugPlugin.newProcess(launch, process, mode));
    }
}
