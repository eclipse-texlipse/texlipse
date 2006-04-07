/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.properties.editor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.StringListFieldEditor;
import net.sourceforge.texlipse.properties.TexlipsePreferencePage;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * A preference page that is contributed to the Preferences dialog. 
 * 
 * Preferences are stored in the preference store that belongs to
 * the main plug-in class.
 * 
 * @author Kimmo Karlsson
 */
public class CodeFoldingPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
    
    /**
     * Creates the preference page.
	 */
	public CodeFoldingPreferencePage() {
		super(GRID);
		setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
		//setDescription(TexlipsePlugin.getResourceString("preferenceCodeFoldingPageDescription"));
	}

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
	public void createFieldEditors() {
		addField(new BooleanFieldEditor(TexlipseProperties.CODE_FOLDING, TexlipsePlugin.getResourceString("preferenceCodeFolding"), getFieldEditorParent()));
        TexlipsePreferencePage.addSpacer(getFieldEditorParent());
        Label label = new Label(getFieldEditorParent(), SWT.LEFT | SWT.WRAP);
        label.setText(TexlipsePlugin.getResourceString("preferenceCodeFoldingSections"));
        addField(new BooleanFieldEditor(TexlipseProperties.CODE_FOLDING_PREAMBLE, TexlipsePlugin.getResourceString("preferenceOutlineIncludePreamble"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.CODE_FOLDING_PART, TexlipsePlugin.getResourceString("preferenceOutlineIncludeParts"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.CODE_FOLDING_CHAPTER, TexlipsePlugin.getResourceString("preferenceOutlineIncludeChapters"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.CODE_FOLDING_SECTION, TexlipsePlugin.getResourceString("preferenceOutlineIncludeSections"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.CODE_FOLDING_SUBSECTION, TexlipsePlugin.getResourceString("preferenceOutlineIncludeSubSections"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.CODE_FOLDING_SUBSUBSECTION, TexlipsePlugin.getResourceString("preferenceOutlineIncludeSubSubSections"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.CODE_FOLDING_PARAGRAPH, TexlipsePlugin.getResourceString("preferenceOutlineIncludeParagraphs"), getFieldEditorParent()));
        addField(new StringListFieldEditor(TexlipseProperties.CODE_FOLDING_ENVS, TexlipsePlugin.getResourceString("preferenceCodeFoldingEnvsLabel"), getFieldEditorParent()));
    }

    /**
     * Initializes this preference page for the given workbench.
     * <p>
     * This method is called automatically as the preference page is being created
     * and initialized. Clients must not call this method.
     * </p>
     *
     * @param workbench the workbench
     */
	public void init(IWorkbench workbench) {
        // nothing to do
	}
}
