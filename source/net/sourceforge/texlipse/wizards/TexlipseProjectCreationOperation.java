/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.TexlipseNature;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.templates.ProjectTemplateManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.ide.IDE;


/**
 * Operation to add latex nature to the project.
 * The main file is also created here, using the appropriate template.
 * 
 * @author Kimmo Karlsson
 */
public class TexlipseProjectCreationOperation implements IRunnableWithProgress {

    // a reference to the page attributes
    private TexlipseProjectAttributes attributes;

    /**
     * Create a new operation.
     * @param attr Project attributes
     */
    public TexlipseProjectCreationOperation(TexlipseProjectAttributes attr) {
        attributes = attr;
    }

    /**
     * Run the project creation task. This method is invoked by the Eclipse IDE after the 
     * Project Creation Wizard is finished.
     * 
     * @param pMon progress monitor
     * @throws InvocationTargetException
     * @throws InterruptedException
     */
    public void run(IProgressMonitor pMon)
           throws InvocationTargetException, InterruptedException {

        IProgressMonitor monitor = pMon;
        // this null-check is recommended by developers of other plugins 
        if (pMon == null) {
            monitor = new NullProgressMonitor();
        }
        
        try {

            monitor.beginTask(TexlipsePlugin.getResourceString("projectWizardProgressCreating"), 12);

            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            String name = attributes.getProjectName();
            IProject project = root.getProject(name);
            monitor.worked(1);

            createProject(project, monitor);
            monitor.worked(1);
            addProjectNature(project, monitor);
            monitor.worked(1);
            createProjectDirs(project, monitor);
            monitor.worked(1);
            
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.LANGUAGE_PROPERTY, attributes.getLanguageCode()+"");
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.MARK_OUTPUT_DERIVED_PROPERTY, "true");
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.MARK_TEMP_DERIVED_PROPERTY, "true");
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.MAKEINDEX_STYLEFILE_PROPERTY, "");
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.BIBREF_DIR_PROPERTY, "");
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUTFILE_PROPERTY, attributes.getOutputFile());
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT, attributes.getOutputFormat());
            TexlipseProperties.setProjectProperty(project, TexlipseProperties.BUILDER_NUMBER, attributes.getBuilder()+"");
            TexlipsePlugin.getDefault().getPreferenceStore().setValue(TexlipseProperties.OUTPUT_FORMAT, attributes.getOutputFormat());
            TexlipsePlugin.getDefault().getPreferenceStore().setValue(TexlipseProperties.BUILDER_NUMBER, attributes.getBuilder());
            monitor.worked(1);
            
            createMainFile(project, monitor);
            monitor.worked(1);
            
            monitor.subTask(TexlipsePlugin.getResourceString("projectWizardProgressSettingsFile"));
            TexlipseProperties.saveProjectProperties(project);
            monitor.worked(1);
            
            IDE.openEditor(TexlipsePlugin.getCurrentWorkbenchPage(),
                    TexlipseProperties.getProjectSourceFile(project));
            
            monitor.worked(1);
            
        } catch (CoreException e) {
            TexlipsePlugin.log(TexlipsePlugin.getResourceString("projectWizardCreateError"), e);
        } finally {
            monitor.done();
        }
    }

    /**
     * Create the project directory.
     * If the user has specified an external project location,
     * the project is created with a custom description for the location.
     * 
     * @param project project
     * @param monitor progress monitor
     * @throws CoreException
     */
    private void createProject(IProject project, IProgressMonitor monitor)
            throws CoreException {

        monitor.subTask(TexlipsePlugin.getResourceString("projectWizardProgressDirectory"));
        
        if (!project.exists()) {
            if (attributes.getProjectLocation() != null) {
                IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
                IPath projectPath = new Path(attributes.getProjectLocation());
                IStatus stat = ResourcesPlugin.getWorkspace().validateProjectLocation(project, projectPath);
                if (stat.getSeverity() != IStatus.OK) {
                    // should not happen. the location should have been checked in the wizard page
                    throw new CoreException(stat);
                }
                desc.setLocation(projectPath);
                project.create(desc, monitor);
            } else {
                project.create(monitor);
            }
        }
        if (!project.isOpen()) {
            project.open(monitor);
        }
    }

    /**
     * Add a nature to the project.
     * 
     * @param project project
     * @param monitor progress monitor
     * @throws CoreException
     */
    public static void addProjectNature(IProject project, IProgressMonitor monitor)
            throws CoreException {

        monitor.subTask(TexlipsePlugin.getResourceString("projectWizardProgressNature"));

        IProjectDescription desc = project.getDescription();
        String[] natures = desc.getNatureIds();
        for (int i = 0; i < natures.length; i++) {
            // don't add if already there
            if (TexlipseNature.NATURE_ID.equals(natures[i])) {
                return;
            }
        }
        
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy(natures, 0, newNatures, 1, natures.length);
        newNatures[0] = TexlipseNature.NATURE_ID;
        desc.setNatureIds(newNatures);
        project.setDescription(desc, monitor);
    }
    
    /**
     * Create project's (sub)directory structure.
     *  
     * @param project project
     * @param monitor progress monitor
     * @throws CoreException
     */
    private void createProjectDirs(IProject project, IProgressMonitor monitor)
            throws CoreException {

        monitor.subTask(TexlipsePlugin.getResourceString("projectWizardProgressSubdirs"));
        
        String outputDir = attributes.getOutputDir();
        String sourceDir = attributes.getSourceDir();
        String tempDir = attributes.getTempDir();

        createDir(project, monitor, outputDir, true);
        createDir(project, monitor, sourceDir, false);
        createDir(project, monitor, tempDir, true);

        TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUT_DIR_PROPERTY, outputDir);
        TexlipseProperties.setProjectProperty(project, TexlipseProperties.SOURCE_DIR_PROPERTY, sourceDir);
        TexlipseProperties.setProjectProperty(project, TexlipseProperties.TEMP_DIR_PROPERTY, tempDir);
    }
    
    /**
     * Create a subdirectory to the given project's directory.
     * @param project project
     * @param monitor progress monitor
     * @param dir directory name
     * @throws CoreException
     */
    private void createDir(IProject project, IProgressMonitor monitor, String dir,
            boolean derivedAsDefault) throws CoreException {
        if (dir != null && dir.length() > 0) {
            IFolder folder = project.getFolder(dir);
            folder.create(true, true, monitor);
            if (derivedAsDefault) {
                folder.setDerived(true);
            }
        }
    }
    
    /**
     * Create main file of the project from template.
     * 
     * @param project project
     * @param monitor progress monitor
     * @throws CoreException
     */
    private void createMainFile(IProject project, IProgressMonitor monitor)
            throws CoreException {

        monitor.subTask(TexlipsePlugin.getResourceString("projectWizardProgressFile"));

        String name = attributes.getSourceFile();
        if (name == null || name.length() == 0) {
            throw new CoreException(TexlipsePlugin.stat("Null main file name"));
        }

        TexlipseProperties.setProjectProperty(project, TexlipseProperties.MAINFILE_PROPERTY, name);
        
        byte[] template = getTemplate(attributes.getTemplate());
        if (template == null) {
            template = new byte[0];
        }
        ByteArrayInputStream stream = new ByteArrayInputStream(template);
        
        IFile mainFile = TexlipseProperties.getProjectSourceFile(project);
        mainFile.create(stream, true, monitor);
    }
    
    /**
     * Return the contents of a template.
     * 
     * @param name template name without the extension
     * @return contents of the template
     */
    private byte[] getTemplate(String name) {

        String[] userNames = ProjectTemplateManager.loadUserTemplateNames();
        for (int i = 0; i < userNames.length; i++) {
        
            if (userNames[i].equals(name)) {
                
                byte[] content = null;
                try {
                    content = ProjectTemplateManager.readUserTemplate(name);
                } catch (IOException e) {
                }
                if (content != null) {
                    return content;
                }
            }
        }
        
        String[] systemNames = ProjectTemplateManager.loadTemplateNames();
        for (int i = 0; i < systemNames.length; i++) {
            
            if (systemNames[i].equals(name)) {
                
                byte[] content = null;
                try {
                    content = ProjectTemplateManager.readSystemTemplate(name);
                } catch (IOException e) {
                }
                if (content != null) {
                    return content;
                }
            }
        }
        
        return null;
    }
}
