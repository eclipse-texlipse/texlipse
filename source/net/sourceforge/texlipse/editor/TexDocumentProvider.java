/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.TexlipseNature;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.wizards.TexlipseProjectCreationOperation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.editors.text.ForwardingDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;


/**
 * Simple document provider implementation that
 * adds latex nature to the project holding a newly created file.
 * This way we also get latex builder and properties to any project
 * that contains latex files.
 * 
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 */
public class TexDocumentProvider extends TextFileDocumentProvider {

    /**
     * 
     */
    public TexDocumentProvider() {
        IDocumentProvider provider= new TextFileDocumentProvider();
        provider= new ForwardingDocumentProvider(TexEditor.TEX_PARTITIONING, 
                new TexDocumentSetupParticipant(), provider);
        setParentDocumentProvider(provider);    
    }
    
    /**
     * Does the same as super.createDocument(Object), except this also adds
     * latex nature to the project containing the given document.
     */
    public IDocument getDocument(Object element) {
        IDocument doc = super.getDocument(element);
        
        // add latex nature to project holding this latex file
        // this way we also get latex builder to any project that has latex files
        if (element instanceof FileEditorInput) {
            IFile file = (IFile) ((FileEditorInput)element).getAdapter(IFile.class);
            if (file != null) {
                
                IProject project = file.getProject();
                try {
                    if (!project.hasNature(TexlipseNature.NATURE_ID)) {
                        TexlipseProjectCreationOperation.addProjectNature(project, new NullProgressMonitor());
                    } else if (TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT) == null) {
                        // this is needed for imported projects
                        TexlipseNature n = new TexlipseNature();
                        // the nature is not added, just configured
                        n.setProject(project);
                        // this will cause the builder to be added, if not already there
                        n.configure();
                    }
                } catch (CoreException e) {
                    return doc;
                }
                
                // output format might not yet be set
                String format = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT);
                if (format == null || format.length() == 0) {
                    TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT, TexlipsePlugin.getPreference(TexlipseProperties.OUTPUT_FORMAT));
                    TexlipseProperties.setProjectProperty(project, TexlipseProperties.BUILDER_NUMBER, TexlipsePlugin.getPreference(TexlipseProperties.BUILDER_NUMBER));
                    TexlipseProperties.setProjectProperty(project, TexlipseProperties.MARK_TEMP_DERIVED_PROPERTY, "true");
                    TexlipseProperties.setProjectProperty(project, TexlipseProperties.MARK_OUTPUT_DERIVED_PROPERTY, "true");
                    
                    String name = file.getName();
                    TexlipseProperties.setProjectProperty(project, TexlipseProperties.MAINFILE_PROPERTY, name);
                    String output = name.substring(0, name.lastIndexOf('.')+1) + TexlipsePlugin.getPreference(TexlipseProperties.OUTPUT_FORMAT);
                    TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUTFILE_PROPERTY, output);
                    
                    IPath path = file.getFullPath();
                    String dir = path.removeFirstSegments(1).removeLastSegments(1).toString();
                    if (dir.length() > 0) {
                        TexlipseProperties.setProjectProperty(project, TexlipseProperties.SOURCE_DIR_PROPERTY, dir);
                        TexlipseProperties.setProjectProperty(project, TexlipseProperties.OUTPUT_DIR_PROPERTY, dir);
                        TexlipseProperties.setProjectProperty(project, TexlipseProperties.TEMP_DIR_PROPERTY, dir);
                    }
                }
            }
        }
        
        return doc;
    }
}
