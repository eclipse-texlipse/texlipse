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

import net.sourceforge.texlipse.texparser.LatexWordCounter;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * A LaTeX cord counter. Counts only normal words, \cite's
 * as one word and the words in the mandatory argument of the
 * sectioning commands.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class WordCountAction implements IWorkbenchWindowActionDelegate {
    
    private IWorkbenchWindow window;
    private static TexSelections selection;
    
    /**
     * Constructs a new word counter.
     */
    public WordCountAction() {
    }
    
    /**
     * The action has been activated. The argument of the
     * method represents the 'real' action sitting
     * in the workbench UI.
     * @see IWorkbenchWindowActionDelegate#run
     */
    public void run(IAction action) {
        ITextEditor textEditor = (ITextEditor)this.window.getActivePage().getActiveEditor();
        if (textEditor == null)
            return;
        selection = new TexSelections(textEditor);
        String selected = "";
        
        if (selection.getRawSelLength() > 0) {
            selected = selection.getSelection();
        } else {
            selected = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()).get();
        }
        
        LatexWordCounter counter = new LatexWordCounter(selected);
        int size = counter.countWords();
        
        MessageDialog.openInformation(
                window.getShell(),
                "Texlipse Plug-in",
                "Approximate words: " + size);
    }
    
    /**
     * Selection in the workbench has been changed. We 
     * can change the state of the 'real' action here
     * if we want, but this can only happen after 
     * the delegate has been created.
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }
    
    /**
     * We can use this method to dispose of any system
     * resources we previously allocated.
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose() {
    }
    
    /**
     * We will cache window object in order to
     * be able to provide parent shell for the message dialog.
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window) {
        this.window = window;
    }
}