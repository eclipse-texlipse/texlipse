/*
 * $Id$
 *
 * Copyright (c) 2005 by the TeXlipse Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Changes the selection to be inside certain start and stop tags.
 * 
 * @author Andrew Eisenberg
 * @author Kimmo Karlsson
 */
public abstract class AbstractTexSelectionChange implements IEditorActionDelegate {

    // target editor containing the selected text
    private IEditorPart targetEditor;

    // current selection
    private TexSelections selection;

    // start tag
    private String startTag;

    // end tag
    private String endTag;

    /**
     * Creates the selection changer.
     * The start and stop tags are initialized here.
     */
    protected AbstractTexSelectionChange() {
        startTag = getStartTag();
        endTag = getEndTag();
    }

    /**
     * Returns the start tag.
     * This is the default implementation, which
     * should be overridden by subclasses.
     * @return start tag
     */
    protected String getStartTag() {
        return "\\{";
    }

    /**
     * Returns the end tag.
     * This is the default implementation that only returns "}".
     * @return end tag
     */
    protected String getEndTag() {
        return "}";
    }

    /**
     * Sets the active editor.
     * This method is called by Eclipse.
     * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction,
     *      org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        this.targetEditor = targetEditor;
    }

    /**
     * Runs the selection modification action.
     * This method is called by Eclipse.
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        selection = new TexSelections(getTextEditor());
        if (isSelectionChanged()) {
            unchange();
        } else {
            change();
        }
    }

    /**
     * Returns the currectly active text editor.
     * @return text editor
     */
    private ITextEditor getTextEditor() {
        if (targetEditor instanceof ITextEditor) {
            return (ITextEditor) targetEditor;
        } else {
            throw new RuntimeException("Expecting text editor. Found:"
                    + targetEditor.getClass().getName());
        }
    }

    /**
     * Changes the selection to be inside the start and end tags.
     */
    private void change() {
        try {
            
            int selStart = selection.getTextSelection().getOffset();
            String replaceText = startTag
                    + selection.getCompleteSelection() + endTag;

            // Replace the text with the modified information
            selection.getDocument().replace(selStart,
                    selection.getSelLength(), replaceText);

            getTextEditor().selectAndReveal(selStart, replaceText.length());
            
        } catch (Exception e) {
            TexlipsePlugin.log("Wrapping selection inside " + startTag, e);
        }
    }

    /**
     * Removes the start and end tags from selection start and end.
     * Assumes that the selection starts with the start tag and
     * ends with the end tag.
     */
    private void unchange() {
        try {
            String selected = selection.getCompleteSelection();
            int selStart = selection.getTextSelection().getOffset();
            
            // Replace the text with the modified information
            String replaceText = selected.substring(startTag.length(),
                    selected.length() - endTag.length());
            selection.getDocument().replace(selStart,
                    selection.getSelLength(), replaceText);
            
            getTextEditor().selectAndReveal(selStart, replaceText.length());
            
        } catch (Exception e) {
            TexlipsePlugin.log("Unwrapping selection from " + startTag, e);
        }
    }

    /**
     * Checks if the selection is already changed.
     * @return true, if the selection starts with the start tag and ends
     *      with the end tag
     */
    private boolean isSelectionChanged() {
        return selection.getCompleteSelection().startsWith(startTag)
                && selection.getCompleteSelection().endsWith(endTag);
    }

    /**
     * Receive a notification about a changed selection.
     * This action is set enabled or disabled according to the type of selection.
     * This method is called by Eclipse.
     * 
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof TextSelection) {
            action.setEnabled(true);
            return;
        }
        action.setEnabled(targetEditor instanceof ITextEditor);
    }
}
