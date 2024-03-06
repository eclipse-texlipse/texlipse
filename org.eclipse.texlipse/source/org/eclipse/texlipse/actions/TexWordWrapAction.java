/*******************************************************************************
 * Copyright (c) 2017 the TeXlipse team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/
package org.eclipse.texlipse.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.editor.TexAutoIndentStrategy;
import org.eclipse.texlipse.editor.TexEditor;
import org.eclipse.texlipse.properties.TexlipseProperties;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;


/**
 * Listens for word wrap toggle -actions, toggling wrap on or off.
 * 
 * @author Laura Takkinen
 * @author Oskar Ojala
 */
public class TexWordWrapAction implements IEditorActionDelegate, IActionDelegate2 {
    private IEditorPart targetEditor;
    private boolean off;

    public TexWordWrapAction() {
        this.off = !TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.WORDWRAP_DEFAULT);
        TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new WrapPropertyChangeListener());
    }

    /**
     * Listens wrap type changes
     * 
     * @author Laura Takkinen
     */
    private class WrapPropertyChangeListener implements IPropertyChangeListener {

        /**
         * Changes between wrap types (soft <-> hard) if wrap toggle
         * button is checked.
         */
        public void propertyChange(PropertyChangeEvent event) {
            String ev = event.getProperty();
            if (ev.equals("wrapType")) {
                configureWordWrap();
            }
        }
    }

    /**
     * Performs the operations needed on wrap configuration changes
     *
     * @author Leonardo Montecchi
     */
    private void configureWordWrap() {
        if (!off) {
            setType();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate2#init(org.eclipse.jface.action.IAction)
     */
    public void init(IAction action) {
        action.setChecked(!off);
        configureWordWrap();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        this.targetEditor = targetEditor;
        action.setEnabled(this.targetEditor instanceof TexEditor);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        ISourceViewer viewer = getTextEditor().getViewer();
        if (action.isChecked()) {
            this.off = false;
            setType();
        } else {
            this.off = true;
            TexAutoIndentStrategy.setHardWrap(false);
            viewer.getTextWidget().setWordWrap(false);
        }
    }

    /**
     * Checks the type of the word wrap and activates the correct type.
     */
    private void setType() {
        String wrapStyle = TexlipsePlugin.getPreference(TexlipseProperties.WORDWRAP_TYPE);
        ISourceViewer viewer = getTextEditor() != null ? getTextEditor().getViewer() : null;
        if (wrapStyle.equals(TexlipseProperties.WORDWRAP_TYPE_SOFT)) {
            TexAutoIndentStrategy.setHardWrap(false);
            if (viewer != null) {
                viewer.getTextWidget().setWordWrap(true);
            }
        } else if (wrapStyle.equals(TexlipseProperties.WORDWRAP_TYPE_HARD)) {
            TexAutoIndentStrategy.setHardWrap(true);
            if (viewer != null) {
                viewer.getTextWidget().setWordWrap(false);
            }
        }
    }

    /**
     * Gets the instance of TexEditor
     * @return current TexEditor instance
     */
    private TexEditor getTextEditor() {
        if (this.targetEditor == null) return null;
        if (this.targetEditor instanceof TexEditor) {
            return (TexEditor) targetEditor;
        } else {
            throw new RuntimeException("Expecting TeX editor. Found: "+targetEditor.getClass().getName());
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate2#dispose()
     */
    public void dispose() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate2#runWithEvent(org.eclipse.jface.action.IAction, org.eclipse.swt.widgets.Event)
     */
    public void runWithEvent(IAction action, Event event) {
        run(action);
    }

}
