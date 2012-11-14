/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions.editor;

import net.sourceforge.texlipse.actions.TexSelections;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.texparser.LatexWordCounter;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * A LaTeX cord counter. Counts only normal words, \cite's
 * as one word and the words in the mandatory argument of the
 * sectioning commands.
 */
public class WordCountHandler extends AbstractHandler {

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        ITextEditor textEditor = TexlipseHandlerUtil.getTextEditor(event);
        TexSelections selection = new TexSelections(textEditor);
        String selected = "";

        if (selection.getRawSelLength() > 0) {
            selected = selection.getSelection();
        }
        else {
            selected = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput()).get();
        }

        LatexWordCounter counter = new LatexWordCounter(selected);
        int size = counter.countWords();

        MessageDialog.openInformation(
                textEditor.getSite().getShell(),
                "Texlipse Plug-in",
                "Approximate words: " + size);
        return null;
    }

}
