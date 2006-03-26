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

import java.util.ArrayList;
import java.util.Arrays;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.viewer.ViewerAttributeRegistry;
import net.sourceforge.texlipse.viewer.ViewerConfigDialog;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;


/**
 * A FieldEditor that holds a list of strings as its value.
 * 
 * The list of strings is converted into a single String object by 
 * putting a separator character between the items while concatenating.
 * 
 * @author Kimmo Karlsson
 * @author Tor Arne Vestbø
 */
public class ViewerListFieldEditor extends ListEditor {

    // separator character for strings
    public static final String SEPARATOR = ",";

    // registry for the configs
    private ViewerAttributeRegistry registry;
    
    // cache for name list
    private ArrayList nameList;

    // button to launch the config editor
    private Button editButton;

    // parent component for accessing default components 
    private Composite parent;

    /**
     * Creates a new string list field editor.
     * 
     * @param labelText text for description label
     * @param parent parent component
     */
    public ViewerListFieldEditor(String labelText, Composite parent) {
        super(ViewerAttributeRegistry.VIEWER_NAMES+".TEMP", labelText, parent);
        nameList = new ArrayList();
        registry = new ViewerAttributeRegistry();
    }

    /**
     * Load the values from preferences.
     * @see super.doLoad()
     */
    protected void doLoad() {
        super.doLoad();
        registry.load(getPreferenceStore());
        List list = getListControl(parent);
        list.setItems(addPaths(TexlipsePlugin.getPreferenceArray(ViewerAttributeRegistry.VIEWER_NAMES)));
    }

    /**
     * Load the default values from preferences.
     * @see super.doLoadDefault()
     */
    protected void doLoadDefault() {
        super.doLoadDefault();
        registry = new ViewerAttributeRegistry();
        List list = getListControl(parent);
        list.setItems(addPaths(TexlipsePlugin.getPreferenceArray(ViewerAttributeRegistry.VIEWER_NAMES)));
    }

    /**
     * Adds path information to the list of viewer names.
     * @param items viewer names
     * @return an array of viewer names with viewer path information added
     */
    private String[] addPaths(String[] items) {
        String oldViewer = registry.getActiveViewer();
        String[] array = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            registry.setActiveViewer(items[i]);
            array[i] = items[i] + " (" + registry.getCommand() + ")";
        }
        registry.setActiveViewer(oldViewer);
        return array;
    }

    /**
     * Store the values to preferences.
     * @see super.doStore()
     */
    protected void doStore() {
        super.doStore();
        List list = getListControl(parent);
        String[] trueNames = removePaths(list.getItems());
        registry.setActiveViewer(trueNames[0]);
        registry.save(getPreferenceStore(), trueNames);
    }

    /**
     * Strips path information from the list of viewer names.
     * @param items the items from the list component
     * @return the given array of items, with path information left out
     */
    private String[] removePaths(String[] items) {
        String[] array = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            array[i] = items[i].substring(0, items[i].indexOf('(')-1);
        }
        return array;
    }

    /**
     * Creates a new editor button for the field editor.
     * @param parent parent component
     * @return the "edit"-button
     */
    protected Button createEditButton(Composite parent) {
        
        Button button = new Button(parent, SWT.PUSH);
        button.setText(TexlipsePlugin.getResourceString("openEdit"));
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.heightHint = convertVerticalDLUsToPixels(button, IDialogConstants.BUTTON_HEIGHT);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.setEnabled(false);
        button.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                openEditorDialog();
            }});
        
        return button;
    }

    /**
     * Open the config editor dialog for editing an existing configuration.
     * This is called, when edit button is pressed.
     */
    private void openEditorDialog() {
        
        List list = getListControl(parent);
        int index = list.getSelectionIndex();
        if (index < 0) {
            // no item selected from the list, do nothing
            return;
        }
        
        String name = list.getItem(index);
        if (name == null || name.length() == 0) {
            // no name for the item, can't load config
            return;
        }
        
        registry.setActiveViewer(name.substring(0, name.indexOf('(')-1));
        ViewerConfigDialog dialog = new ViewerConfigDialog(editButton.getShell(),
                (ViewerAttributeRegistry) registry.clone());
        
        int code = dialog.open();
        if (code == Window.OK) {
            registry.mergeWith(dialog.getRegistry());
            list.setItem(index, registry.getActiveViewer() + " (" + registry.getCommand() + ")");
        }
    }

    /**
     * Insert a button to the button box.
     * This method calls super and creates a the "edit"-button to the list.
     */
    public Composite getButtonBoxControl(Composite parent) {
        Composite box = super.getButtonBoxControl(parent);
        if (editButton == null) {
            this.parent = parent;
            editButton = createEditButton(box);
        }
        return box;
    }
    
    /**
     * Add an additional SelectionListener to the list component.
     * This method call super and installs a SelectionListener,
     * which enables and disables the "edit"-button.
     */
    public List getListControl(Composite parent) {
        List list = super.getListControl(parent);
        list.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                List listWidget = (List) event.widget;
                editButton.setEnabled(listWidget.getSelectionIndex() >= 0);
            }});
        return list;
    }
    
    /**
     * Combines the given list of items into a single string.
     * @param items list of items
     * @return a single string
     */
    protected String createList(String[] items) {
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < items.length-1; i++) {
            sb.append(items[i]);
            sb.append(SEPARATOR);
        }
        sb.append(items[items.length-1]);
        
        nameList.clear();
        nameList.addAll(Arrays.asList(items));
        
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
        
        ViewerConfigDialog dialog = new ViewerConfigDialog(this.getLabelControl().getShell(), nameList);
        
        int code = dialog.open();
        if (code == Window.OK) {
            
            ViewerAttributeRegistry reg = dialog.getRegistry();
            String name = reg.getActiveViewer();
            nameList.add(name);
            
            registry.mergeWith(reg);
            
            return name + " (" + reg.getCommand() + ")";
            
        }
        return null;
    }

    /**
     * Parse the given string into a list of items.
     * @return a list of items parsed from the given string 
     */
    protected String[] parseString(String stringList) {
        String[] arr = stringList.split(SEPARATOR);
        nameList.clear();
        nameList.addAll(Arrays.asList(arr));
        return arr;
    }
}
