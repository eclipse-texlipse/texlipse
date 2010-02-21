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
import net.sourceforge.texlipse.builder.BuilderRegistry;
import net.sourceforge.texlipse.builder.ProgramRunner;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.help.WorkbenchHelp;


/**
 * Builder preferences.
 * 
 * @author Kimmo Karlsson
 */
public class BuilderSettingsPreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage {

    // list of builders to configure
    private List builderList;
    
    // checkbox for enabling console output
    private Button consoleOutputCheckBox;

    // checkbox for enabling console output
    private Button auxParserCheckBox;

    // field for browsing to tex distribution install dir
    private Text texDirField;

    // last path opened with texDirField
    protected File lastPath;

    // button that opens an editor dialog
    private Button editButton;

    /**
     * Construct a new page.
     */
	public BuilderSettingsPreferencePage() {
		super();
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        //setDescription(TexlipsePlugin.getResourceString("preferenceBuilderPageDescription"));
        lastPath = new File(resolveTexDir());
    }

	/**
     * @return the supposed directory of the tex distribution binaries
     */
    private String resolveTexDir() {
        ProgramRunner runner = BuilderRegistry.getRunner(0);
        if (runner == null) {
            return File.separator;
        }
        
        String dir = runner.getProgramPath();
        if (dir == null || dir.length() == 0) {
            return File.separator;
        }
        
        int index = dir.lastIndexOf(File.separatorChar);
        if (index < 0) {
            return dir;
        }
        
        return dir.substring(0, index);
    }

    /**
     * Creates the page components.
	 */
	protected Control createContents(Composite parent) {
        
	    Composite contents = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        contents.setLayout(gl);
        
	    addBuilderList(contents);
        TexlipsePreferencePage.addSpacer(2, false, contents);
        addTexInstallDir(contents);
        TexlipsePreferencePage.addSpacer(2, false, contents);
        addConsoleCheckBox(contents);
        //TexlipsePreferencePage.addSpacer(1, true, contents);
        addAuxParserCheckBox(contents);
        
        return contents;
	}

    /**
     * @param contents parent component
     */
    private void addTexInstallDir(Composite contents) {
        
        Composite parent = new Composite(contents, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        parent.setLayout(layout);
        GridData pgd = new GridData(GridData.FILL_HORIZONTAL);
        pgd.horizontalSpan = 2;
        parent.setLayoutData(pgd);

        TexlipsePreferencePage.addLabelField(3, TexlipsePlugin.getResourceString("preferenceBuilderTexDirDescription"), parent);
        
        Label label = new Label(parent, SWT.LEFT);
        label.setText(TexlipsePlugin.getResourceString("preferenceBuilderTexDirLabel"));
        label.setLayoutData(new GridData());
        
        texDirField = new Text(parent, SWT.SINGLE | SWT.BORDER);
        texDirField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        texDirField.setText("");
        WorkbenchHelp.setHelp(texDirField, TexlipseHelpIds.BUILDER_TEX_DIR);
        
        Button button = new Button(parent, SWT.PUSH);
        button.setText(JFaceResources.getString("openBrowse"));
        button.setLayoutData(new GridData());
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                
                DirectoryDialog dialog = new DirectoryDialog(getShell());
                if (lastPath != null) {
                    if (lastPath.exists())
                        dialog.setFilterPath(lastPath.getAbsolutePath());
                }
                
                String dir = dialog.open();
                if (dir != null) {
                    lastPath = new File(dir.trim());
                    if (lastPath.exists()) {
                        texDirField.setText(lastPath.getAbsolutePath());
                    } else {
                        lastPath = null;
                    }
                }
            }});
    }

    /**
     * Add list of builders with an edit button.
     * @param contents parent component
     */
    private void addBuilderList(Composite contents) {
        Composite leftPart = new Composite(contents, SWT.NULL);
        leftPart.setLayout(new GridLayout());
        leftPart.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Label label = new Label(leftPart, SWT.LEFT);
        label.setText(TexlipsePlugin.getResourceString("preferenceBuilderListLabel"));
        label.setLayoutData(new GridData());
        
        builderList = new List(leftPart, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        builderList.setItems(getBuilderItems());
        builderList.setLayoutData(new GridData(GridData.FILL_BOTH));
        builderList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                int index = builderList.getSelectionIndex();
                editButton.setEnabled(index >= 0);
            }});
        WorkbenchHelp.setHelp(builderList, TexlipseHelpIds.BUILDER_LIST);
        
        Composite rightPart = new Composite(contents, SWT.NULL);
        rightPart.setLayout(new GridLayout());
        rightPart.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Label empty = new Label(rightPart, SWT.LEFT);
        empty.setLayoutData(new GridData());
        
        editButton = new Button(rightPart, SWT.PUSH);
        editButton.setEnabled(false);
        editButton.setText(TexlipsePlugin.getResourceString("openEdit"));
        editButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        editButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                int index = builderList.getSelectionIndex();
                if (index >= 0) {
                    openEditorDialog(index);
                }
            }});
        
        Label filler = new Label(rightPart, SWT.LEFT);
        filler.setLayoutData(new GridData(GridData.FILL_VERTICAL));
    }

    /**
     * Add "output to console" -checkbox.
     * @param contents parent component
     */
    private void addConsoleCheckBox(Composite contents) {
        Composite checkField = new Composite(contents, SWT.NULL);
        GridData checkData = new GridData(GridData.FILL_HORIZONTAL);
        checkData.horizontalSpan = 2;
        checkField.setLayoutData(checkData);
        GridLayout checkLay = new GridLayout();
        checkLay.numColumns = 2;
        checkField.setLayout(checkLay);
        
        consoleOutputCheckBox = new Button(checkField, SWT.CHECK);
        consoleOutputCheckBox.setLayoutData(new GridData());
        consoleOutputCheckBox.setText(TexlipsePlugin.getResourceString("preferenceBuilderConsoleOutput"));
        consoleOutputCheckBox.setSelection(getPreferenceStore().getBoolean(TexlipseProperties.BUILDER_CONSOLE_OUTPUT));
        // checkboxes don't show their help ids (at least on linux) 
        //WorkbenchHelp.setHelp(consoleOutputCheckBox, TexlipseHelpIds.BUILDER_CONSOLE);
    }

    /**
     * Add "Parse .aux files" -checkbox.
     * @param contents parent component
     */
    private void addAuxParserCheckBox(Composite contents) {
        Composite checkField = new Composite(contents, SWT.NULL);
        GridData checkData = new GridData(GridData.FILL_HORIZONTAL);
        checkData.horizontalSpan = 2;
        checkField.setLayoutData(checkData);
        GridLayout checkLay = new GridLayout();
        checkLay.numColumns = 2;
        checkField.setLayout(checkLay);
        
        auxParserCheckBox = new Button(checkField, SWT.CHECK);
        auxParserCheckBox.setLayoutData(new GridData());
        auxParserCheckBox.setText(TexlipsePlugin.getResourceString("preferenceBuilderAuxParser"));
        auxParserCheckBox.setSelection(getPreferenceStore().getBoolean(TexlipseProperties.BUILDER_PARSE_AUX_FILES));
    }

    /**
     * Called when ok-button (or apply-button) is pressed.
     * Saves all the field editor values to preferences.
     * @return true
     */
    public boolean performOk() {
        boolean ok = super.performOk();
        getPreferenceStore().setValue(TexlipseProperties.BUILDER_CONSOLE_OUTPUT, consoleOutputCheckBox.getSelection());
        getPreferenceStore().setValue(TexlipseProperties.BUILDER_PARSE_AUX_FILES, auxParserCheckBox.getSelection());
        changeTexDistribution();
        texDirField.setText("");
        builderList.setItems(getBuilderItems());
        return ok;
    }

    /**
     * Called when defaults-button is pressed.
     * Sets all field values to defaults.
     * Currently does nothing to builder paths.
     */
    public void performDefaults() {
        super.performDefaults();
        consoleOutputCheckBox.setSelection(getPreferenceStore().getDefaultBoolean(TexlipseProperties.BUILDER_CONSOLE_OUTPUT));
        auxParserCheckBox.setSelection(getPreferenceStore().getDefaultBoolean(TexlipseProperties.BUILDER_PARSE_AUX_FILES));
    }
    
    /**
     * Change the tex distribution of all program runners.
     */
    private void changeTexDistribution() {

        String texDir = texDirField.getText();
        if (texDir != null && texDir.length() > 0) {

            File dir = new File(texDir);
            if (dir != null && dir.exists() && dir.isDirectory()) {

                int size = BuilderRegistry.getNumberOfRunners();
                for (int i = 0; i < size; i++) {

                    ProgramRunner prog = BuilderRegistry.getRunner(i);
                    File file = new File(dir, prog.getProgramName());
                    
                    if (file != null && file.exists() && file.isFile()) {
                        prog.setProgramPath(file.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * @return list of labels for configurable builders
     */
    private String[] getBuilderItems() {
        int number = BuilderRegistry.getNumberOfRunners();
        String[] array = new String[number];
        for (int i = 0; i < number; i++) {
            array[i] = getRunnerLabel(i);
        }
        return array;
    }

    /**
     * @param runner index
     * @return label for configurable builder
     */
    private String getRunnerLabel(int i) {
        ProgramRunner runner = BuilderRegistry.getRunner(i);
        return runner.getDescription() + "        (" + runner.getProgramPath() + ')';
    }
    
    /**
     * Opens the BuilderConfigDialog.
     * @param index index of the configurable builder
     */
    private void openEditorDialog(int index) {
        BuilderConfigDialog dialog = new BuilderConfigDialog(getShell(),
                BuilderRegistry.getRunner(index));
        int code = dialog.open();
        if (code == BuilderConfigDialog.OK) {
            builderList.setItem(index, getRunnerLabel(index));
        }
    }

	/**
	 * Page initialization. Does nothing.
	 */
	public void init(IWorkbench workbench) {
	}
}
