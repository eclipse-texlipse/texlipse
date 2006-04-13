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

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * The tab containing Latex previewer configuration.
 * 
 * @author Kimmo Karlsson
 * @author Tor Arne Vestbø
 */
public class TexLaunchConfigurationTab extends AbstractLaunchConfigurationTab {

    // list of viewers
    private Combo choiceCombo;
    
    // the viewer command
    private Text commandField;
    
    // the viewer arguments
    private Text argumentField;

    // button to launch the file browser
    private Button commandBrowserButton;

    // a copy of the viewer attributes
    private ViewerAttributeRegistry registry;
    
    private DDEGroup ddeCloseGroup;
    private DDEGroup ddeViewGroup;
    
    // true if we are populating the whole tab with new information
    private boolean isUpdatingFields = false;
    
    /**
     * Construct a new ConfigurationTab.
     */
    public TexLaunchConfigurationTab() {
        registry = new ViewerAttributeRegistry();
    }

    /**
     * Creates the top level control for this launch configuration
     * tab under the given parent composite.  This method is called once on
     * tab creation, after <code>setLaunchConfigurationDialog</code>
     * is called.
     * <p>
     * Implementors are responsible for ensuring that
     * the created control can be accessed via <code>getControl</code>
     * </p>
     *
     * @param parent the parent composite
     */
    public void createControl(Composite parent) {
        
        Composite composite = new Composite(parent, SWT.FILL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        createVerticalSpacer(composite, 3);
        
        Label choiceLabel = new Label(composite, SWT.LEFT);
        choiceLabel.setText(TexlipsePlugin.getResourceString("launchTabChoiceLabel"));
        choiceLabel.setToolTipText(TexlipsePlugin.getResourceString("launchTabChoiceTooltip"));
        choiceLabel.setLayoutData(new GridData());
        
        choiceCombo = new Combo(composite, SWT.DROP_DOWN);
        choiceCombo.setToolTipText(TexlipsePlugin.getResourceString("launchTabChoiceTooltip"));
        GridData cgd = new GridData();
        cgd.horizontalSpan = 2;
        choiceCombo.setLayoutData(cgd);
        String[] viewers = registry.getViewerList();
        choiceCombo.setItems(viewers);
        choiceCombo.select(registry.getActiveViewerIndex(viewers));
        choiceCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateFields(choiceCombo.getSelectionIndex());
            }});
        
        createVerticalSpacer(composite, 3);
        
        Label commandLabel = new Label(composite, SWT.LEFT);
        commandLabel.setText(TexlipsePlugin.getResourceString("launchTabCommandLabel"));
        commandLabel.setToolTipText(TexlipsePlugin.getResourceString("launchTabCommandTooltip"));
        commandLabel.setLayoutData(new GridData());
        
        commandField = new Text(composite, SWT.SINGLE | SWT.WRAP | SWT.BORDER);
        commandField.setToolTipText(TexlipsePlugin.getResourceString("launchTabCommandTooltip"));
        commandField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        commandField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                File f = new File(commandField.getText());
                if (!f.exists()) {
                    setErrorMessage(TexlipsePlugin.getResourceString("launchTabCommandError"));
                }
            }});
        commandField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
        
        commandBrowserButton = new Button(composite, SWT.PUSH);
        commandBrowserButton.setText(JFaceResources.getString("openChange"));
        commandBrowserButton.setToolTipText(TexlipsePlugin.getResourceString("launchTabCommandTooltip"));
        commandBrowserButton.setLayoutData(new GridData());
        commandBrowserButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                FileDialog dialog = new FileDialog(commandBrowserButton.getShell(), SWT.OPEN);
                String file = dialog.open();
                if (file != null && file.length() > 0) {
                    File f = new File(file);
                    if (f.exists()) {
                        commandField.setText(file);
                    }
                }
            }});
        
        createVerticalSpacer(composite, 3);
        
        Label argumentLabel = new Label(composite, SWT.LEFT);
        argumentLabel.setText(TexlipsePlugin.getResourceString("launchTabArgumentsLabel"));
        argumentLabel.setToolTipText(TexlipsePlugin.getResourceString("launchTabArgumentsTooltip"));
        argumentLabel.setLayoutData(new GridData());
        
        argumentField = new Text(composite, SWT.SINGLE | SWT.WRAP | SWT.BORDER);
        argumentField.setToolTipText(TexlipsePlugin.getResourceString("launchTabArgumentsTooltip"));
        argumentField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        argumentField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateLaunchConfigurationDialog();
			}
		});
        
        addDDEGroups(composite);
                
        setControl(composite);
    }

    /**
     * Handle the change of selected item in the viewer list.
     * @param selectionIndex currently selected item
     */
    private void updateFields(int selectionIndex) {

    	isUpdatingFields = true;
    	
        registry.setCommand(commandField.getText());
        registry.setArguments(argumentField.getText());
        registry.setDDEViewCommand(ddeViewGroup.command.getText());
        registry.setDDEViewServer(ddeViewGroup.server.getText());
        registry.setDDEViewTopic(ddeViewGroup.topic.getText());
        registry.setDDECloseCommand(ddeCloseGroup.command.getText());
        registry.setDDECloseServer(ddeCloseGroup.server.getText());
        registry.setDDECloseTopic(ddeCloseGroup.topic.getText());

        String viewer = choiceCombo.getItem(selectionIndex);
        registry.setActiveViewer(viewer);
        
        String command = registry.getCommand();
        if (command == null) {
            command = "";
        }
        commandField.setText(command);

        String arguments = registry.getArguments();
        if (arguments == null) {
            arguments = "";
        }
        argumentField.setText(arguments);
        
        ddeViewGroup.command.setText(registry.getDDEViewCommand());
		ddeViewGroup.server.setText(registry.getDDEViewServer());
		ddeViewGroup.topic.setText(registry.getDDEViewTopic());
		
		ddeCloseGroup.command.setText(registry.getDDECloseCommand());
		ddeCloseGroup.server.setText(registry.getDDECloseServer());
		ddeCloseGroup.topic.setText(registry.getDDECloseTopic());
        
        updateLaunchConfigurationDialog();
        
        isUpdatingFields = false;
    }
    
    /**
     * Initializes the given launch configuration with
     * default values for this tab. This method
     * is called when a new launch configuration is created
     * such that the configuration can be initialized with
     * meaningful values. This method may be called before this
     * tab's control is created.
     * 
     * If the configuration parameter contains an attribute named
     * 'viewerCurrent', the tab is initialized with the default values
     * for the given viewer. The given viewer is expected to exist.
     * 
     * @param configuration launch configuration
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        try {
            String viewer = configuration.getAttribute("viewerCurrent", registry.getActiveViewer());
            registry.setActiveViewer(viewer);
            
            configuration.setAttribute(ViewerAttributeRegistry.VIEWER_CURRENT, viewer);
            configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_COMMAND, registry.getCommand());
            configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_ARGUMENTS, registry.getArguments());
            configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_DDE_VIEW_COMMAND, registry.getDDEViewCommand());
            configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_DDE_VIEW_SERVER, registry.getDDEViewServer());
            configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_DDE_VIEW_TOPIC, registry.getDDEViewTopic());
            configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_DDE_CLOSE_COMMAND, registry.getDDECloseCommand());
            configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_DDE_CLOSE_SERVER, registry.getDDECloseServer());
            configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_DDE_CLOSE_TOPIC, registry.getDDECloseTopic());  
        } catch (CoreException e) {
            TexlipsePlugin.log("Initializing launch configuration", e);
        }
    }

    /**
     * Initializes this tab's controls with values from the given
     * launch configuration. This method is called when
     * a configuration is selected to view or edit, after this
     * tab's control has been created.
     * 
     * @param configuration launch configuration
     */
    public void initializeFrom(ILaunchConfiguration configuration) {
        
        try {
            registry.setValues(configuration.getAttributes());
            
            String[] viewers = registry.getViewerList();
            choiceCombo.setItems(viewers);
            choiceCombo.select(registry.getActiveViewerIndex(viewers));
            
            commandField.setText(registry.getCommand());
            argumentField.setText(registry.getArguments());
            
            ddeViewGroup.command.setText(registry.getDDEViewCommand());
    		ddeViewGroup.server.setText(registry.getDDEViewServer());
    		ddeViewGroup.topic.setText(registry.getDDEViewTopic());
    		
    		ddeCloseGroup.command.setText(registry.getDDECloseCommand());
    		ddeCloseGroup.server.setText(registry.getDDECloseServer());
    		ddeCloseGroup.topic.setText(registry.getDDECloseTopic());

        } catch (CoreException e) {
            TexlipsePlugin.log("Reading launch configuration", e);
        }
    }

    /**
     * Copies values from this tab into the given 
     * launch configuration.
     * 
     * @param configuration launch configuration
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        registry.setActiveViewer(choiceCombo.getItem(choiceCombo.getSelectionIndex()));
        registry.setCommand(commandField.getText());
        registry.setArguments(argumentField.getText());
        registry.setDDEViewCommand(ddeViewGroup.command.getText());
        registry.setDDEViewServer(ddeViewGroup.server.getText());
        registry.setDDEViewTopic(ddeViewGroup.topic.getText());
        registry.setDDECloseCommand(ddeCloseGroup.command.getText());
        registry.setDDECloseServer(ddeCloseGroup.server.getText());
        registry.setDDECloseTopic(ddeCloseGroup.topic.getText());
        configuration.setAttributes(registry.asMap());
    }

    /**
     * Returns the image of this tab.
     * @return the image of this tab
     */
    public Image getImage() {
        return TexlipsePlugin.getImage("sample");
    }
    
    /**
     * Returns the name of this tab.
     * 
     * @return the name of this tab
     */
    public String getName() {
        return TexlipsePlugin.getResourceString("launchTabTitle");
    }
    
    /**
     * Only update the fields if we are done filling them in 
     */
    protected void updateLaunchConfigurationDialog() {
    	if (!isUpdatingFields)
    		super.updateLaunchConfigurationDialog();    	
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
		
		ddeCloseGroup = new DDEGroup(composite,
				TexlipsePlugin.getResourceString("preferenceViewerDDECloseLabel"),
				TexlipsePlugin.getResourceString("preferenceViewerDDECloseTooltip"));
		
		//	Only show DDE configuration if on Win32
        if (Platform.getOS().equals(Platform.OS_WIN32)) {
        	ddeViewGroup.setVisible(true);
        	ddeCloseGroup.setVisible(true);
        }
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
			command.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					updateLaunchConfigurationDialog();
				}
			});
			((GridData) command.getLayoutData()).horizontalSpan = 3;

			Label ddeServerLabel = new Label(group, SWT.LEFT);
			ddeServerLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerDDEServerLabel"));
			ddeServerLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDEServerTooltip"));
			ddeServerLabel.setLayoutData(new GridData());

			server = new Text(group, SWT.SINGLE | SWT.BORDER);
			server.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDEServerTooltip"));
			server.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			server.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					updateLaunchConfigurationDialog();
				}
			});
			
			Label ddeTopicLabel = new Label(group, SWT.LEFT);
			ddeTopicLabel.setText(TexlipsePlugin.getResourceString("preferenceViewerDDETopicLabel"));
			ddeTopicLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDETopicTooltip"));
			ddeTopicLabel.setLayoutData(new GridData());
			
			topic = new Text(group, SWT.SINGLE | SWT.BORDER);
			topic.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerDDETopicTooltip"));
			topic.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			topic.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					updateLaunchConfigurationDialog();
				}
			});
			
			setVisible(false);
		}
	}
}
