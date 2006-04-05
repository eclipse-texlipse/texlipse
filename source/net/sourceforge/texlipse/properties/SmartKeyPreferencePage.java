package net.sourceforge.texlipse.properties;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The preference page for vim-latex -style smart key support, comes
 * under the Editor-menu 
 * 
 * @author Oskar Ojala
 */
public class SmartKeyPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    /**
     * Creates a smart key preference page
     */
    public SmartKeyPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceSmartKeyDescription"));
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditorPreferencePage#createFieldEditors()
     */
    protected void createFieldEditors() {

        // Smart backspace
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_BACKSPACE, TexlipsePlugin.getResourceString("preferenceSmartBackspace"), getFieldEditorParent()));
        Label backspaceLabel = new Label(getFieldEditorParent(),SWT.LEFT | SWT.WRAP);
        backspaceLabel.setText(TexlipsePlugin.getResourceString("preferenceSmartBackspace"));
        
        // Smart quotes
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_QUOTES, TexlipsePlugin.getResourceString("preferenceTexReplaceQuotes"), getFieldEditorParent()));
        Label quotesLabel = new Label(getFieldEditorParent(),SWT.LEFT | SWT.WRAP);
        quotesLabel.setText(TexlipsePlugin.getResourceString("preferenceTexReplaceQuotesText"));

        // Smart parens
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_PARENS, TexlipsePlugin.getResourceString("preferenceTexBracketCompletion"), getFieldEditorParent()));
        Label bracketLabel = new Label(getFieldEditorParent(),SWT.LEFT | SWT.WRAP);
        bracketLabel.setText(TexlipsePlugin.getResourceString("preferenceTexBracketCompletionText"));

        
        // Smart \ldots
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_LDOTS, TexlipsePlugin.getResourceString("preferenceSmartLdots"), getFieldEditorParent()));
        
        
//        addField(new BooleanFieldEditor(TexlipseProperties.SMART_KEY_ENABLE,
//                TexlipsePlugin.getResourceString("preferenceSmartKeyEnable"),
//                getFieldEditorParent()));
//        addField(new SmartKeyListFieldEditor(
//                TexlipseProperties.STYLE_COMPLETION_SETTINGS,
//                TexlipsePlugin.getResourceString("preferenceSmartKeyLabel"),
//                getFieldEditorParent()));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    public void init(IWorkbench workbench) {
    }
}
