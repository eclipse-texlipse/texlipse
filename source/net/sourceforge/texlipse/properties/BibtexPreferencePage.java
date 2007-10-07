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

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * A preference page that is contributed to the Preferences dialog. 
 * 
 * Preferences are stored in the preference store that belongs to
 * the main plug-in class.
 * 
 * @author Kimmo Karlsson
 */
public class BibtexPreferencePage
extends FieldEditorPreferencePage
implements IWorkbenchPreferencePage {
    
    //delay limits
    private static final int MAX_DELAY = 10000;
    private static final int MIN_DELAY = 0;
    
    /**
     * Creates the preference page.
     */
    public BibtexPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        //setDescription(TexlipsePlugin.getResourceString("preferenceBibtexPageDescription"));
    }
    
    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
    public void createFieldEditors() {
        TexlipsePreferencePage.addSpacer(3, getFieldEditorParent());
        addField(new BooleanFieldEditor(TexlipseProperties.BIB_COMPLETION, TexlipsePlugin.getResourceString("preferenceBibCompletion"), getFieldEditorParent()));
        String message = TexlipsePlugin.getResourceString("preferenceBibCompletionDelay").replaceFirst("%1", "" + MIN_DELAY).replaceFirst("%2", "" + MAX_DELAY);
        IntegerFieldEditor delay = new IntegerFieldEditor(TexlipseProperties.BIB_COMPLETION_DELAY, message, getFieldEditorParent());
        delay.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
        delay.setValidRange(MIN_DELAY, MAX_DELAY);
        addField(delay);
        
        addField(new BooleanFieldEditor(TexlipseProperties.BIB_CODE_FOLDING, TexlipsePlugin.getResourceString("preferenceBibCodeFolding"), getFieldEditorParent()));
        addField(new BooleanFieldEditor(TexlipseProperties.BIB_FOLD_INITIAL, TexlipsePlugin.getResourceString("preferenceBibFoldInitial"), getFieldEditorParent()));
        //addField(new BooleanFieldEditor(TexlipseProperties.BIB_STRING, TexlipsePlugin.getResourceString("preferenceBibString"), getFieldEditorParent()));        
        //addField(new BooleanFieldEditor(TexlipseProperties.BIB_FREQSORT, TexlipsePlugin.getResourceString("preferenceBibFreqSort"), getFieldEditorParent()));
    }
    
    /**
     * Initializes this preference page for the given workbench.
     * <p>
     * This method is called automatically as the preference page is being created
     * and initialized. Clients must not call this method.
     * </p>
     *
     * @param workbench the workbench
     */
    public void init(IWorkbench workbench) {
        // nothing to do
    }
}
