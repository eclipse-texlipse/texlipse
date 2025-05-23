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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * @author Laura Takkinen
 *
 * Listens uncomment actions.
 */
public class TexUncomment implements IEditorActionDelegate {
	private IEditorPart targetEditor;
	private static TexSelections selection;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		selection = new TexSelections(getTextEditor());
		uncomment();
	}
	
	/**
	 * This function returns the text editor.
	 */
	private ITextEditor getTextEditor() {
		if (targetEditor instanceof ITextEditor) {
			return (ITextEditor) targetEditor;
		} else {
			throw new RuntimeException("Expecting text editor. Found:"+targetEditor.getClass().getName());
		}
	}

	/**
	 * Uncomments selected lines of text.
	 */
	private void uncomment() {
        StringBuffer strbuf = new StringBuffer();
        selection.selectCompleteLines();

        try {
            // For each line, comment them out
            for (int i = selection.getStartLineIndex(); i <= selection.getEndLineIndex(); i++) {
                String line = selection.getLine(i);
                
                //we may want to remove comments that have leading whitespace
                line = line.trim();
                if (line.startsWith("% ")) {
                    strbuf.append(line.replaceFirst("% ", "") + (i < selection.getEndLineIndex() ? selection.getEndLineDelim() : ""));
                } else if (line.startsWith("%")) {
                        strbuf.append(line.replaceFirst("%", "") + (i < selection.getEndLineIndex() ? selection.getEndLineDelim() : ""));
                } else {
                    strbuf.append(line + (i < selection.getEndLineIndex() ? selection.getEndLineDelim() : ""));
                }
            }
            // Replace the text with the modified information
            selection.getDocument().replace(selection.getStartLine().getOffset(), selection.getSelLength(), strbuf.toString());
            
        } catch (Exception e) {
        	TexlipsePlugin.log("TexUncomment.uncomment(): ", e);
        }
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof TextSelection) {
			action.setEnabled(true);
			return;
		}
		action.setEnabled( targetEditor instanceof ITextEditor);
	}
}
