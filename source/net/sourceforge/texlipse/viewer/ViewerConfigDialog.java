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
import java.util.ArrayList;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
 */
public class ViewerConfigDialog extends Dialog {

    private static final String[] inverseSearchValues = new String[] { ViewerAttributeRegistry.INVERSE_SEARCH_NO, ViewerAttributeRegistry.INVERSE_SEARCH_RUN, ViewerAttributeRegistry.INVERSE_SEARCH_STD };
    
    protected File lastPath;
    private Text fileField;
    private Text nameField;
    private Text argsField;
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
        
        String name = nameField.getText();
        if (name == null || name.length() == 0) {
            setStatus("preferenceViewerDialogNameEmpty", "");
            return;
        }
        
        // if adding new configuration, existing name is not valid
        if (nameList != null && nameList.contains(name)) {
            setStatus("preferenceViewerDialogNameExists", name);
            return;
        }
        
        File f = new File(fileField.getText());
        if (!f.exists()) {
            setStatus("preferenceViewerDialogFileNotFound", f.getAbsolutePath());
            return;
        }
        
        registry.setActiveViewer(name);
        registry.setCommand(fileField.getText());
        registry.setArguments(argsField.getText());
        registry.setFormat(formatChooser.getItem(formatChooser.getSelectionIndex()));
        registry.setInverse(inverseSearchValues[inverseChooser.getSelectionIndex()]);
        registry.setForward(forwardChoice.getSelection() +"");
        
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
        addFormatChooser(composite);
        addInverseChooser(composite);
        addForwardChooser(composite);
        
        Group group = new Group(composite, SWT.SHADOW_IN);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridData)group.getLayoutData()).horizontalSpan = 2;
        group.setLayout(new GridLayout());
        statusField = new Label(group, SWT.LEFT);
        statusField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        statusField.setText(resolveStatus());
        statusField.setToolTipText(TexlipsePlugin.getResourceString("preferenceViewerStatusTooltip"));
        
        return composite;
    }
    
    /**
     * @return an initial status message for the status field
     */
    private String resolveStatus() {
        String path = fileField.getText();
        if (path != null && path.length() > 0) {
            File file = new File(path);
            if (!file.exists()) {
                Button b = getButton(IDialogConstants.OK_ID);
                if (b != null) {
                    // set button status
                    b.setEnabled(false);
                }
                return TexlipsePlugin.getResourceString("preferenceViewerDialogFileNotFound").replaceAll("%s", path);
            }
        }
        Button b = getButton(IDialogConstants.OK_ID);
        if (b != null) {
            // set button status
            b.setEnabled(true);
        }
        return TexlipsePlugin.getResourceString("preferenceViewerDialogFileOk");
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
                statusField.setText(resolveStatus());
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
        int index = 0;
        while (index < inverseSearchValues.length &&
                !inverseSearchValues[index].equals(registry.getInverse())) {
            index++;
        }
        
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
}
