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

package org.eclipse.texlipse.properties.editor;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.properties.TexlipseProperties;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The preference page for vim-latex -style smart key support, comes
 * under the Editor-menu 
 * 
 * @author Oskar Ojala
 */
public class SmartKeyPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    /**
     * Creates a smart key preference page
     */
    public SmartKeyPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceSmartKeyDescription"));
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    protected void createFieldEditors() {

        // Smart backspace
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_BACKSPACE,
                TexlipsePlugin.getResourceString("preferenceSmartBackspace"), getFieldEditorParent()));
        Label backspaceLabel = new Label(getFieldEditorParent(),SWT.LEFT | SWT.WRAP);
        backspaceLabel.setText(TexlipsePlugin.getResourceString("preferenceSmartBackspaceText"));
        
        // Smart quotes
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_QUOTES,
                TexlipsePlugin.getResourceString("preferenceSmartReplaceQuotes"), getFieldEditorParent()));
        Label quotesLabel = new Label(getFieldEditorParent(),SWT.LEFT | SWT.WRAP);
        quotesLabel.setText(TexlipsePlugin.getResourceString("preferenceSmartReplaceQuotesText"));

        // Smart parens
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_PARENS,
                TexlipsePlugin.getResourceString("preferenceSmartBracketCompletion"), getFieldEditorParent()));
        Label bracketLabel = new Label(getFieldEditorParent(),SWT.LEFT | SWT.WRAP);
        bracketLabel.setText(TexlipsePlugin.getResourceString("preferenceSmartBracketCompletionText"));

        // Smart \ldots
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_LDOTS,
                TexlipsePlugin.getResourceString("preferenceSmartLdots"), getFieldEditorParent()));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }
}
