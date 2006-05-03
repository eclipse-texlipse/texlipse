/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.properties.editor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
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
