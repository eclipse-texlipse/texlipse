/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.spelling;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.KeyValueListFieldEditor;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set spell checker environment variables.
 * 
 * @author Kimmo Karlsson
 */
public class SpellCheckerEnvPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

    /**
     * Creates an instance of the preference page.
     */
    public SpellCheckerEnvPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceSpellEnvPageDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        addField(new KeyValueListFieldEditor(SpellChecker.SPELL_CHECKER_ENV, TexlipsePlugin.getResourceString("preferenceSpellEnvLabel"), getFieldEditorParent()));
    }
    
    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }
}
