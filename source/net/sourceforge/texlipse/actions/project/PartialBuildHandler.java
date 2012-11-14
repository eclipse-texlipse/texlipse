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

import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;


/**
 * This action enables the partial building mode
 * on the current project.
 *
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 * @author Matthias Erll
 */
public class PartialBuildHandler extends AbstractHandler
    implements IElementUpdater {

    /** Command ID string. */
    private static final String COMMAND_ID_STR = "net.sourceforge.texlipse.commands.partialBuild";

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        Command command = event.getCommand();
        boolean oldState = HandlerUtil.toggleCommandState(command);
        String value = oldState ? null : "true";

        IProject project = TexlipseHandlerUtil.getProject(event);
        TexlipseProperties.setProjectProperty(project, TexlipseProperties.PARTIAL_BUILD_PROPERTY, value);
        if (value == null) {
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, null);
            // delete all tempPartial0000 files
            try {
                IResource[] res;
                IFolder projectOutputDir = TexlipseProperties.getProjectOutputDir(project);
                if (projectOutputDir != null)
                    res = projectOutputDir.members();
                else
                    res = project.members();
                for (int i = 0; i < res.length; i++) {
                    if (res[i].getName().startsWith("tempPartial0000"))
                        res[i].delete(true, null);
                }

                IFolder projectTempDir = TexlipseProperties.getProjectTempDir(project);
                if (projectTempDir != null && projectTempDir.exists())
                    res = projectTempDir.members();
                else
                    res = project.members();

                for (int i = 0; i < res.length; i++) {
                    if (res[i].getName().startsWith("tempPartial0000"))
                        res[i].delete(true, null);
                }
                IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
                res = sourceDir.members();
                for (int i = 0; i < res.length; i++) {
                    if (res[i].getName().startsWith("tempPartial0000"))
                        res[i].delete(true, null);
                }

            }
            catch (CoreException e) {
                TexlipsePlugin.log("Error while deleting temp files", e);
            }
        }

        return null;
    }

    @Override
    public final void setEnabled(final Object evaluationContext) {
        super.setEnabled(evaluationContext);
        IProject project = TexlipseHandlerUtil.getProject(evaluationContext);
        if (project != null) {
            TexlipseHandlerUtil.setStateChecked(COMMAND_ID_STR,
                    TexlipseProperties.getProjectProperty(project, TexlipseProperties.PARTIAL_BUILD_PROPERTY) != null);
        }
        else {
            TexlipseHandlerUtil.setStateChecked(COMMAND_ID_STR,
                    false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public final void updateElement(final UIElement element, final Map parameters) {
        boolean checked = TexlipseHandlerUtil.isStateChecked(COMMAND_ID_STR);
        element.setChecked(checked);
    }

}
