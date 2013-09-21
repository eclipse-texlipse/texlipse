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
import net.sourceforge.texlipse.actions.InputQueryDialog;
import net.sourceforge.texlipse.templates.ProjectTemplateManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;


/**
 * This handler saves the currently open file as a project template file.
 *
 * @author Kimmo Karlsson
 */
public class SaveAsTemplateHandler extends AbstractHandler {

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
        IFile file = ((FileEditorInput) editor.getEditorInput()).getFile();
        String fullname = file.getFullPath().toString();

        // create dialog
        InputQueryDialog dialog = new InputQueryDialog(editor.getEditorSite().getShell(),
                TexlipsePlugin.getResourceString("templateSaveDialogTitle"),
                TexlipsePlugin.getResourceString("templateSaveDialogMessage").replaceAll("%s", fullname),
                file.getName().substring(0, file.getName().lastIndexOf('.')),
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
        return null;
    }

}
