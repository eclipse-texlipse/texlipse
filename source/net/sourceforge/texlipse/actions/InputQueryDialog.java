/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * A simple dialog window with "ok" and "cancel" -buttons,
 * a label and a text field.
 * 
 * This is almost like org.eclipse.jface.dialogs.InputDialog, but
 * this version has an image and creation-time-settable button texts.
 * 
 * The value of the textfield can be queried after the dialog has been closed.
 * 
 * @author Kimmo Karlsson
 */
public class InputQueryDialog extends MessageDialog {

    // the value of the textfield. null in the beginning,
    // non-null after the user types something
    private String input;
    
    // the text field in the dialog window
    private Text field;

    // a flag indicating the button that was pressed in the dialog.
    // true means ok-button, false means cancel
    private boolean okPressed = false;

    // the initial value of the textfield 
    private String initialText;

    private IInputValidator validator;

    /**
     * Does the same as super().
     * @see MessageDialog#MessageDialog(org.eclipse.swt.widgets.Shell, java.lang.String, org.eclipse.swt.graphics.Image, java.lang.String, int, java.lang.String[], int)
     */
    public InputQueryDialog(Shell parentShell, String dialogTitle,
            Image dialogTitleImage, String dialogMessage, int dialogImageType,
            String[] dialogButtonLabels, int defaultIndex) {
        super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
                dialogImageType, dialogButtonLabels, defaultIndex);
    }

    /**
     * Same kind of constructor as in InputDialog.
     * This class just doesn't create extra components.
     * 
     * @param title dialog title
     * @param message dialog message
     * @param def default text for the textfield
     * @param vali validator for the text
     */
    public InputQueryDialog(Shell shell, String title, String message, String def, IInputValidator vali) {
        super(shell, title, null, message, QUESTION, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
        initialText = def;
        input = def;
        validator = vali;
    }
    
    /**
     * Creates and returns the contents of an area of the dialog which appears
     * below the message and above the button bar.
     * <p>
     * The default implementation of this framework method returns
     * <code>null</code>. Subclasses may override.
     * </p>
     * 
     * @param parent
     *            parent composite to contain the custom area
     * @return the custom area control, or <code>null</code>
     */
    protected Control createCustomArea(Composite parent) {
        
        // create the top level composite for the dialog area
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(data);
        
        field = new Text(composite, SWT.SINGLE | SWT.BORDER);
        if (initialText != null) {
            field.setText(initialText);
            field.setSelection(0, initialText.length());
        }
        field.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        field.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                if (validator != null) {
                    input = field.getText();
                    if (input != null) {
                        input = input.trim();
                    }
                    String error = validator.isValid(input);
                    Button ok = getButton(IDialogConstants.OK_ID);
                    ok.setEnabled(error == null);
                } else {
                    input = field.getText();
                }
            }});
        
        return composite;
    }

    /**
     * Sets a validator for the dialog input.
     * @param validator input validator
     */
    public void setValidator(IInputValidator validator) {
        this.validator = validator;
    }
    
    /**
     * Returns the text of the text field. Recommended to call after
     * the dialog has been closed.
     * 
     * @return the text from the text field, or null
     *         if the user hasn't typed anything yet
     */
    public String getInput() {
        return input;
    }
    
    /**
     * Convenience method to open a simple "enter text" dialog.
     * 
     * @param title dialog title
     * @param message dialog message
     * @param okText text for the ok-button
     * @param cancelText text for the cancel-button
     * @return query dialog
     */
    public static InputQueryDialog createQuery(String title, String message, String okText, String cancelText) {
        return new InputQueryDialog(new Shell(),
            title,
            null, // accept the default window icon
            message, QUESTION,
            new String[] { okText, cancelText },
            0); // OK is the default
    }
    
    /**
     * Convenience method to open a simple "enter text" dialog.
     * This version uses the workbench default "ok" and "cancel" labels
     * as button texts.
     * 
     * @param title dialog title
     * @param message dialog message
     * @return query dialog
     */
    public static InputQueryDialog createQuery(String title, String message) {
        return createQuery(title, message, IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL);
    }
}
