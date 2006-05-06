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

import net.sourceforge.texlipse.actions.InputQueryDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;


/**
 * A FieldEditor that holds a list of strings as its value.
 * 
 * The list of strings is converted into a single String object by 
 * putting a separator character between the items while concatenating.
 * 
 * @author Kimmo Karlsson
 */
public class StringListFieldEditor extends ListEditor implements IInputValidator {

    // separator character for strings
    public static final String SEPARATOR = ",";

    // list of invalid characters that can't be in a keyword
    private static final String INVALID_CHARS = " ,()[]{}<>|\\?+/&#%$¤£#@\"!§½";
    
    /**
     * Creates a new string list field editor.
     * 
     * @param name preference name
     * @param labelText text for description label
     * @param parent parent component
     */
    public StringListFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
    }

    /**
     * Combines the given list of items into a single string.
     * @param items list of items
     * @return a single string
     */
    protected String createList(String[] items) {
        StringBuffer sb = new StringBuffer();
        if (items.length > 0) {
        	for (int i = 0; i < items.length-1; i++) {
        		sb.append(items[i]);
        		sb.append(SEPARATOR);
        	}
        	sb.append(items[items.length-1]);
        }
        return sb.toString();
    }

    /**
     * Creates and returns a new item for the list.
     * This implementation opens a question dialog, where user can
     * enter a new item.
     * 
     * @return the string the user wanted to add, or null
     *         if the cancel button was pressed or the string was an empty one
     */
    protected String getNewInputObject() {
        
        InputQueryDialog dialog =
            InputQueryDialog.createQuery("Enter string", "Please enter keyword",
                    IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL);
        dialog.setValidator(this);
        
        int code = dialog.open();
        if (code == Window.OK) {
            
            String g = dialog.getInput();
            if (g != null && g.length() == 0) {
                return null;
            }
            
            return g;
        }
        return null;
    }

    /**
     * Parse the given string into a list of items.
     * @return a list of items parsed from the given string 
     */
    protected String[] parseString(String stringList) {
        return stringList.split(SEPARATOR);
    }

    /**
     * Validates the input of the dialog.
     * @param newText the contents of the dialog's text field
     * @return error message, or null if text is valid
     */
    public String isValid(String newText) {
        boolean error = false;
        
        for (int i = 0; i < newText.length(); i++) {
            if (INVALID_CHARS.indexOf(newText.charAt(i)) >= 0) {
                error = true;
                break;
            }
        }
        
        return error ? "invalid character" : null;
    }
}
