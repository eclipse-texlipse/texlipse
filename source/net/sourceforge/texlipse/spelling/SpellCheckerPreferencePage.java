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
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set spell checker preferences.
 * 
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 */
public class SpellCheckerPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {
    
    private DirectoryFieldEditor customDictDir;
    private DirectoryFieldEditor dictDir;
    
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
        
        final Group group = new Group(getFieldEditorParent(), SWT.NONE);
        group.setText(TexlipsePlugin.getResourceString("preferenceSpellBuildIn"));
        group.setLayout(new GridLayout());
        GridData layData = new GridData(SWT.FILL, SWT.NONE, true, true);
        layData.horizontalSpan = 3;
        group.setLayoutData(layData);
        TexlipsePreferencePage.addSpacer(1, group);
        
        BooleanFieldEditor buildInSpell = 
            new BooleanFieldEditor(TexlipseProperties.ECLIPSE_BUILDIN_SPELLCHECKER, TexlipsePlugin.getResourceString("preferenceSpellUseBuildIn"), group) {
            protected void valueChanged(boolean oldValue, boolean newValue) {
                super.valueChanged(oldValue, newValue);
                customDictDir.setEnabled(newValue, group);
                dictDir.setEnabled(newValue, group);
            }
        };
        BooleanFieldEditor ignoreComments = new BooleanFieldEditor(TexlipseProperties.SPELLCHECKER_IGNORE_COMMENTS, 
                TexlipsePlugin.getResourceString("preferenceSpellIgnoreComments") , group);
        BooleanFieldEditor ignoreMixedCase = new BooleanFieldEditor(TexlipseProperties.SPELLCHECKER_IGNORE_MIXED_CASE, 
                TexlipsePlugin.getResourceString("preferenceSpellIgnoreMixedCase") , group);
        addField (ignoreMixedCase);
//        BooleanFieldEditor ignoreMath = new BooleanFieldEditor(TexlipseProperties.SPELLCHECKER_IGNORE_MATH, 
//                TexlipsePlugin.getResourceString("preferenceSpellIgnoreMath") , group);
//        addField(ignoreMath);
        TexlipsePreferencePage.addSpacer(3, group);
        
        dictDir = 
            new DirectoryFieldEditor(TexlipseProperties.SPELLCHECKER_DICT_DIR, TexlipsePlugin.getResourceString("preferenceSpellDictDir"), group);
        dictDir.setEnabled(TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.ECLIPSE_BUILDIN_SPELLCHECKER), group);
        customDictDir = 
            new DirectoryFieldEditor(TexlipseProperties.SPELLCHECKER_CUSTOM_DICT_DIR, TexlipsePlugin.getResourceString("preferenceSpellCustomDict"), group);
        customDictDir.setEnabled(TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.ECLIPSE_BUILDIN_SPELLCHECKER), group);

        addField(buildInSpell);
        addField(dictDir);
        addField(customDictDir);
        addField(ignoreComments);
        TexlipsePreferencePage.addSpacer(3, group);
        
        //Construct Aspell preferences group
        final Group aspellGroup = new Group(getFieldEditorParent(), SWT.NONE);
        aspellGroup.setText(TexlipsePlugin.getResourceString("preferenceSpellAspell"));
        aspellGroup.setLayout(new GridLayout());
        GridData aspellLayData = new GridData(SWT.FILL, SWT.NONE, true, true);
        aspellLayData.horizontalSpan = 3;
        aspellGroup.setLayoutData(aspellLayData);
        TexlipsePreferencePage.addSpacer(3, aspellGroup);
        
        addField(new FileFieldEditor(SpellChecker.SPELL_CHECKER_COMMAND, TexlipsePlugin.getResourceString("preferenceSpellCommandLabel"), aspellGroup));
        //FIXME: Looks ugly but I have no clue how to get it in one row
        Composite c = new Composite(aspellGroup, SWT.NONE);
        c.setLayout(new GridLayout(3, false));
        GridData layData2 = new GridData();
        layData2.horizontalSpan = 2;
        c.setLayoutData(layData2);
        
        addField(new StringFieldEditor(SpellChecker.SPELL_CHECKER_ARGUMENTS, TexlipsePlugin.getResourceString("preferenceSpellArgumentsLabel"), c));
    }
    
    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }
}
