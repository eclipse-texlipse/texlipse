/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.builder.BuilderRegistry;
import org.eclipse.texlipse.properties.TexlipseProperties;
import org.eclipse.texlipse.viewer.TexLaunchConfigurationDelegate;
import org.eclipse.texlipse.viewer.TexLaunchConfigurationTab;
import org.eclipse.texlipse.viewer.ViewerAttributeRegistry;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;


/**
 * Preview action runs the previewer (that is configured in preferences)
 * on the current project.
 * 
 * @author Kimmo Karlsson
 * @author Tor Arne Vestb�
 */
public class PreviewAction implements IWorkbenchWindowActionDelegate, IEditorActionDelegate {
    
	/**
     *  Launches either the most recent viewer configuration, or if there
     *  is no previous viewers, creates a new viewer launch configuration.
	 */
	public void run(IAction action) {
        try {      	
        	ILaunchConfiguration config = null;
        	
            
            // Get the output format
            IProject project = TexlipsePlugin.getCurrentProject();
            String outputFormat = TexlipseProperties.getProjectProperty(project,
                TexlipseProperties.OUTPUT_FORMAT);
            
            // Get the preferred viewer for the current output format
            ViewerAttributeRegistry var = new ViewerAttributeRegistry();
            String preferredViewer = var.getPreferredViewer(outputFormat);

            if (null == preferredViewer) {
                BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorOutputFormatNotSet").replaceAll("%s", project.getName()));
                throw new CoreException(TexlipsePlugin.stat("No previewer found for the current output format."));
            }
            
            ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
            ILaunchConfigurationType type = manager.getLaunchConfigurationType(
                    TexLaunchConfigurationDelegate.CONFIGURATION_ID);               
            ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
            if (configs != null) {
                // Try to find a recent viewer launch first
                for (ILaunchConfiguration c : configs) {
                    if (c.getType().getIdentifier().equals(TexLaunchConfigurationDelegate.CONFIGURATION_ID)) {
                        if (c.getAttribute("viewerCurrent", "").equals(preferredViewer)) {
                            config = c;
                            break;
                        }
                    }                
                }
            }
            
            // If there was no available viewer
            if (config == null)
            {
                // Create a new one
                ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null,
                        manager.generateUniqueLaunchConfigurationNameFrom("Preview Document in " + preferredViewer));
                
                // Request another viewer than the topmost in the preferences
                workingCopy.setAttribute("viewerCurrent", preferredViewer);
                
                // For some reason the Eclipse API wants us to init default values
                // for new configurations using the ConfigurationTab dialog.
                TexLaunchConfigurationTab tab = new TexLaunchConfigurationTab();                
                tab.setDefaults(workingCopy);     
                
                config = workingCopy.doSave();
            }
            
            DebugUITools.launch(config, ILaunchManager.RUN_MODE);        
        	
        	
        } catch (CoreException e) {
            TexlipsePlugin.log("Launching viewer", e);
        }
	}

	/**
     * Nothing to do.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
     * Nothing to do.
	 */
	public void dispose() {
	}

	/**
	 * Cache the window object in order to be able to provide
     * UI components for the action.
	 */
	public void init(IWorkbenchWindow window) {
	}

    /**
     * 
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    }
}
