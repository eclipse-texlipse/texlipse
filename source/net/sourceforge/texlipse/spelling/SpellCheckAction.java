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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
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
        
        // read encoding from the file
        if (input instanceof FileEditorInput) {
            try {
                String enc = ((FileEditorInput) input).getFile().getCharset();
                SpellChecker.setEncoding(enc);
            } catch (CoreException e) {
            }
        }
        
        SpellChecker.checkSpelling(textEditor.getDocumentProvider().getDocument(input));
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

	/**
	 * Cache the window object in order to be able to provide
     * UI components for the action.
     * @param window the workbench window
	 */
	public void init(IWorkbenchWindow window) {
	}

    /**
     * Dispose the action. Nothing to do.
     */
    public void dispose() {
    }
}
