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
import org.eclipse.texlipse.properties.StringListFieldEditor;
import org.eclipse.texlipse.properties.TexlipseProperties;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * Outline preferences page.
 */
public class OutlinePreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

    /**
     * Construct a new page.
     */
	public OutlinePreferencePage(){
		super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceOutlinePageDescription"));
	}
    
	/**
     * Creates the page components.
	 */
	protected void createFieldEditors() {
        Label label = new Label(getFieldEditorParent(), SWT.LEFT | SWT.WRAP);
        label.setText(TexlipsePlugin.getResourceString("preferenceOutlinePageSections"));
        addField(new BooleanFieldEditor(TexlipseProperties.OUTLINE_PREAMBLE, TexlipsePlugin.getResourceString("preferenceOutlineIncludePreamble"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.OUTLINE_PART, TexlipsePlugin.getResourceString("preferenceOutlineIncludeParts"), getFieldEditorParent()));
		addField(new BooleanFieldEditor(TexlipseProperties.OUTLINE_CHAPTER, TexlipsePlugin.getResourceString("preferenceOutlineIncludeChapters"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.OUTLINE_SECTION, TexlipsePlugin.getResourceString("preferenceOutlineIncludeSections"), getFieldEditorParent()));
		addField(new BooleanFieldEditor(TexlipseProperties.OUTLINE_SUBSECTION, TexlipsePlugin.getResourceString("preferenceOutlineIncludeSubSections"), getFieldEditorParent()));
		addField(new BooleanFieldEditor(TexlipseProperties.OUTLINE_SUBSUBSECTION, TexlipsePlugin.getResourceString("preferenceOutlineIncludeSubSubSections"), getFieldEditorParent()));
		addField(new BooleanFieldEditor(TexlipseProperties.OUTLINE_PARAGRAPH, TexlipsePlugin.getResourceString("preferenceOutlineIncludeParagraphs"), getFieldEditorParent()));
        addField(new StringListFieldEditor(TexlipseProperties.OUTLINE_ENVS, TexlipsePlugin.getResourceString("preferenceOutlineEnvsLabel"), getFieldEditorParent()));
	}

	/**
	 * Page initialization. Does nothing.
	 */
	public void init(IWorkbench workbench) {
	}
}
