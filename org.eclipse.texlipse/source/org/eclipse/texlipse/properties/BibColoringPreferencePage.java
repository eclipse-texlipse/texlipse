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

package org.eclipse.texlipse.properties;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.bibeditor.BibColorProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set syntax highlighting colors.
 * 
 * @author kimmo
 */
public class BibColoringPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

    /**
     * Creates an instance of the "syntax highlighting colors" -preference page.
     */
    public BibColoringPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceBibColorPageDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        addField(new ColorFieldEditor(BibColorProvider.DEFAULT, TexlipsePlugin.getResourceString("preferenceBibColorTextLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(BibColorProvider.TYPE, TexlipsePlugin.getResourceString("preferenceBibColorTypeLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(BibColorProvider.KEYWORD, TexlipsePlugin.getResourceString("preferenceBibColorKeywordLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(BibColorProvider.STRING, TexlipsePlugin.getResourceString("preferenceBibColorStringLabel"), getFieldEditorParent()));
//        addField(new ColorFieldEditor(BibColorProvider.MULTI_LINE_COMMENT, TexlipsePlugin.getResourceString("preferenceBibColorMLCommentLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(BibColorProvider.SINGLE_LINE_COMMENT, TexlipsePlugin.getResourceString("preferenceBibColorCommentLabel"), getFieldEditorParent()));
    }
    
    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }
}
