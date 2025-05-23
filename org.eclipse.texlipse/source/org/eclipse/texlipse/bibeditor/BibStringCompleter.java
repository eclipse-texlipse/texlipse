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

package org.eclipse.texlipse.bibeditor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.properties.TexlipseProperties;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * This class listens to document events and whenever { or " is typed it inserts
 * the corresponding closing string mark after the cursor position.
 * 
 * @author Oskar Ojala
 * 
 * TODO This feature must be reimplemented, cmp. Bracketinserter 
 */
public class BibStringCompleter implements IDocumentListener {

	private ITextEditor editor;

	private IDocument document;

	private boolean recentlyAdded = false;

	/**
	 * Constructs a new string completer.
	 * 
	 * @param editor
	 *            The editor that this listener is associated to
	 */
	public BibStringCompleter(ITextEditor editor) {
		this.editor = editor;
		this.document = editor.getDocumentProvider().getDocument(
				editor.getEditorInput());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent event) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	public void documentChanged(DocumentEvent event) {
	    if (TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(
				TexlipseProperties.BIB_STRING)) {

			if ("{".equals(event.getText())) {
				ITextSelection textSelection = (ITextSelection) this.editor
						.getSelectionProvider().getSelection();
				try {
					document.replace(textSelection.getOffset() + 0, 1, "}");
				} catch (BadLocationException e) {
				}
			} else if ("\"".equals(event.getText()) && !recentlyAdded) {
				ITextSelection textSelection = (ITextSelection) this.editor
						.getSelectionProvider().getSelection();
				try {
					recentlyAdded = true;
					document.replace(textSelection.getOffset() + 1, 0, "\"");
				} catch (BadLocationException e) {
				}
				recentlyAdded = false;
			}
		}
	}
}
