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

package org.eclipse.texlipse.templates;

import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;


/**
 * @author Esa Seuranen
 * 
 * Class for the TeX editor's preference page for templates.
 */
public class TexTemplatePreferencesPage
extends TemplatePreferencePage
implements IWorkbenchPreferencePage {
    
    /**
     * Constructor
     */
    public TexTemplatePreferencesPage() {
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setTemplateStore(TexlipsePlugin.getDefault().getTexTemplateStore());
        setContextTypeRegistry(TexlipsePlugin.getDefault().getTexContextTypeRegistry());
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.templates.TemplatePreferencePage#isShowFormatterSetting()
     */
    protected boolean isShowFormatterSetting() {
        return true;
    }
    
    /**
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    public boolean performOk() {
        boolean ok = super.performOk();
        
        if (ok)
            TexlipsePlugin.getDefault().savePluginPreferences();
        
        return ok;
    }	
}
