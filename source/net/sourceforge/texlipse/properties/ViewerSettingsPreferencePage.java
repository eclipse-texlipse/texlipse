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
import net.sourceforge.texlipse.viewer.util.FileLocationServer;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;


/**
 * Viewer preferences.
 * 
 * @author Kimmo Karlsson
 */
public class ViewerSettingsPreferencePage extends FieldEditorPreferencePage
		implements IWorkbenchPreferencePage {

	// port limits
    private static final int MAX_PORT = 65536;
    private static final int MIN_PORT = 1;
	
    /**
     * Construct a new page.
     */
	public ViewerSettingsPreferencePage(){
		super(GRID);
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceViewerPageDescription"));
	}

	/**
     * Creates the page components.
	 */
	protected void createFieldEditors() {
        
        TexlipsePreferencePage.addSpacer(2, getFieldEditorParent());
        ViewerListFieldEditor vfe = new ViewerListFieldEditor(TexlipsePlugin.getResourceString("preferenceViewerConfigsLabel"), getFieldEditorParent());
        addField(vfe);
        WorkbenchHelp.setHelp(vfe.getListControl(getFieldEditorParent()), TexlipseHelpIds.VIEWER_LIST);

        // Depricated. Eclipse has it's own mechanism for auto rebuild.
        //TexlipsePreferencePage.addSpacer(2, getFieldEditorParent());
        //TexlipsePreferencePage.addSeparator(2, getFieldEditorParent());
        //addField(new BooleanFieldEditor(TexlipseProperties.BUILD_BEFORE_VIEW, TexlipsePlugin.getResourceString("preferenceViewerBuildLabel"), getFieldEditorParent()));
        
        TexlipsePreferencePage.addSpacer(2, getFieldEditorParent());
        TexlipsePreferencePage.addSeparator(2, getFieldEditorParent());
        
        TexlipsePreferencePage.addSpacer(2, getFieldEditorParent());
        TexlipsePreferencePage.addLabelField(2, TexlipsePlugin.getResourceString("preferenceViewerPortLabel"), getFieldEditorParent());
        String message = TexlipsePlugin.getResourceString("preferenceViewerConfigsPort").replaceFirst("%1", "" + MIN_PORT).replaceFirst("%2", "" + MAX_PORT);
        IntegerFieldEditor port = new IntegerFieldEditor(TexlipseProperties.FILE_LOCATION_PORT, message, getFieldEditorParent());
        port.setValidateStrategy(IntegerFieldEditor.VALIDATE_ON_KEY_STROKE);
        port.setValidRange(MIN_PORT, MAX_PORT);
        addField(port);
        
        addField(new BooleanFieldEditor(TexlipseProperties.BUILDER_FORCE_RETURN_FOCUS, TexlipsePlugin.getResourceString("forceReturnFocusOnInverseSearch"), getFieldEditorParent()));
        
        addField(new BooleanFieldEditor(TexlipseProperties.BUILDER_RETURN_FOCUS, TexlipsePlugin.getResourceString("preferenceViewerReturnFocusLabel"), getFieldEditorParent()));
        
        WorkbenchHelp.setHelp(port.getTextControl(getFieldEditorParent()), TexlipseHelpIds.VIEWER_PORT);
    }

    /**
     * Run when ok-button is pressed.
     * Here the preferences get saved.
     * We also check for a change in the file location server port number.
     * If there is a change we stop the old server. It gets started when needed.
     */
    public boolean performOk() {
        int oldPort = getPreferenceStore().getInt(TexlipseProperties.FILE_LOCATION_PORT);
        boolean ok = super.performOk();
        int newPort = getPreferenceStore().getInt(TexlipseProperties.FILE_LOCATION_PORT);
        if (newPort != oldPort) {
            FileLocationServer.getInstance().stop();
        }
        return ok;
    }

    /**
	 * Page initialization. Does nothing.
	 */
	public void init(IWorkbench workbench) {
	}
}
