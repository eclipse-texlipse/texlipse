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

import java.util.HashMap;

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author Oskar Ojala
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TexQuoteListener implements IDocumentListener {

    private ITextEditor editor;
    private IDocument document;
    
    private static HashMap quotes;
    
    public TexQuoteListener(ITextEditor editor) {
        this.editor = editor;
        this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (quotes == null) {
            quotes = new HashMap();
            quotes.put("eno", "``");
            quotes.put("enc", "''");
            quotes.put("fio", "''");
            quotes.put("fic", "''");
            quotes.put("fro", "<<");
            quotes.put("frc", ">>");
            quotes.put("deo", "``");
            quotes.put("dec", "''");
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentChanged(DocumentEvent event) {
        // if this is enabled
        if ("\"".equals(event.getText())) {
            ITextSelection textSelection = (ITextSelection) this.editor.getSelectionProvider().getSelection();            
            try {
                char prev = document.getChar(textSelection.getOffset() - 1);
                String replacement = "\"";
                // TODO null checks?
                IProject project = ((FileEditorInput)editor.getEditorInput()).getFile().getProject();
                String lang = TexlipseProperties.getProjectProperty(project, TexlipseProperties.LANGUAGE_PROPERTY);
                if (Character.isWhitespace(prev)) {
                    replacement = (String) quotes.get(lang + "o");
                } else if (Character.isLetterOrDigit(prev)) {
                    replacement = (String) quotes.get(lang + "c");
                } else {
                    return;
                }
                document.removeDocumentListener(this);
                document.replace(textSelection.getOffset(), 1, replacement);
                document.addDocumentListener(this);
                //editor.resetHighlightRange();
                //editor.setHighlightRange(textSelection.getOffset() + 1, 1, true);
                //editor.getSelectionProvider().setSelection(new TextSelection(textSelection.getOffset() + 3, 5));
            } catch (BadLocationException e) {}
        }
    }
}
