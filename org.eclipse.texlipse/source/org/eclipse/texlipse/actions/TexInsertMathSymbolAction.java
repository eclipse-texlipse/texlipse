/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.editor.TexCompletionProposal;
import org.eclipse.texlipse.editor.TexEditor;
import org.eclipse.texlipse.model.TexCommandEntry;
import org.eclipse.ui.IEditorPart;

/**
 * Simple action for inserting a Tex command into the current editor
 * @author Boris von Loesch
 *
 */
public class TexInsertMathSymbolAction extends Action {
	TexCommandEntry entry;
	TexEditor editor;
	
	/**
	 * Creates a new Action from the given entry
     * @param entry
	 */
    public TexInsertMathSymbolAction(TexCommandEntry entry) {
		super();
		this.entry = entry;
        setText(entry.key);
        setToolTipText(entry.info);
        setImageDescriptor(entry.imageDesc);
	}
	
	public void run() {
        if (editor == null)
            return;
        ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
        IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        TexCompletionProposal prop = new TexCompletionProposal(entry, selection.getOffset() + 1, 0, 
                editor.getViewer());
        try {
            // insert a backslash first
            doc.replace(selection.getOffset(), 0, "\\");
            prop.apply(doc);
            int newOffset = selection.getOffset() + entry.key.length() + 1;
            if (entry.arguments > 0) {
                newOffset += 1;
            }
            editor.getSelectionProvider().setSelection(new TextSelection(newOffset, 0));
        } catch (BadLocationException e) {
            TexlipsePlugin.log("Error while trying to insert command", e);
        }
    }

	public void setActiveEditor(IEditorPart part){
		if (part instanceof TexEditor)
			editor = (TexEditor) part;
        else
            editor = null;
	}

}
