/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.templates.ProjectTemplateManager;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * This action sets the currently open file as project's main file.
 * 
 * @author Kimmo Karlsson
 */
public class SaveAsTemplateAction implements IWorkbenchWindowActionDelegate, IEditorActionDelegate {
    
    // the window
    private IWorkbenchWindow window;
    
    // the editor
    private IEditorPart editor;
    
    /**
     * Creates a window for entering the template name, checks the user's input and
     * proceeds to save the template. 
     */
    public void run(IAction action) {
        
        IFile file = ((FileEditorInput)editor.getEditorInput()).getFile();
        String fullname = file.getFullPath().toString();
        
        // create dialog
        InputQueryDialog dialog = new InputQueryDialog(editor.getEditorSite().getShell(),
                TexlipsePlugin.getResourceString("templateSaveDialogTitle"),
                TexlipsePlugin.getResourceString("templateSaveDialogMessage").replaceAll("%s", fullname),
                file.getName().substring(0,file.getName().lastIndexOf('.')),
                new IInputValidator() {
            public String isValid(String newText) {
                if (newText != null && newText.length() > 0) {
                    return null; // no error
                }
                return TexlipsePlugin.getResourceString("templateSaveErrorFileName");
            }});
        
        if (dialog.open() == InputDialog.OK) {
            String newName = dialog.getInput();
            
            // check existing
            boolean reallySave = true;
            if (ProjectTemplateManager.templateExists(newName)) {
                reallySave = MessageDialog.openConfirm(editor.getSite().getShell(),
                        TexlipsePlugin.getResourceString("templateSaveOverwriteTitle"),
                        TexlipsePlugin.getResourceString("templateSaveOverwriteText").replaceAll("%s", newName));
            }
            
            if (reallySave) {
                ProjectTemplateManager.saveProjectTemplate(file, newName);
            }
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
        this.window = window;
    }
    
    /**
     * 
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        editor = targetEditor;
        action.setEnabled(editor instanceof ITextEditor);
    }
}
