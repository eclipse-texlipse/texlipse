package net.sourceforge.texlipse.smartkey;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * The preference page for the smart key support.
 * 
 * @author Markus Maus
 * @author Reza Esmaeili Soumeeh
 * @author Ehsan Baghi
 */
public class SmartKeyPreferencePage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    /**
     * Creates an instance of the "smart key support" -preference page.
     */
    public SmartKeyPreferencePage() {
        super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceSmartKeyDescription"));
    }

    protected void createFieldEditors() {
        addField(new BooleanFieldEditor(TexlipseProperties.SMART_KEY_ENABLE,
                TexlipsePlugin.getResourceString("preferenceSmartKeyEnable"),
                getFieldEditorParent()));
        addField(new SmartKeyListFieldEditor(
                TexlipseProperties.STYLE_COMPLETION_SETTINGS,
                TexlipsePlugin.getResourceString("preferenceSmartKeyLabel"),
                getFieldEditorParent()));
    }

    public void init(IWorkbench workbench) {
    }
}
