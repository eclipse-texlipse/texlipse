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

import java.io.File;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.ProgramRunner;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


/**
 * Builder configuration dialog.
 * Configures all the different ProgramRunner classes.
 * 
 * @author Kimmo Karlsson
 */
public class BuilderConfigDialog extends Dialog {

    protected File lastPath;
    private Text fileField;
    private Text argsField;
    private Label statusField;
    private ProgramRunner builder;
    
    /**
     * @param parentShell
     */
    public BuilderConfigDialog(Shell parentShell, ProgramRunner runner) {
        super(parentShell);
        builder = runner;
    }

    /**
     * Set dialog title when the window is created.
     */
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(TexlipsePlugin.getResourceString("preferenceBuilderDialogTitle"));
    }
    
    /**
     * Check that the config is valid.
     * Close the dialog is the config is valid.
     */
    protected void okPressed() {
        
        String path = fileField.getText();
        if (!checkFile(path)) {
            return;
        }

        builder.setProgramPath(path);
        builder.setProgramArguments(argsField.getText());
        
        setReturnCode(OK);
        close();
    }

    /**
     * Check if the file name field contains a valid value.
     * @param path file path
     * @return true, if path is empty string or absolute path to an existing file
     */
    protected boolean checkFile(String path) {
        if (path != null && path.length() > 0) {
            
            File exec = new File(path);
            if (!exec.isFile() || (!exec.exists() && exec.isFile())) {
                
                setStatus("preferenceBuilderDialogFileNotFound", "");
                return false;
            }
        }
        setStatus("preferenceBuilderDialogFileOk", "");
        return true;
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
     * Called at component creation phase.
     * @return initial status message for the dialog 
     */
    private String resolveStatus() {
        String path = builder.getProgramPath();
        
        if (path == null || path.length() == 0) {
            return TexlipsePlugin.getResourceString("preferenceBuilderDialogFileEmpty");
        }
        
        File exec = new File(path);
        if (!exec.exists() && exec.isFile()) {
            return TexlipsePlugin.getResourceString("preferenceBuilderDialogFileNotFound");
        }
        
        return TexlipsePlugin.getResourceString("preferenceBuilderDialogFileOk");
    }
    
    /**
     * Create the contents of the dialog.
     */
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        
        GridLayout gl = (GridLayout) composite.getLayout();
        gl.numColumns = 2;
        
        Label descrLabel = new Label(composite, SWT.LEFT);
        descrLabel.setText(TexlipsePlugin.getResourceString("preferenceBuilderDialogDescriptionLabel").replaceAll("%s", builder.getDescription()));
        GridData dgd = new GridData(GridData.FILL_HORIZONTAL);
        dgd.horizontalSpan = 2;
        descrLabel.setLayoutData(dgd);

        addFileBrowser(composite);
        
        addArgumentsField(composite);
        
        if (builder.getInputFormat() != null && builder.getInputFormat().length() > 0
                && builder.getOutputFormat() != null && builder.getOutputFormat().length() > 0) {
            addFormatsField(composite);
        }
        
        Group group = new Group(composite, SWT.SHADOW_IN);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)group.getLayoutData()).horizontalSpan = 2;
        group.setLayout(new GridLayout());
        statusField = new Label(group, SWT.LEFT);
        statusField.setText(resolveStatus());
        statusField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        statusField.setToolTipText(TexlipsePlugin.getResourceString("preferenceBuilderDialogStatusTooltip"));
        
        return composite;
    }
    
    /**
     * Creates the executable browsing component.
     * @param composite parent component
     */
    private void addFileBrowser(Composite composite) {

        Label fileLabel = new Label(composite, SWT.LEFT);
        fileLabel.setText(TexlipsePlugin.getResourceString("preferenceBuilderCommandLabel"));
        fileLabel.setToolTipText(TexlipsePlugin.getResourceString("preferenceBuilderCommandTooltip"));
        fileLabel.setLayoutData(new GridData());
        
        Composite browser = new Composite(composite, SWT.NONE);
        browser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout bgl = new GridLayout();
        bgl.numColumns = 2;
        browser.setLayout(bgl);

        fileField = new Text(browser, SWT.SINGLE | SWT.BORDER);
        fileField.setText(builder.getProgramPath());
        fileField.setToolTipText(TexlipsePlugin.getResourceString("preferenceBuilderCommandTooltip"));
        fileField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fileField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                checkFile(fileField.getText());
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
                    } else {
                        lastPath = null;
                    }
                    checkFile(fileField.getText());
                }
                
            }});
    }
    
    /**
     * Adds the program arguments text field to the dialog.
     * @param parent parent component
     */
    private void addArgumentsField(Composite parent) {
        
        Label label = new Label(parent, SWT.LEFT);
        label.setText(TexlipsePlugin.getResourceString("preferenceBuilderArgsLabel"));
        label.setToolTipText(TexlipsePlugin.getResourceString("preferenceBuilderArgsTooltip"));
        label.setLayoutData(new GridData());
        
        argsField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        argsField.setText(builder.getProgramArguments());
        argsField.setToolTipText(TexlipsePlugin.getResourceString("preferenceBuilderArgsTooltip"));
        argsField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }
    
    /**
     * Adds the program arguments text field to the dialog.
     * @param parent parent component
     */
    private void addFormatsField(Composite parent) {

        Composite leftPart = new Composite(parent, SWT.NULL);
        leftPart.setLayoutData(new GridData());
        GridLayout llay = new GridLayout();
        llay.numColumns = 2;
        leftPart.setLayout(llay);
        
        Label label = new Label(leftPart, SWT.LEFT);
        label.setText(TexlipsePlugin.getResourceString("preferenceBuilderInputFormatLabel"));
        label.setLayoutData(new GridData());
        
        Group inputGroup = new Group(leftPart, SWT.SHADOW_IN);
        inputGroup.setLayoutData(new GridData());
        inputGroup.setLayout(new GridLayout());
        
        Label inputLabel = new Label(inputGroup, SWT.LEFT);
        inputLabel.setText("." + builder.getInputFormat());
        inputLabel.setLayoutData(new GridData());
        
        Composite rightPart = new Composite(parent, SWT.NULL);
        rightPart.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout rlay = new GridLayout();
        rlay.numColumns = 2;
        rightPart.setLayout(rlay);
        
        Label label2 = new Label(rightPart, SWT.LEFT);
        label2.setText(TexlipsePlugin.getResourceString("preferenceBuilderOutputFormatLabel"));
        label2.setLayoutData(new GridData());
        
        Group outputGroup = new Group(rightPart, SWT.SHADOW_IN);
        outputGroup.setLayoutData(new GridData());
        outputGroup.setLayout(new GridLayout());
        
        Label outputLabel = new Label(outputGroup, SWT.LEFT);
        outputLabel.setText("." + builder.getOutputFormat());
        outputLabel.setLayoutData(new GridData());
    }        
}
