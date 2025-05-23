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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set bib file directories.
 * 
 * @author kimmo
 */
public class BibDirectoriesPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

    /**
     * Creates an instance of the "bib directories" -preference page.
     */
    public BibDirectoriesPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceBibDirPageDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        PathEditor dir = new PathEditor(TexlipseProperties.BIB_DIR, TexlipsePlugin.getResourceString("preferenceBibDirLabel"), "", parent);
        addField(dir);
        dir.getButtonBoxControl(parent).setToolTipText(TexlipsePlugin.getResourceString("preferenceBibDirTooltip"));
        dir.getLabelControl(parent).setToolTipText(TexlipsePlugin.getResourceString("preferenceBibDirTooltip"));
        dir.getListControl(parent).setToolTipText(TexlipsePlugin.getResourceString("preferenceBibDirTooltip"));
    }
    
    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }
}
