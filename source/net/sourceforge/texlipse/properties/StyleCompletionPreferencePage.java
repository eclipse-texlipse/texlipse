package net.sourceforge.texlipse.properties;

import net.sourceforge.texlipse.TexlipsePlugin;

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
        addField(new KeyValueListFieldEditor(
                TexlipseProperties.STYLE_COMPLETION_SETTINGS,
                TexlipsePlugin.getResourceString("preferenceSmartKeyLabel"),
                getFieldEditorParent()));
    }

    /**
     * Nothing to do.
     */
    public void init(IWorkbench workbench) {
    }

}
