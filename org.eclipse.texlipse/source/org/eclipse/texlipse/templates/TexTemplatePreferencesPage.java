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
