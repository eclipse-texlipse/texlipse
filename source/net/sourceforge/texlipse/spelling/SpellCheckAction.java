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

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Check the spelling of the currently edited document.
 * 
 * @author Kimmo Karlsson
 */
public class SpellCheckAction implements IEditorActionDelegate {
    
	private IEditorPart targetEditor;

    /**
     * Run the spell check action.
     * @param action the action
	 */
	public void run(IAction action) {
        
        if (targetEditor == null) {
            return;
        }
        if (!(targetEditor instanceof ITextEditor)) {
            return;
        }
        
        ITextEditor textEditor = (ITextEditor) targetEditor;
        IEditorInput input = textEditor.getEditorInput();
        
        IFile file = ((FileEditorInput) input).getFile();
        
        SpellChecker.checkSpelling(textEditor.getDocumentProvider().getDocument(input), file);
	}

	/**
     * Change the active selection.
     * @param action the action
     * @param selection the selection on the currently edited document
	 */
	public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof TextSelection) {
            action.setEnabled(true);
            return;
        }
        action.setEnabled(targetEditor instanceof ITextEditor);
	}

    /**
     * Change the active editor.
     * @param action the action
     * @param targetEditor the new active editor
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        this.targetEditor = targetEditor;
    }
}
