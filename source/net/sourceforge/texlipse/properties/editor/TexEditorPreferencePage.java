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
import net.sourceforge.texlipse.properties.TexlipseHelpIds;
import net.sourceforge.texlipse.properties.TexlipsePreferencePage;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.IWorkbenchHelpSystem;


/**
 * A preference page that is contributed to the Preferences dialog. 
 * 
 * Preferences are stored in the preference store that belongs to
 * the main plug-in class.
 * 
 * @author Kimmo Karlsson
 */
public class TexEditorPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
    
    // word wrap limits
    private static final int MAX_WRAP_LENGTH = 1000;
    private static final int MIN_WRAP_LENGTH = 10;
    // content assist delay limits
    private static final int MAX_COMPLETION_DELAY = 10000;
    private static final int MIN_COMPLETION_DELAY = 0;
    // auto parsing delay limits
    private static final int MAX_AUTO_DELAY = 30000;
    private static final int MIN_AUTO_DELAY = 0;
    
    private IWorkbench workbench;
    
    /**
     * Creates the preference page.
	 */
	public TexEditorPreferencePage() {
		super(GRID);
		setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
		//setDescription(TexlipsePlugin.getResourceString("preferenceEditorPageDescription"));
	}

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
	public void createFieldEditors() {
        
        TexlipsePreferencePage.addSpacer(getFieldEditorParent());
        addField(new BooleanFieldEditor(TexlipseProperties.TEX_COMPLETION, TexlipsePlugin.getResourceString("preferenceTexCompletion"), getFieldEditorParent()));
        String completionMessage = TexlipsePlugin.getResourceString("preferenceTexCompletionDelay").replaceFirst("%1", "" + MIN_COMPLETION_DELAY).replaceFirst("%2", "" + MAX_COMPLETION_DELAY);
        IntegerFieldEditor completionDelay = new IntegerFieldEditor(TexlipseProperties.TEX_COMPLETION_DELAY, completionMessage, getFieldEditorParent());
        completionDelay.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
        completionDelay.setValidRange(MIN_COMPLETION_DELAY, MAX_COMPLETION_DELAY);
        addField(completionDelay);
        //TexlipsePreferencePage.addSpacer(getFieldEditorParent());
        
        // auto \item completion
        addField(new BooleanFieldEditor(TexlipseProperties.TEX_ITEM_COMPLETION,
                TexlipsePlugin.getResourceString("preferenceTexItemCompletion"),
                getFieldEditorParent()));
        Label itemLabel = new Label(getFieldEditorParent(), SWT.LEFT | SWT.WRAP);
        itemLabel.setText(TexlipsePlugin.getResourceString("preferenceTexItemCompletionText"));        
        //TexlipsePreferencePage.addSpacer(getFieldEditorParent());
                
        // auto parsing
        addField(new BooleanFieldEditor(TexlipseProperties.AUTO_PARSING,
                TexlipsePlugin.getResourceString("preferenceAutoParsing"),
                getFieldEditorParent()));
        String autoParsingMessage = TexlipsePlugin.getResourceString(
                "preferenceAutoParsingDelay").replaceFirst("%1",
                "" + MIN_AUTO_DELAY).replaceFirst("%2", "" + MAX_AUTO_DELAY);
        IntegerFieldEditor autoDelay = new IntegerFieldEditor(TexlipseProperties.AUTO_PARSING_DELAY,
                autoParsingMessage, getFieldEditorParent());
        autoDelay.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
        autoDelay.setValidRange(MIN_AUTO_DELAY, MAX_AUTO_DELAY);
        addField(autoDelay);
        
        // Check for missing sections
        addField(new BooleanFieldEditor(TexlipseProperties.SECTION_CHECK,
                TexlipsePlugin.getResourceString("preferenceSectionCheck"),
                getFieldEditorParent()));

        // Mark occurences (references and environments)
        addField(new BooleanFieldEditor(TexlipseProperties.TEX_EDITOR_ANNOTATATIONS,
                TexlipsePlugin.getResourceString("preferencesEditorHighlighting"),
                getFieldEditorParent()));
        
        TexlipsePreferencePage.addSpacer(getFieldEditorParent());
        
        // word wrapping
        Group group = new Group(getFieldEditorParent(), SWT.NONE);
        group.setText(TexlipsePlugin.getResourceString("preferenceWrapping"));
        group.setLayout(new GridLayout());
        GridData layData = new GridData(GridData.FILL_HORIZONTAL);
        layData.horizontalSpan = 2;
        group.setLayoutData(layData);
        
        Composite wordWrapParent = getFieldEditorParent(group);

        // default on/off wrapping
        addField(new BooleanFieldEditor(TexlipseProperties.WORDWRAP_DEFAULT, TexlipsePlugin.getResourceString("preferenceWrappingDefault"), wordWrapParent));
        TexlipsePreferencePage.addSpacer(wordWrapParent);
        
        String message = TexlipsePlugin.getResourceString("preferenceWrapLineLength").replaceFirst("%1", ""+MIN_WRAP_LENGTH).replaceFirst("%2", ""+MAX_WRAP_LENGTH);
        IntegerFieldEditor wordWrapLength = new IntegerFieldEditor(TexlipseProperties.WORDWRAP_LENGTH, message, wordWrapParent);
        wordWrapLength.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
        wordWrapLength.setValidRange(MIN_WRAP_LENGTH, MAX_WRAP_LENGTH);
        addField(wordWrapLength);
        
        IWorkbenchHelpSystem helpsystem = workbench.getHelpSystem();
        helpsystem.setHelp(wordWrapLength.getTextControl(wordWrapParent), TexlipseHelpIds.WRAP_LENGTH);
        
        //WorkbenchHelp.setHelp(wordWrapLength.getTextControl(wordWrapParent), TexlipseHelpIds.WRAP_LENGTH);
        
        TexlipsePreferencePage.addSpacer(wordWrapParent);
        Label label = new Label(wordWrapParent, SWT.LEFT | SWT.WRAP);
        label.setText(TexlipsePlugin.getResourceString("preferenceWrapSoftWarning"));
        GridData lgl = new GridData(GridData.FILL_HORIZONTAL);
        lgl.horizontalSpan = 2;
        label.setLayoutData(lgl);
        addField(new RadioGroupFieldEditor(TexlipseProperties.WORDWRAP_TYPE, TexlipsePlugin.getResourceString("preferenceWrapType"), 1, 
                                           new String[][] {//{ TexlipsePlugin.getResourceString("preferenceWrapNoneLabel"), TexlipseProperties.WORDWRAP_TYPE_NONE },
                                                           { TexlipsePlugin.getResourceString("preferenceWrapSoftLabel"), TexlipseProperties.WORDWRAP_TYPE_SOFT }, 
                                                           { TexlipsePlugin.getResourceString("preferenceWrapHardLabel"), TexlipseProperties.WORDWRAP_TYPE_HARD }},
                                                           getFieldEditorParent(group)));
    }

    /**
     * Encapsulate group members to own components.
     * This is necessary to get the layout right.
     * @param group field editor group
     * @return parent component for a field editor
     */
    private Composite getFieldEditorParent(Group group) {
        Composite parent = new Composite(group, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        parent.setLayout(layout);
        parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return parent;
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
        this.workbench = workbench;
        // nothing to do
	}
}
