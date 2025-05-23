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
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set viewer environment variables.
 * 
 * @author Kimmo Karlsson
 */
public class ViewerEnvPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

    /**
     * Creates an instance of the preference page.
     */
    public ViewerEnvPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceViewerEnvPageDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        addField(new KeyValueListFieldEditor(TexlipseProperties.VIEWER_ENV_SETTINGS, TexlipsePlugin.getResourceString("preferenceViewerEnvLabel"), getFieldEditorParent()));
    }
    
    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }
}
