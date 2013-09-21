/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions.project;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.builder.BuilderRegistry;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.viewer.TexLaunchConfigurationDelegate;
import net.sourceforge.texlipse.viewer.TexLaunchConfigurationTab;
import net.sourceforge.texlipse.viewer.ViewerAttributeRegistry;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;


/**
 * Preview action runs the previewer (that is configured in preferences)
 * on the current project.
 *
 * @author Kimmo Karlsson
 * @author Tor Arne Vestbø
 */
public class PreviewHandler extends AbstractHandler {

    /**
     * Launches either the most recent viewer configuration, or if there
     * is no previous viewers, creates a new viewer launch configuration.
     *
     * @param event execution event
     * @return null
     * @exception ExecutionException if the current editor cannot be retrieved
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        try {
            ILaunchConfiguration config = null;

            // Get the output format
            IProject project = TexlipseHandlerUtil.getProject(event);
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
            if (config == null) {
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

        }
        catch (CoreException e) {
            TexlipsePlugin.log("Launching viewer", e);
        }
        return null;
    }

}
