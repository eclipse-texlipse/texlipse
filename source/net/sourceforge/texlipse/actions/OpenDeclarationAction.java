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
import net.sourceforge.texlipse.model.AbstractEntry;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.texparser.LatexParserUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.SubStatusLineManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.ide.IDE;

/**
 * This action opens the declaration of
 * <ul><li>References</li>
 * <li>Citations</li>
 * <li>Custom commands (/newcommand)</li></ul>
 * 
 * @author Boris von Loesch
 */
public class OpenDeclarationAction implements IEditorActionDelegate {
	private IEditorPart targetEditor;

	/**
	 * Creates new action.
	 */
	public OpenDeclarationAction() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
	 */
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
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
            throw new RuntimeException("Expecting text editor. Found: "+ targetEditor.getClass().getName());
        }
        IProject project = editor.getProject();
        if (project == null) 
            return;

        ISourceViewer sourceViewer = editor.getViewer();
        ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
        IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        String docString = doc.get();
        
        SubStatusLineManager slm = 
            (SubStatusLineManager) targetEditor.getEditorSite().getActionBars().getStatusLineManager();

        //Get command under cursor
        IRegion comRegion = LatexParserUtils.getCommand(docString, selection.getOffset());
        if (comRegion == null) {
            slm.setErrorMessage(TexlipsePlugin.getResourceString("gotoDeclarationNoCommandFound"));
            slm.setVisible(true);
            sourceViewer.getTextWidget().getDisplay().beep();
            return;
        }        
        String command = docString.substring(comRegion.getOffset(), comRegion.getOffset() + comRegion.getLength());
        
        AbstractEntry refEntry = null;
        if (selection.getOffset() < comRegion.getOffset() + comRegion.getLength()) {
            //Cursor is over the command, not the argument, so we try to find the command first
            AbstractEntry[] entries = editor.getDocumentModel().getRefMana().getCompletionsCom(command.substring(1), TexCommandEntry.NORMAL_CONTEXT); 
            if (entries != null && entries.length > 0 && entries[0].fileName != null) 
                refEntry = entries[0];
        }
        if (refEntry == null && (command.indexOf("ref") >= 0 || command.indexOf("cite") >=0)) {
            //We need the argument
            IRegion region = null;
            try {
                region = LatexParserUtils.getCommandArgument(docString, comRegion.getOffset());
            } catch (BadLocationException e1) { }
            if (region == null) {
                slm.setErrorMessage(TexlipsePlugin.getResourceString("gotoDeclarationNoArgumentFound"));
                slm.setVisible(true);                
                sourceViewer.getTextWidget().getDisplay().beep();
                return;
            }
            String ref = docString.substring(region.getOffset(), region.getOffset() + region.getLength());
        
            if (command.indexOf("ref")>=0) 
                refEntry = editor.getDocumentModel().getRefMana().getLabel(ref);
            else if (command.indexOf("cite")>=0) {
                //There could be more than one reference (e.g. cite1,cite2)
                if (ref.indexOf(',') > 0) {
                    int cIndex = selection.getOffset() - region.getOffset();
                    if (cIndex < 0) {
                        slm.setErrorMessage(TexlipsePlugin.getResourceString("gotoDeclarationNoArgumentFound"));
                        slm.setVisible(true);                
                        sourceViewer.getTextWidget().getDisplay().beep();
                        return;
                    }
                    if (ref.charAt(cIndex) == ',') cIndex--;
                    int start = ref.lastIndexOf(',', cIndex) + 1;
                    if (start < 0) 
                        start = 0;
                    int end = ref.indexOf(',', cIndex);
                    if (end < 0) 
                        end = ref.length();
                    ref = ref.substring(start, end);
                }
                refEntry = editor.getDocumentModel().getRefMana().getBib(ref.trim());
            }
        }
        if (refEntry == null || refEntry.fileName == null) {
            slm.setErrorMessage(TexlipsePlugin.getResourceString("gotoDeclarationNoDeclarationFound"));
            slm.setVisible(true);
            sourceViewer.getTextWidget().getDisplay().beep();
            return;
        }

        IFile file = project.getFile(refEntry.fileName);
        try {
            //Open the correct document and jump to label
            AbstractTextEditor part = (AbstractTextEditor) IDE.openEditor(editor.getEditorSite().getPage(), file);
            IDocument doc2 = part.getDocumentProvider().getDocument(part.getEditorInput());
            int lineOffset = doc2.getLineOffset(refEntry.startLine - 1);
            int offset = 0;
            if (command.indexOf("ref")>=0 && refEntry.position != null)
                offset = refEntry.position.offset;
            part.getEditorSite().getSelectionProvider().setSelection(
                    new TextSelection(lineOffset + offset, 0));
        } catch (PartInitException e) {
            TexlipsePlugin.log("Jump2Label PartInitException", e);
        } catch (BadLocationException e) {
            TexlipsePlugin.log("Jump2Label BadLocationException", e);
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
