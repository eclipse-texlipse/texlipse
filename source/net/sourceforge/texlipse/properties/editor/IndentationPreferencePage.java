/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.properties.editor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.StringListFieldEditor;
import net.sourceforge.texlipse.properties.TexlipsePreferencePage;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * Indentation preferences.
 * 
 * @author Laura Takkinen
 * @author Kimmo Karlsson
 */
public class IndentationPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	 // indentation limits
    private static final int MAX_INDENTATION = 20;
    private static final int MIN_INDENTATION = 0;
	
    /**
     * Construct a new page.
     */
	public IndentationPreferencePage(){
		super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        //setDescription(TexlipsePlugin.getResourceString("preferenceIndentPageDescription"));
	}
    
	/**
     * Creates the page components.
	 */
	protected void createFieldEditors() {
        TexlipsePreferencePage.addSpacer(getFieldEditorParent());
        addField(new BooleanFieldEditor(TexlipseProperties.INDENTATION, TexlipsePlugin.getResourceString("preferenceIndentEnabledLabel"), getFieldEditorParent()));

        String message = TexlipsePlugin.getResourceString("preferenceIndentLevelLabel").replaceFirst("%1", "" + MIN_INDENTATION).replaceFirst("%2", "" + MAX_INDENTATION);
        final IntegerFieldEditor indentationWidth = new IntegerFieldEditor(TexlipseProperties.INDENTATION_LEVEL, message, getFieldEditorParent());
        indentationWidth.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
        indentationWidth.setValidRange(MIN_INDENTATION, MAX_INDENTATION);
        if (TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.INDENTATION_TABS)) {
            indentationWidth.setEnabled(false, getFieldEditorParent());
        }
        addField(indentationWidth);
		
        BooleanFieldEditor indentationTabs = new BooleanFieldEditor(TexlipseProperties.INDENTATION_TABS, TexlipsePlugin.getResourceString("preferenceIndentTabsLabel"), getFieldEditorParent()){
		  @Override
		    protected void valueChanged(boolean oldValue, boolean newValue) {
		        super.valueChanged(oldValue, newValue);
		        if (newValue == true) indentationWidth.setEnabled(false, getFieldEditorParent());
		        else indentationWidth.setEnabled(true, getFieldEditorParent());
		    }  
		};
        addField(indentationTabs);
        TexlipsePreferencePage.addSpacer(getFieldEditorParent());
        addField(new StringListFieldEditor(TexlipseProperties.INDENTATION_ENVS, TexlipsePlugin.getResourceString("preferenceIndentEnvsLabel"), getFieldEditorParent()));
	}

	/**
	 * Page initialization. Does nothing.
	 */
	public void init(IWorkbench workbench) {
	}
}
