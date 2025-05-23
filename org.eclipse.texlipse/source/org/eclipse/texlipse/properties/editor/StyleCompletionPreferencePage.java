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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.properties.TexlipseProperties;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for defining style completions
 * 
 * @author Oskar Ojala
 */
public class StyleCompletionPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    /**
     * Creates an instance of the preference page.
     */
    public StyleCompletionPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceStyleDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        // TODO StyleListFieldEditor has an edit button, the
        // lower one has an import dialog which is irrelevant for this
        // but could be useful in general
        addField(new StyleListFieldEditor(
                TexlipseProperties.STYLE_COMPLETION_SETTINGS,
                TexlipsePlugin.getResourceString("preferenceStyleLabel"),
                getFieldEditorParent()));

//        addField(new KeyValueListFieldEditor(
//                TexlipseProperties.STYLE_COMPLETION_SETTINGS,
//                TexlipsePlugin.getResourceString("preferenceSmartKeyLabel"),
//                getFieldEditorParent()));
    }

    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }

}
