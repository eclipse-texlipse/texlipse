/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.properties;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.ColorManager;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * The page to set syntax highlighting colors.
 * 
 * @author kimmo
 */
public class ColoringPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage {

    /**
     * Creates an instance of the "syntax highlighting colors" -preference page.
     */
    public ColoringPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceColorPageDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        addField(new ColorFieldEditor(ColorManager.DEFAULT, TexlipsePlugin.getResourceString("preferenceColorTextLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.COMMAND, TexlipsePlugin.getResourceString("preferenceColorCommandLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.CURLY_BRACKETS, TexlipsePlugin.getResourceString("preferenceColorArgumentLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.SQUARE_BRACKETS, TexlipsePlugin.getResourceString("preferenceColorOptionalLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.EQUATION, TexlipsePlugin.getResourceString("preferenceColorMathLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.COMMENT, TexlipsePlugin.getResourceString("preferenceColorCommentLabel"), getFieldEditorParent()));
        //addField(new ColorFieldEditor(ColorManager.TEX_NUMBER, TexlipsePlugin.getResourceString("preferenceColorNumberLabel"), getFieldEditorParent()));
        addField(new ColorFieldEditor(ColorManager.TEX_SPECIAL, TexlipsePlugin.getResourceString("preferenceColorSpeLabel"), getFieldEditorParent()));
    }
    
    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }
}
