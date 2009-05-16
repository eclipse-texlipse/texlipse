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
import net.sourceforge.texlipse.properties.TexlipsePreferencePage;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set spell checker preferences.
 * 
 * @author Kimmo Karlsson
 */
public class SpellCheckerPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

    /**
     * Creates an instance of the preference page.
     */
    public SpellCheckerPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceSpellPageDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        TexlipsePreferencePage.addSpacer(3, getFieldEditorParent());
        addField(new FileFieldEditor(SpellChecker.SPELL_CHECKER_COMMAND, TexlipsePlugin.getResourceString("preferenceSpellCommandLabel"), getFieldEditorParent()));
        TexlipsePreferencePage.addSpacer(3, getFieldEditorParent());
        addField(new StringFieldEditor(SpellChecker.SPELL_CHECKER_ARGUMENTS, TexlipsePlugin.getResourceString("preferenceSpellArgumentsLabel"), getFieldEditorParent()));
        TexlipsePreferencePage.addSpacer(3, getFieldEditorParent());
        addField(new BooleanFieldEditor(TexlipseProperties.ECLIPSE_BUILDIN_SPELLCHECKER, TexlipsePlugin.getResourceString("preferenceSpellUseBuildIn"), getFieldEditorParent()));
    }
    
    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }
}
