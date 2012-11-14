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

import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.spelling.SpellChecker;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Check the spelling of the currently edited document or clear spelling errors.
 *
 * @author Kimmo Karlsson
 */
public class SpellCheckHandler extends AbstractHandler {

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        ITextEditor textEditor = TexlipseHandlerUtil.getTextEditor(event);
        IEditorInput input = textEditor.getEditorInput();

        if (input instanceof FileEditorInput) {
            IFile file = ((FileEditorInput) input).getFile();
            SpellChecker.checkSpelling(textEditor.getDocumentProvider().getDocument(input), file);
        }
        return null;
    }

}
