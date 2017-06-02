/*******************************************************************************
 * Copyright (c) 2017 the TeXlipse team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
