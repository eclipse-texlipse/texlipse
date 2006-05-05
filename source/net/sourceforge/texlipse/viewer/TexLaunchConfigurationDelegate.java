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
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.preference.IPreferenceStore;


/**
 * Launch a document viewer program using the given launch configuration.
 * 
 * @author Kimmo Karlsson
 * @author Tor Arne Vestbø
 */
public class TexLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	 public static final String CONFIGURATION_ID = "net.sourceforge.texlipse.viewer.launchConfigurationType";
	
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
     * Launches the viewer specified in the  <code>configuration</code> argument
     */
    public void launch(ILaunchConfiguration configuration, String mode,
            ILaunch launch, IProgressMonitor monitor) throws CoreException {

        Map regMap = configuration.getAttributes();
        ViewerAttributeRegistry registry = new ViewerAttributeRegistry();
        registry.setValues(regMap);
        
        Map addEnv = configuration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap());
        
        monitor.beginTask("Viewing document", 100);     
        Process process = ViewerManager.preview(registry, addEnv, monitor);
       
        // if this process is added to launcer, the output will not be parsed correctly
                //launch.addProcess(DebugPlugin.newProcess(launch, process, mode));
        
        // Return focus to Eclipse after previewing (optional)
        IPreferenceStore prefs = TexlipsePlugin.getDefault().getPreferenceStore();
        if (prefs.getBoolean(TexlipseProperties.BUILDER_RETURN_FOCUS)) {
            
            try {
                Thread.sleep(500); // A small delay required
            } catch (InterruptedException e) {
                // swallow
            }
            
            ViewerManager.returnFocusToEclipse(false);
        }
        
    }

    /* 
     * @see LaunchConfigurationDelegate#getProjectsForProblemSearch(ILaunchConfiguration, String)
     */
    protected IProject[] getProjectsForProblemSearch(ILaunchConfiguration configuration, String mode) throws CoreException {
        return new IProject[] { TexlipsePlugin.getCurrentProject() };
    }
    
    
    
    
}
