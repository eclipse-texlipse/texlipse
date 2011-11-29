/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.properties;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set build cycle autodetection preferences.
 * 
 * @author Matthias Erll
 */
public class BuilderCyclePreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

    public static final int MIN_LIMIT = 1;
    public static final int MAX_LIMIT = 20;

    /**
     * Creates an instance of the preference page.
     */
    public BuilderCyclePreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceBuilderCyclePageDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        // TODO This can be re-activated when the latex log parser is ready
        /*
        addField(new BooleanFieldEditor(TexlipseProperties.BUILD_CYCLE_FLS_ENABLED,
                TexlipsePlugin.getResourceString("preferenceBuilderCycleFlsLabel"),
                getFieldEditorParent()));
        TexlipsePreferencePage.addSpacer(2, getFieldEditorParent());
        */
        Label addDescriptionLabel = new Label(getFieldEditorParent(), SWT.LEAD | SWT.WRAP);
        addDescriptionLabel.setText(TexlipsePlugin.getResourceString(
                "preferenceBuilderCycleAddDescriptionLabel"));
        addDescriptionLabel.setLayoutData(new GridData(SWT.FILL , SWT.FILL, true,
                false, 2, 1));
        addField(new StringListFieldEditor(TexlipseProperties.BUILD_CYCLE_ADD_EXTS,
                TexlipsePlugin.getResourceString("preferenceBuilderCycleAddExtsLabel"),
                getFieldEditorParent()));
        TexlipsePreferencePage.addSpacer(2, getFieldEditorParent());
        Composite bottomPart = new Composite(getFieldEditorParent(), SWT.NULL);
        bottomPart.setLayout(new GridLayout(2, false));
        bottomPart.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        Label maxDescriptionLabel = new Label(bottomPart, SWT.LEAD | SWT.WRAP);
        maxDescriptionLabel.setText(TexlipsePlugin.getResourceString(
                "preferenceBuilderCycleMaxDescriptionLabel"));
        maxDescriptionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                false, 2, 1));
        IntegerFieldEditor maxCycleEditor = new IntegerFieldEditor(
                TexlipseProperties.BUILD_CYCLE_MAX, TexlipsePlugin.getResourceString(
                        "preferenceBuilderCycleMaxAmountLabel"), bottomPart);
        maxCycleEditor.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
        maxCycleEditor.setValidRange(MIN_LIMIT, MAX_LIMIT);
        addField(maxCycleEditor);
        addField(new BooleanFieldEditor(TexlipseProperties.BUILD_CYCLE_MAX_ERROR,
                TexlipsePlugin.getResourceString("preferenceBuilderCycleShowErrorOnMax"),
                bottomPart));
        addField(new BooleanFieldEditor(TexlipseProperties.BUILD_CYCLE_HALT_INVALID,
                TexlipsePlugin.getResourceString("preferenceBuilderCycleHaltOnInvalidRunner"),
                bottomPart));
    }

    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }

}
