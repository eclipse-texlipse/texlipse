/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.templates;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;


/**
 * @author Esa Seuranen
 * 
 * Class for the BiBTeX editor's preference page for templates.
 */
public class BibTexTemplatePreferencesPage
extends TemplatePreferencePage
implements IWorkbenchPreferencePage {
    
    /**
     * Constructor
     */
    public BibTexTemplatePreferencesPage() {
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setTemplateStore(TexlipsePlugin.getDefault().getBibTemplateStore());
        setContextTypeRegistry(TexlipsePlugin.getDefault().getBibContextTypeRegistry());
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
