/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.viewer;


import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * Viewer configuration editor.
 * This dialog can be used for editing old configs or creating new ones.
 * 
 * @author Kimmo Karlsson
 * @author Tor Arne Vestbø
 */
public class ViewerConfigDialog extends Dialog {

    private static final String[] inverseSearchValues = new String[] { ViewerAttributeRegistry.INVERSE_SEARCH_NO, ViewerAttributeRegistry.INVERSE_SEARCH_RUN, ViewerAttributeRegistry.INVERSE_SEARCH_STD };
    
    protected File lastPath;
    private Text fileField;
    private Text nameField;
    private Text argsField; 
	private DDEGroup ddeViewGroup;
	private DDEGroup ddeCloseGroup;
    private Combo formatChooser;
    private Combo inverseChooser;
    private Label statusField;

    private ViewerAttributeRegistry registry;
    private ArrayList nameList;
    private boolean newConfig;

    private Button forwardChoice;


    /**
     * Create a new config editor dialog.
     * @param parentShell shell of the creating component
     */
    public ViewerConfigDialog(Shell parentShell, ViewerAttributeRegistry reg) {
        super(parentShell);
        registry = reg;
        newConfig = false;
    }

    /**
     * Create a new config creator dialog.
     * @param parentShell shell of the creating component
     * @param nameList list of existing viewer configuration names
     */
    public ViewerConfigDialog(Shell parentShell, ArrayList nameList) {
        super(parentShell);
        registry = new ViewerAttributeRegistry();
        registry.setActiveViewer(ViewerAttributeRegistry.VIEWER_NONE);
        this.nameList = nameList;
        newConfig = true;
    }

    /**
     * Set status field message.
     * @param key message text key on resource bundle
     * @param info value of the text parameter (%s)
     */
    protected void setStatus(String key, String info) {
        String msg = "";
        if (key != null && key.length() > 0) {
            msg = TexlipsePlugin.getResourceString(key);
            if (msg.indexOf("%s") >= 0) {
                msg = msg.replaceAll("%s", info);
            }
        }
        statusField.setText(msg);
    }
    
    /**
     * Set dialog title when the window is created.
     */
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        if (newConfig) {
            newShell.setText(TexlipsePlugin.getResourceString("preferenceViewerDialogAddTitle"));
        } else {
            newShell.setText(TexlipsePlugin.getResourceString("preferenceViewerDialogEditTitle"));
        }
    }
    
    /**
     * Should be called after dialog has been closed.
     * @return the viewer registry containing the dialog contents
     */
    public ViewerAttributeRegistry getRegistry() {
        return registry;
    }
    
    /**
     * Check that the config is valid.
     * Close the dialog is the config is valid.
     */
    protected void okPressed() {
        
        if (!validateFields())
            return;
        
        String name = nameField.getText();
        registry.setActiveViewer(nameField.getText());
        registry.setCommand(fileField.getText());
        registry.setArguments(argsField.getText());        
        registry.setDDEViewCommand(ddeViewGroup.command.getText());
        registry.setDDEViewServer(ddeViewGroup.server.getText());
        registry.setDDEViewTopic(ddeViewGroup.topic.getText());
        registry.setDDECloseCommand(ddeCloseGroup.command.getText());
        registry.setDDECloseServer(ddeCloseGroup.server.getText());
        registry.setDDECloseTopic(ddeCloseGroup.topic.getText());
        registry.setFormat(formatChooser.getItem(formatChooser.getSelectionIndex()));
        registry.setInverse(inverseSearchValues[inverseChooser.getSelectionIndex()]);
        registry.setForward(forwardChoice.getSelection());
        
        // Ask user if launch configs should be updated
        try {
            ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
            if (manager != null) {
                ILaunchConfigurationType type = manager.getLaunchConfigurationType(
                    TexLaunchConfigurationDelegate.CONFIGURATION_ID);
                if (type != null) {
                    ILaunchConfiguration[] configs = manager.getLaunchConfigurations(type);
                    if (configs != null) {
                        // Check all configurations
                        int returnCode = 0;
                        MessageDialogWithToggle md = null;
                        for (int i = 0; i < configs.length ; i++) {
                            ILaunchConfiguration c = configs[i];
                            if (c.getType().getIdentifier().equals(TexLaunchConfigurationDelegate.CONFIGURATION_ID)) {
                                if (c.getAttribute("viewerCurrent", "").equals(name)) {
                                    // We've found a config which was based on this viewer 
                                    if (0 == returnCode) {
                                        String message = MessageFormat.format(
                                            TexlipsePlugin.getResourceString("preferenceViewerUpdateConfigurationQuestion"),
                                            new Object[] { c.getName() });
                                        md = MessageDialogWithToggle.openYesNoCancelQuestion(getShell(),
                                            TexlipsePlugin.getResourceString("preferenceViewerUpdateConfigurationTitle"), message,
                                            TexlipsePlugin.getResourceString("preferenceViewerUpdateConfigurationAlwaysApply"),
                                            false, null, null);
                                        
                                        if (md.getReturnCode() == MessageDialogWithToggle.CANCEL)
                                            return;
                                        
                                        returnCode = md.getReturnCode();
                                    } 
                                        
                                    // If answer was yes, update each config with latest values from registry
                                    if (returnCode == IDialogConstants.YES_ID) {
                                        ILaunchConfigurationWorkingCopy workingCopy = c.getWorkingCopy();
                                        workingCopy.setAttributes(registry.asMap());

                                        // We need to set at least one attribute using a one-shot setter method
                                        // because the method setAttributes does not mark the config as dirty. 
                                        // A dirty config is required for a doSave to do anything useful. 
                                        workingCopy.setAttribute("viewerCurrent", name);
                                        workingCopy.doSave();
                                    }
                                    
                                    // Reset return-code if we should be asked again
                                    if (!md.getToggleState()) {
                                        returnCode = 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (CoreException e) {
            // Something wrong with the config, or could not read attributes, so swallow and skip
        }

        setReturnCode(OK);
        close();
    }
    
    /**
     * Create the contents of the dialog.
     * @param parent parent component
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);    
    
        GridLayout gl = (GridLayout) composite.getLayout();
        gl.numColumns = 2;
        
        Label descrLabel = new Label(composite, SWT.LEFT);
        descrLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerDescriptionLabel"));
        GridData dgd = new GridData(GridData.FILL_HORIZONTAL);
        dgd.horizontalSpan = 2;
        descrLabel.setLayoutData(dgd);

        addConfigNameField(composite);
        addFileBrowser(composite);
        addArgumentsField(composite);
        addDDEGroups(composite);
        addFormatChooser(composite);
        addInverseChooser(composite);
        addForwardChooser(composite);
        
        Group group = new Group(composite, SWT.SHADOW_IN);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)group.getLayoutData()).horizontalSpan = 2;
        group.setLayout(new GridLayout());
        statusField = new Label(group, SWT.LEFT);
        statusField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        statusField.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerStatusTooltip"));
        
        return composite;
    }
    
    
    
    /* 
     * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    protected Control createButtonBar(Composite parent) {
        Control ctrl = super.createButtonBar(parent);
        validateFields();
        return ctrl;
    }

    private boolean validateFields() {
        
        boolean everythingOK = true;
        
        String name = nameField.getText();
        if ( nameField.getText() == null ||  nameField.getText().length() == 0) {
            setStatus("preferenceViewerDialogNameEmpty", "");
            everythingOK = false;
        }
        if (formatChooser.getSelectionIndex() == -1) {
            setStatus("preferenceViewerFormatEmpty", "");
            everythingOK = false;
        }
        
        // if adding new configuration, existing name is not valid
        if (nameList != null && nameList.contains(name)) {
            setStatus("preferenceViewerDialogNameExists", name);
            everythingOK = false;
        }
        
        File f = new File(fileField.getText());
        if (fileField.getText().trim().equals("")) {
            setStatus("preferenceViewerDialogFileNoFile", "");
            everythingOK = false;
        }
        else if (!f.exists()) {
            setStatus("preferenceViewerDialogFileNotFound", "");
            everythingOK = false;
        }

        if (!everythingOK) {
            Button b = getButton(IDialogConstants.OK_ID);
            if (b != null) {
                // set button status
                b.setEnabled(false);
            }
            return false;
        }
        
        Button b = getButton(IDialogConstants.OK_ID);
        if (b != null) {
            b.setEnabled(true);
        }
        
        setStatus("preferenceViewerDialogFileOk", "");
        
        return true;
    }

    /**
     * Creates the configuration name text field.
     * @param composite parent component
     */
    private void addConfigNameField(Composite composite) {
        
        Label nameLabel = new Label(composite, SWT.LEFT);
        nameLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerNameLabel"));
        nameLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerNameTooltip"));
        nameLabel.setLayoutData(new GridData());
        
        int rw = newConfig ? 0 : SWT.READ_ONLY;
        nameField = new Text(composite, SWT.SINGLE | SWT.BORDER | rw);
        nameField.setText(registry.getActiveViewer());
        nameField.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerNameTooltip"));
        nameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    /**
     * Creates the executable browsing component.
     * @param composite parent component
     */
    private void addFileBrowser(Composite composite) {

        Label fileLabel = new Label(composite, SWT.LEFT);
        fileLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerCommandLabel"));
        fileLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerCommandTooltip"));
        fileLabel.setLayoutData(new GridData());
        
        Composite browser = new Composite(composite, SWT.NONE);
        browser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout bgl = new GridLayout();
        bgl.numColumns = 2;
        browser.setLayout(bgl);

        fileField = new Text(browser, SWT.SINGLE | SWT.BORDER);
        fileField.setText(registry.getCommand());
        fileField.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerCommandTooltip"));
        fileField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fileField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                validateFields();
            }});
        
        Button browseButton = new Button(browser, SWT.PUSH);
        browseButton.setText(JFaceResources.getString("openBrowse"));
        browseButton.setLayoutData(new GridData());
        browseButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                
                FileDialog dialog = new FileDialog(getShell());
                if (lastPath != null) {
                    if (lastPath.exists()) {
                        dialog.setFilterPath(lastPath.getAbsolutePath());
                    }
                } else {
                    lastPath = new File(fileField.getText());
                    while (lastPath != null && !lastPath.isDirectory()) {
                        lastPath = lastPath.getParentFile();
                    }
                    if (lastPath != null && lastPath.exists()) {
                        dialog.setFilterPath(lastPath.getAbsolutePath());
                    }
                }
                
                String dir = dialog.open();
                if (dir != null) {
                    lastPath = new File(dir.trim());
                    if (lastPath.exists()) {
                        fileField.setText(lastPath.getAbsolutePath());
                        registry.setCommand(fileField.getText());
                    } else {
                        lastPath = null;
                    }
                }
                
            }});
    }

    /**
     * Creates the command arguments field to the dialog.
     * @param composite parent component
     */
    private void addArgumentsField(Composite composite) {
        
        Label argsLabel = new Label(composite, SWT.LEFT);
        argsLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerArgumentLabel"));
        argsLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerArgumentTooltip"));
        argsLabel.setLayoutData(new GridData());
        
        argsField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        argsField.setText(registry.getArguments());
        argsField.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerArgumentTooltip"));
        argsField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
    
    /**
	 * Creates the two groups for DDE view and close
	 * 
	 * @param composite
	 *            parent component
	 */
	private void addDDEGroups(Composite composite) {
		
		ddeViewGroup = new DDEGroup(composite,
				TexlipsePlugin.getResourceString("preferenceViewerDDEViewLabel"),
				TexlipsePlugin.getResourceString("preferenceViewerDDEViewTooltip"));
		ddeViewGroup.command.setText(registry.getDDEViewCommand());
		ddeViewGroup.server.setText(registry.getDDEViewServer());
		ddeViewGroup.topic.setText(registry.getDDEViewTopic());
		
		ddeCloseGroup = new DDEGroup(composite,
				TexlipsePlugin.getResourceString("preferenceViewerDDECloseLabel"),
				TexlipsePlugin.getResourceString("preferenceViewerDDECloseTooltip"));
		ddeCloseGroup.command.setText(registry.getDDECloseCommand());
		ddeCloseGroup.server.setText(registry.getDDECloseServer());
		ddeCloseGroup.topic.setText(registry.getDDECloseTopic());
		
		// Only show DDE configuration if on Win32
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
        	ddeViewGroup.setVisible(true);
        	ddeCloseGroup.setVisible(true);
        }
	}

    /**
     * Creates the file format chooser to the page.
     * @param composite parent component
     */
    private void addFormatChooser(Composite composite) {
        
        Label formatLabel = new Label(composite, SWT.LEFT);
        formatLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerFormatLabel"));
        formatLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerFormatTooltip"));
        formatLabel.setLayoutData(new GridData());
        
        formatChooser = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        formatChooser.setLayoutData(new GridData());
        formatChooser.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerFormatTooltip"));
        formatChooser.setItems(registry.getFormatList());
        formatChooser.select(formatChooser.indexOf(registry.getFormat()));
    }

    /**
     * Creates the additional controls of the page.
     * @param parent parent component
     */
    private void addInverseChooser(Composite parent) {
        
        Label label = new Label(parent, SWT.LEFT);
        label.setText(TexlipsePlugin.getResourceString("preferenceViewerInverseLabel"));
        label.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerInverseTooltip"));
        label.setLayoutData(new GridData());
        
        String[] list = new String[] {
                TexlipsePlugin.getResourceString("preferenceViewerInverseSearchNo"),
                TexlipsePlugin.getResourceString("preferenceViewerInverseSearchRun"),
                TexlipsePlugin.getResourceString("preferenceViewerInverseSearchStd")
        };
        
        // find out which option to choose by default
        int index = inverseSearchValues.length - 1;
        for (; index > 0 && !inverseSearchValues[index].equals(registry.getInverse()); index--) {}
        
        
        inverseChooser = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        inverseChooser.setLayoutData(new GridData());
        inverseChooser.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerInverseTooltip"));
        inverseChooser.setItems(list);
        inverseChooser.select(index);
    }
    
    /**
     * Creates the forward search support -checkbox.
     * @param parent parent component
     */
    private void addForwardChooser(Composite parent) {

        forwardChoice = new Button(parent, SWT.CHECK);
        forwardChoice.setText(TexlipsePlugin.getResourceString("preferenceViewerForwardLabel"));
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        forwardChoice.setLayoutData(gd);
        forwardChoice.setSelection(registry.getForward());
    }
    

	private class DDEGroup extends Composite {

		// Public members since the class is private to the dialog
		public Text command;
		public Text server;
		public Text topic;

		public DDEGroup(Composite parent, String name, String toolTip) {
			super(parent, SWT.NONE);
			
			setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		    ((GridData)getLayoutData()).horizontalSpan = 2;
		    setLayout( new GridLayout());
		    	    
   		    Group group = new Group(this, SWT.SHADOW_IN);
	        group.setText(name);
	        group.setToolTipText(toolTip);
   		    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	        group.setLayout(new GridLayout(4, false));

			Label ddeCommandLabel = new Label(group, SWT.LEFT);
			ddeCommandLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerDDECommandLabel"));
			ddeCommandLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDECommandTooltip"));
			ddeCommandLabel.setLayoutData(new GridData());

			command = new Text(group, SWT.SINGLE | SWT.BORDER);
			command.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDECommandTooltip"));
			command.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			((GridData) command.getLayoutData()).horizontalSpan = 3;

			Label ddeServerLabel = new Label(group, SWT.LEFT);
			ddeServerLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerDDEServerLabel"));
			ddeServerLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDEServerTooltip"));
			ddeServerLabel.setLayoutData(new GridData());

			server = new Text(group, SWT.SINGLE | SWT.BORDER);
			server.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDEServerTooltip"));
			server.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Label ddeTopicLabel = new Label(group, SWT.LEFT);
			ddeTopicLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerDDETopicLabel"));
			ddeTopicLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDETopicTooltip"));
			ddeTopicLabel.setLayoutData(new GridData());

			topic = new Text(group, SWT.SINGLE | SWT.BORDER);
			topic.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDETopicTooltip"));
			topic.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			
			setVisible(false);
		}
	}
}

