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
import org.eclipse.jface.action.SubStatusLineManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.editor.TexEditor;
import org.eclipse.texlipse.editor.TexPairMatcher;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

/**
 * Action for jumping to the associated brace
 * 
 * @author Boris von Loesch
 */
public class GoToMatchingBracketAction implements IEditorActionDelegate {

    private TexEditor targetEditor;
    
    public void selectionChanged(IAction action, ISelection selection) {
    }

    public void run(IAction action) {
        if (targetEditor == null) return;
        ISourceViewer sourceViewer= targetEditor.getViewer();
        IDocument document= sourceViewer.getDocument();
        if (document == null)
            return;
        ITextSelection selection = (ITextSelection) targetEditor.getSelectionProvider().getSelection();
        SubStatusLineManager slm = 
            (SubStatusLineManager) targetEditor.getEditorSite().getActionBars().getStatusLineManager();
        
        int selectionLength= Math.abs(selection.getLength());
        if (selectionLength > 1) {
            slm.setErrorMessage(TexlipsePlugin.getResourceString("gotoMatchingBracketNotSelected"));
            slm.setVisible(true);
            sourceViewer.getTextWidget().getDisplay().beep();
            return;
        }
        
        int sourceCaretOffset= selection.getOffset() + selection.getLength();

        TexPairMatcher fBracketMatcher = new TexPairMatcher("{}[]()");
        
        IRegion region= fBracketMatcher.match(document, sourceCaretOffset);
        if (region == null) {
            slm.setErrorMessage(TexlipsePlugin.getResourceString("gotoMatchingBracketNotFound"));
            slm.setVisible(true);            
            sourceViewer.getTextWidget().getDisplay().beep();
            return;
        }

        int offset= region.getOffset();
        int length= region.getLength();

        if (length < 1) return;

        int anchor = fBracketMatcher.getAnchor();
        int targetOffset= (ICharacterPairMatcher.RIGHT == anchor) ? offset + 1: offset + length;

        if (selection.getLength() < 0)
            targetOffset -= selection.getLength();

        sourceViewer.setSelectedRange(targetOffset, selection.getLength());
        sourceViewer.revealRange(targetOffset, selection.getLength());
    }
    
    public void setActiveEditor(IAction action, IEditorPart part) {
        if (part instanceof TexEditor)
            targetEditor = (TexEditor) part;
        else
            targetEditor = null;
    }

}
