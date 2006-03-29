package net.sourceforge.texlipse.properties;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.smartkey.SmartKeyListFieldEditor;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page for defining style completions
 * 
 * @author Oskar Ojala
 */
public class StyleCompletionPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    /**
     * Creates an instance of the preference page.
     */
    public StyleCompletionPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceSmartKeyDescription"));
    }

    /**
     * Creates the property editing UI components of this page.
     */
    protected void createFieldEditors() {
        // TODO SmartKeyListFieldEditor has an edit button, the
        // lower one has an import dialog which is irrelevant for this
        // but could be useful in general
        addField(new SmartKeyListFieldEditor(
                TexlipseProperties.STYLE_COMPLETION_SETTINGS,
                TexlipsePlugin.getResourceString("preferenceSmartKeyLabel"),
                getFieldEditorParent()));

//        addField(new KeyValueListFieldEditor(
//                TexlipseProperties.STYLE_COMPLETION_SETTINGS,
//                TexlipsePlugin.getResourceString("preferenceSmartKeyLabel"),
//                getFieldEditorParent()));
    }

    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }

}
