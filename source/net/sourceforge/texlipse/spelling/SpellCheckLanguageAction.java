/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.spelling;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.InputQueryDialog;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;


/**
 * Change the language of the spell-checker.
 * 
 * @author Kimmo Karlsson
 */
public class SpellCheckLanguageAction implements IEditorActionDelegate {
    
    // cached reference to the workbench window
    private IWorkbenchWindow window;

    /**
     * Run the spell check action.
     * @param action the action
	 */
	public void run(IAction action) {
        InputQueryDialog dialog = new InputQueryDialog(window.getShell(),
                TexlipsePlugin.getResourceString("spellCheckerLanguageDialogTitle"),
                TexlipsePlugin.getResourceString("spellCheckerLanguageChoose"), 
                TexlipsePlugin.getPreference(SpellChecker.SPELL_CHECKER_LANGUAGE), null);
        
        int code = dialog.open();
        if (code == InputQueryDialog.OK) {
            SpellChecker.setLanguage(dialog.getInput());
        }
	}

	/**
     * Change the active selection.
     * @param action the action
     * @param selection the selection on the currently edited document
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

    /**
     * 
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        if (targetEditor != null) {
            IWorkbenchPartSite site = targetEditor.getSite();
            if (site != null) {
                window = site.getWorkbenchWindow();
            }
        }
    }
}
