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

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 * A Latex Nature containing a builder.
 * 
 * @author Kimmo Karlsson
 */
public class TexlipseNature implements IProjectNature {

    // Use fully qualified class name as the nature id.
    // This requires the plugin name to be the same as the main package name.
    public static final String NATURE_ID = TexlipseNature.class.getName();

    // the associated project
    private IProject project;

    /**
     * Configure the nature.
     * Called by IProject.setDescription().
     */
    public void configure() throws CoreException {
        addBuilder(TexlipseBuilder.BUILDER_ID);
    }

    /**
     * Deconfigure the nature.
     * Called by IProject.setDescription().
     */
    public void deconfigure() throws CoreException {
        removeBuilder(TexlipseBuilder.BUILDER_ID);
    }

    /**
     * @return the associated project
     */
    public IProject getProject() {
        return project;
    }

    /**
     * Set the associated project.
     * Called by IProject.setDescription().
     */
    public void setProject(IProject project) {
        this.project = project;
    }

    /**
     * Add a builder to the project.
     * 
     * @param id id of the builder to add
     * @throws CoreException
     */
    private void addBuilder(String id) throws CoreException {

        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();

        if (!hasBuilder(commands, id)) {

            ICommand command = desc.newCommand();
            command.setBuilderName(id);
            ICommand[] newCommands = new ICommand[commands.length + 1];

            System.arraycopy(commands, 0, newCommands, 1, commands.length);
            newCommands[0] = command;
            desc.setBuildSpec(newCommands);

            project.setDescription(desc, null);
        }
    }

    /**
     * Remove builder from the project.
     * 
     * @param id id of the builder to remove
     * @throws CoreException
     */
    private void removeBuilder(String id) throws CoreException
    {
        IProjectDescription desc = project.getDescription();
        ICommand[] commands = desc.getBuildSpec();

        if (hasBuilder(commands, id)) {

            ICommand[] newCommands = new ICommand[commands.length - 1];

            System.arraycopy(commands, 0, newCommands, 0, commands.length-1);
            desc.setBuildSpec(newCommands);

            project.setDescription(desc, null);
        }
    }
    
    /**
     * Search for a specific builder.
     * 
     * @param commands list of commands containing the builders
     * @param id id of the builder to search for
     * @return true, if the given builder was found
     */
    private boolean hasBuilder(ICommand[] commands, String id) {
        for (int i = 0; i < commands.length; i++) {
            if (commands[i].getBuilderName().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
