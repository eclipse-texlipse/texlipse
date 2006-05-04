/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.ide.IDE;

/**
 * Action for jumping to a label from a reference.
 * 
 * @author Boris von Loesch
 */
public class TexJump2Label implements IEditorActionDelegate {
	private IEditorPart targetEditor;

	/**
	 * Creates new action.
	 */
	public TexJump2Label() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
	}

	/**
     * Extracts the label of a \ref{label} command if the character at position 
     * offset is part of this \ref command 
	 * @param doc
	 * @param offset
	 * @return name of the referenced position
	 */
	private String getReference(IDocument doc, int offset){
		try {
			int i = offset;
            while (i >= 0 && doc.getChar(i) != '\\')
                i--;
            if (i == -1)
                return null;
            int start = i;
            i = offset;
            while (i < doc.getLength() && doc.getChar(i) != '}')
                i++;
            if (i == doc.getLength())
                return null;
            int end = i;
            String current = doc.get().substring(start, end + 1);
            if (current.startsWith("\\ref{") && current.lastIndexOf("{") < 5) {
                return current.substring(5, current.length() - 1);
            } else {
                return null;
            }
		} catch (BadLocationException e) {
            TexlipsePlugin.log("Jump2Label BadLocationException", e);
			return null;
		}
	}

	/**
     * Returns the project of the currentInput
     * @param currentInput
     * @return project or null if the file has no project or is no IFileEditorInput
	 */
    public static IProject getProject(IEditorInput currentInput) {
        IProject project = null;
        if (currentInput instanceof IFileEditorInput) {
            IFileEditorInput fileInput = (IFileEditorInput) currentInput;
            IFile currentFile = fileInput.getFile();
            project = currentFile.getProject();
        }
        return project;
    }
	
	/*
     *  (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
    public void run(IAction action) {
		TexEditor editor;
        if (targetEditor instanceof TexEditor) {
            editor = (TexEditor) targetEditor;
        } else {
            throw new RuntimeException("Expecting text editor. Found:"
                    + targetEditor.getClass().getName());
        }

        ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
        IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        // Parse for \ref{...}
        String ref = getReference(doc, selection.getOffset());
        if (ref != null) {
            ReferenceEntry label = editor.getDocumentModel().getRefMana().getLabel(ref);
            if (label == null)
                return;
            IFile file = getProject(editor.getEditorInput()).getFile(label.fileName);
            try {
                //Open the correct document and jump to label
                AbstractTextEditor part = (AbstractTextEditor) IDE.openEditor(editor.getEditorSite().getPage(), file);
                IDocument doc2 = part.getDocumentProvider().getDocument(part.getEditorInput());
                int lineOffset = doc2.getLineOffset(label.startLine - 1);
                part.getEditorSite().getSelectionProvider().setSelection(
                                new TextSelection(lineOffset
                                        + label.position.offset, 0));
            } catch (PartInitException e) {
                TexlipsePlugin.log("Jump2Label PartInitException", e);
            } catch (BadLocationException e) {
                TexlipsePlugin.log("Jump2Label BadLocationException", e);
            }
        }
	}

    /*
     *  (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
		action.setEnabled(targetEditor instanceof TexEditor);
	}

}
