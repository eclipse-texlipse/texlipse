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

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.editor.ColorManager;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set syntax highlighting colors.
 * 
 * @author Kimmo Karlsson
 */
public class ColoringPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

       final static String[][] RADIO_STYLE_STRING = {{"Normal", ""+SWT.NORMAL},
               {"Bold", ""+SWT.BOLD}, {"Italic", ""+SWT.ITALIC}};

    /**
     * Creates an instance of the "syntax highlighting colors" -preference page.
     */
    public ColoringPreferencePage() {
        super(GRID);

        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceColorPageDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        adjustGridLayout();
        addField(new ColorFieldEditor(ColorManager.DEFAULT, TexlipsePlugin.getResourceString("preferenceColorTextLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.DEFAULT_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.COMMAND, TexlipsePlugin.getResourceString("preferenceColorCommandLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.COMMAND_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.CURLY_BRACKETS, TexlipsePlugin.getResourceString("preferenceColorArgumentLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.CURLY_BRACKETS_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.SQUARE_BRACKETS, TexlipsePlugin.getResourceString("preferenceColorOptionalLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.SQUARE_BRACKETS_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.EQUATION, TexlipsePlugin.getResourceString("preferenceColorMathLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.EQUATION_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.COMMENT, TexlipsePlugin.getResourceString("preferenceColorCommentLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.COMMENT_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.TEX_NUMBER, TexlipsePlugin.getResourceString("preferenceColorNumberLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.TEX_NUMBER_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.TEX_SPECIAL, TexlipsePlugin.getResourceString("preferenceColorSpeLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.TEX_SPECIAL_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.VERBATIM, TexlipsePlugin.getResourceString("preferenceColorVerbatimLabel"), getFieldEditorParent()));
        addField(new RadioGroupFieldEditor(ColorManager.VERBATIM_STYLE, "", 3, RADIO_STYLE_STRING, getFieldEditorParent()));
    }
    protected void adjustGridLayout() {
        super.adjustGridLayout();
        getFieldEditorParent().setLayout(new GridLayout(6, false));
    }

    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }
}
