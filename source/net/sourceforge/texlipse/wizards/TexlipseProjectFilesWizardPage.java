/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.wizards;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;


/**
 * Filename settings -page on the project creation wizard.
 * 
 * @author Kimmo Karlsson
 */
public class TexlipseProjectFilesWizardPage extends TexlipseWizardPage {

    // textfield for output dir name
    private Text outputDirNameField;

    // textfield for output file name
    private Text outputFileNameField;

    // textfield for source dir name
    private Text sourceDirNameField;

    // textfield for main source file name
    private Text sourceFileNameField;

    // textfield for temp dir name
    private Text tempDirNameField;
    
    // tree view of directories
    private Tree dirTree;

    // tree view's items
    private TreeItem projectDirItem;
    private TreeItem outputDirItem;
    private TreeItem outputFileItem;
    private TreeItem sourceDirItem;
    private TreeItem sourceFileItem;
    private TreeItem tempDirItem;
    private TreeItem tempFileItem;

    // image registry
    private ISharedImages images;

    /**
     * This is the second page on the wizard. Here you set the filenames.
     * @param attributes project attributes
     */
    public TexlipseProjectFilesWizardPage(TexlipseProjectAttributes attributes) {
        super(1, attributes);
        images = TexlipsePlugin.getDefault().getWorkbench().getSharedImages();
    }

    /**
     * Called when this page is made visible.
     * This method updates the project name and output format to the
     * directory tree component.
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        projectDirItem.setText(attributes.getProjectName());
        outputFileNameField.setText(attributes.getOutputFile());
        outputFileItem.setText(attributes.getOutputFile());
    }

    /**
     * Create the layout of the page.
     * @param parent parent component in the UI
     * @return number of components using a status message
     */
    public void createComponents(Composite parent) {

        addSpacer(parent, 2);
        Label label = new Label(parent, SWT.LEFT);
        label.setText(TexlipsePlugin.getResourceString("projectWizardDirTreeLabel"));
        label.setLayoutData(new GridData());
        addSpacer(parent, 1);
        
        createTreeControl(parent);

        Composite right = new Composite(parent, SWT.FILL);
        right.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        right.setLayout(gl);

        createOutputDirControl(right);
        createOutputFileControl(right);
        createMainDirControl(right);
        createMainFileControl(right);
        createTempDirControl(right);
    }

    /**
     * Create a directory tree settings box.
     * @param parent the parent container
     */
    private void createTreeControl(Composite parent) {

        dirTree = new Tree(parent, SWT.SINGLE | SWT.BORDER);
        dirTree.setToolTipText(TexlipsePlugin.getResourceString("projectWizardDirTreeTooltip"));
        dirTree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

        recreateSubTree();
    }

    /**
     * Rebuild the whole directory tree component.
     */
    private void recreateSubTree() {

        dirTree.removeAll();
        projectDirItem = new TreeItem(dirTree, SWT.LEFT);
        projectDirItem.setText(attributes.getProjectName());
        projectDirItem.setImage(images.getImage(ISharedImages.IMG_OBJ_FOLDER));

        String dir = attributes.getOutputDir();
        if (dir != null && dir.length() > 0) {
            outputDirItem = new TreeItem(projectDirItem, SWT.LEFT);
            outputDirItem.setText(dir);
            outputDirItem.setImage(images.getImage(ISharedImages.IMG_OBJ_FOLDER));
            outputFileItem = new TreeItem(outputDirItem, SWT.LEFT);
        } else {
            outputDirItem = null;
            outputFileItem = new TreeItem(projectDirItem, SWT.LEFT);
        }
        outputFileItem.setText(attributes.getOutputFile());
        outputFileItem.setImage(images.getImage(ISharedImages.IMG_OBJ_FILE));
        
        dir = attributes.getSourceDir();
        if (dir != null && dir.length() > 0) {
            sourceDirItem = new TreeItem(projectDirItem, SWT.LEFT);
            sourceDirItem.setText(dir);
            sourceDirItem.setImage(images.getImage(ISharedImages.IMG_OBJ_FOLDER));
            sourceFileItem = new TreeItem(sourceDirItem, SWT.LEFT);
        } else {
            sourceDirItem = null;
            sourceFileItem = new TreeItem(projectDirItem, SWT.LEFT);
        }
        sourceFileItem.setText(attributes.getSourceFile());
        sourceFileItem.setImage(images.getImage(ISharedImages.IMG_OBJ_FILE));
        
        dir = attributes.getTempDir();
        if (dir != null && dir.length() > 0) {
            tempDirItem = new TreeItem(projectDirItem, SWT.LEFT);
            tempDirItem.setText(dir);
            tempDirItem.setImage(images.getImage(ISharedImages.IMG_OBJ_FOLDER));
            tempFileItem = new TreeItem(tempDirItem, SWT.LEFT);
        } else {
            tempDirItem = null;
            tempFileItem = new TreeItem(projectDirItem, SWT.LEFT);
        }
        tempFileItem.setText(attributes.getTempFile());
        tempFileItem.setImage(images.getImage(ISharedImages.IMG_OBJ_FILE));
        
        dirTree.showItem(outputFileItem);
        dirTree.showItem(sourceFileItem);
        dirTree.showItem(tempFileItem);
    }

    /**
     * Create the output file name field.
     * @param composite the parent container
     */
    private void createOutputDirControl(Composite composite) {
        
        // add label
        Label mainLabel = new Label(composite, SWT.LEFT);
        mainLabel.setText(TexlipsePlugin.getResourceString("projectWizardOutputDirLabel"));
        mainLabel.setToolTipText(TexlipsePlugin.getResourceString("projectWizardOutputDirTooltip"));
        mainLabel.setLayoutData(new GridData());
        
        // add text field
        outputDirNameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        outputDirNameField.setText(attributes.getOutputDir());
        outputDirNameField.setToolTipText(TexlipsePlugin.getResourceString("projectWizardOutputDirTooltip"));
        outputDirNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        outputDirNameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) {
                if (outputDirItem != null) {
                    dirTree.setSelection(new TreeItem[] { outputDirItem });
                }
            }});
        outputDirNameField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!outputDirNameField.isDisposed()) {
                    String t = outputDirNameField.getText();
                    attributes.setOutputDir(t);
                    validateDirName(outputDirNameField, t);
                    if (t == null || t.length() == 0) {
                        recreateSubTree();
                    } else if (outputDirItem == null) {
                        recreateSubTree();
                    }
                    if (outputDirItem != null) {
                        outputDirItem.setText(t);
                    }
                }
            }});
    }

    /**
     * Create the output file name field.
     * @param composite the parent container
     */
    private void createOutputFileControl(Composite composite) {
        
        // add label
        Label mainLabel = new Label(composite, SWT.LEFT);
        mainLabel.setText(TexlipsePlugin.getResourceString("projectWizardOutputFileLabel"));
        mainLabel.setToolTipText(TexlipsePlugin.getResourceString("projectWizardOutputFileTooltip"));
        mainLabel.setLayoutData(new GridData());
        
        // add text field
        outputFileNameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        outputFileNameField.setText(attributes.getOutputFile());
        outputFileNameField.setToolTipText(TexlipsePlugin.getResourceString("projectWizardOutputFileTooltip"));
        outputFileNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        outputFileNameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) {
                dirTree.setSelection(new TreeItem[] { outputFileItem });
            }});
        outputFileNameField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!outputFileNameField.isDisposed()) {
                    String t = outputFileNameField.getText();
                    outputFileItem.setText(t);
                    validateOutputFileName(t);
                }
            }});
    }

    /**
     * Create the output file name field.
     * @param composite the parent container
     */
    private void createMainDirControl(Composite composite) {
        
        // add label
        Label mainLabel = new Label(composite, SWT.LEFT);
        mainLabel.setText(TexlipsePlugin.getResourceString("projectWizardMainDirLabel"));
        mainLabel.setToolTipText(TexlipsePlugin.getResourceString("projectWizardMainDirTooltip"));
        mainLabel.setLayoutData(new GridData());
        
        // add text field
        sourceDirNameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        sourceDirNameField.setText(attributes.getSourceDir());
        sourceDirNameField.setToolTipText(TexlipsePlugin.getResourceString("projectWizardMainDirTooltip"));
        sourceDirNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sourceDirNameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) {
            	   if (sourceDirItem != null) {
            	       dirTree.setSelection(new TreeItem[] { sourceDirItem });
            	   }
            }});
        sourceDirNameField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!sourceDirNameField.isDisposed()) {
                    String t = sourceDirNameField.getText();
                    attributes.setSourceDir(t);
                    validateDirName(sourceDirNameField, t);
                    if (t == null || t.length() == 0) {
                        recreateSubTree();
                    } else if (sourceDirItem == null) {
                        recreateSubTree();
                    }
                    if (sourceDirItem != null) {
                        sourceDirItem.setText(t);
                    }
                }
            }});
    }

    /**
     * Create main file settings box.
     * @param composite the parent container
     */
    private void createMainFileControl(Composite composite) {
        
        // add label
        Label mainLabel = new Label(composite, SWT.LEFT);
        mainLabel.setText(TexlipsePlugin.getResourceString("projectWizardMainFileLabel"));
        mainLabel.setToolTipText(TexlipsePlugin.getResourceString("projectWizardMainFileTooltip"));
        mainLabel.setLayoutData(new GridData());
        
        // add text field
        sourceFileNameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        sourceFileNameField.setText(attributes.getSourceFile());
        sourceFileNameField.setToolTipText(TexlipsePlugin.getResourceString("projectWizardMainFileTooltip"));
        sourceFileNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sourceFileNameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) {
                dirTree.setSelection(new TreeItem[] { sourceFileItem });
            }});
        sourceFileNameField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!sourceFileNameField.isDisposed()) {
                    String t = sourceFileNameField.getText();
                    sourceFileItem.setText(t);
                    tempFileItem.setText(t.substring(0, t.lastIndexOf('.')+1) + "aux");
                    validateMainFileName(t);
                }
            }});
    }

    /**
     * Create the output file name field.
     * @param composite the parent container
     */
    private void createTempDirControl(Composite composite) {
        
        // add label
        Label mainLabel = new Label(composite, SWT.LEFT);
        mainLabel.setText(TexlipsePlugin.getResourceString("projectWizardTempDirLabel"));
        mainLabel.setToolTipText(TexlipsePlugin.getResourceString("projectWizardTempDirTooltip"));
        mainLabel.setLayoutData(new GridData());
        
        // add text field
        tempDirNameField = new Text(composite, SWT.SINGLE | SWT.BORDER);
        tempDirNameField.setText(attributes.getTempDir());
        tempDirNameField.setToolTipText(TexlipsePlugin.getResourceString("projectWizardTempDirTooltip"));
        tempDirNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tempDirNameField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) {
            	   if (tempDirItem != null) {
                    dirTree.setSelection(new TreeItem[] { tempDirItem });
            	   }
            }});
        tempDirNameField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (!tempDirNameField.isDisposed()) {
                    String t = tempDirNameField.getText();
                    attributes.setTempDir(t);
                    validateDirName(tempDirNameField, t);
                    if (t == null || t.length() == 0) {
                        recreateSubTree();
                    } else if (tempDirItem == null) {
                        recreateSubTree();
                    }
                    if (tempDirItem != null) {
                        tempDirItem.setText(t);
                    }
                }
            }});
    }

    /**
     * Check that the given name corresponds to a valid directory.
     * @param text the directory name
     */
    private void validateDirName(Text field, String text) {

        IStatus status = null;
        
        if (text.indexOf('/') >= 0 || text.indexOf('\\') >= 0) {
            status = createStatus(IStatus.ERROR,
                    TexlipsePlugin.getResourceString("projectWizardDirNameError"));
        } else {
            status = createStatus(IStatus.OK, TexlipsePlugin.getResourceString("projectWizardOutputFileNameOk"));
        }

        updateStatus(status, field);
    }

    /**
     * Check that the given name corresponds to the current output format.
     * @param text the file name
     */
    private void validateOutputFileName(String text) {

        String out = attributes.getOutputFormat();
        IStatus status = null;
        if (!text.toLowerCase().endsWith('.' + out)) {
            status = createStatus(IStatus.ERROR,
                    TexlipsePlugin.getResourceString("projectWizardOutputFileNameError").replaceFirst("%s", out));
        } else {
            status = createStatus(IStatus.OK, TexlipsePlugin.getResourceString("projectWizardOutputFileNameOk"));
            attributes.setOutputFile(text);
        }

        updateStatus(status, outputFileNameField);
    }

    /**
     * Check if the given name is a ".tex" file name.
     * @param text
     */
    private void validateMainFileName(String text) {
        
        IStatus status = null;
        if (!text.toLowerCase().endsWith(".tex") && !text.toLowerCase().endsWith(".ltx")) {
            status = createStatus(IStatus.ERROR,
                    TexlipsePlugin.getResourceString("projectWizardFileNameError"));
        } else {
            status = createStatus(IStatus.OK, TexlipsePlugin.getResourceString("projectWizardFileNameOk"));
            attributes.setSourceFile(text);
        }

        updateStatus(status, sourceFileNameField);
    }
}
