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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
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
