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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


/**
 * The tab containing Latex previewer configuration.
 * 
 * @author Kimmo Karlsson
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
        commandField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                updateLaunchConfigurationDialog();
            }});
        
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
        argumentField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                updateLaunchConfigurationDialog();
            }});
        
        setControl(composite);
    }

    /**
     * Handle the change of selected item in the viewer list.
     * @param selectionIndex currently selected item
     */
    private void updateFields(int selectionIndex) {

        registry.setCommand(commandField.getText());
        registry.setArguments(argumentField.getText());

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
        
        updateLaunchConfigurationDialog();
    }
    
    /**
     * Initializes the given launch configuration with
     * default values for this tab. This method
     * is called when a new launch configuration is created
     * such that the configuration can be initialized with
     * meaningful values. This method may be called before this
     * tab's control is created.
     * 
     * @param configuration launch configuration
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {

        String viewer = registry.getActiveViewer();
        configuration.setAttribute(ViewerAttributeRegistry.VIEWER_CURRENT, viewer);
        configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_COMMAND, registry.getCommand());
        configuration.setAttribute(viewer + ViewerAttributeRegistry.ATTRIBUTE_ARGUMENTS, registry.getArguments());
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
}
