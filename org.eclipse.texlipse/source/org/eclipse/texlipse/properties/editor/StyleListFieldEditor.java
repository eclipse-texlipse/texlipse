/*******************************************************************************
 * Copyright (c) 2017 the TeXlipse team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/
package org.eclipse.texlipse.properties.editor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.texlipse.TexlipsePlugin;


/**
 * A FieldEditor that holds a list of key-value-pairs as its value.
 * 
 * The list of strings is converted into a single String object by 
 * putting a separator character between the items while concatenating.
 * 
 * @author Kimmo Karlsson
 * @author Markus Maus
 * @author Reza Esmaeili Soumeeh
 * @author Ehsan Baghi
 */
public class StyleListFieldEditor extends FieldEditor implements IInputValidator {
    
    // separator character for strings
    public static final String SEPARATOR = ",";
    
    private static final String SK_TABLE_KEY = "key";
    private static final String SK_TABLE_VALUE = "value";
    
    // component holding smart key variables table and label
    private Composite table;
    
    // smart key variable table component
    private TableViewer smartKeyTable;
    
    // for adding new variables
    private Button skAddButton;
    
    // for removing variables
    private Button skRemoveButton;
    
    // for editing existing variables
    private Button skEditButton;
    
    // the smartKeys variables
    private Map smartKeys;
    
    /**
     * Smart key variable for the smart key table.
     */
    class SmartKeyVariable {
        
        // The name of the smartKeys variable
        private String name;
        
        // The value of the smartKeys variable
        private String value;
        
        /**
         * Create new key/value -pair.
         * @param name key
         * @param value value
         */
        public SmartKeyVariable(String name, String value) {
            this.name = name;
            this.value = value;
        }
        
        /**
         * Returns this variable's name, which serves as the key in the key/value
         * pair this variable represents
         * 
         * @return this variable's name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Returns this variables value.
         * @return this variable's value
         */
        public String getValue() {
            return value;
        }
        
        /**
         * Sets this variable's name (key)
         * @param name name
         */
        public void setName(String name) {
            this.name = name;
        }
        
        /**
         * Sets this variable's value
         * @param value value
         */
        public void setValue(String value) {
            this.value = value;
        }
        
        /**
         * Returns this variable's name.
         * @return this variable's name
         */
        public String toString() {
            return getName();
        }
        
        /**
         * Compare names.
         */
        public boolean equals(Object obj) {
            boolean equal = false;
            if (obj instanceof SmartKeyVariable) {
                SmartKeyVariable var = (SmartKeyVariable)obj;
                equal = var.getName().equals(name);
            }
            return equal;       
        }
        
        /**
         * @return name.hashCode()
         */
        public int hashCode() {
            return name.hashCode();
        }
    }
    
    /**
     * Content provider for the smartKeys table.
     * @see org.eclipse.debug.ui.EnvironmentTab.EnvironmentVariableContentProvider
     */
    class SmartKeyVariableContentProvider implements IStructuredContentProvider {
        public Object[] getElements(Object inputElement) {
            SmartKeyVariable[] elements = new SmartKeyVariable[0];
            Map m = (Map) inputElement;
            if (m != null && !m.isEmpty()) {
                elements = new SmartKeyVariable[m.size()];
                String[] varNames = new String[m.size()];
                m.keySet().toArray(varNames);
                for (int i = 0; i < m.size(); i++) {
                    elements[i] = new SmartKeyVariable(varNames[i], (String) m.get(varNames[i]));
                }
            }
            return elements;
        }
        public void dispose() {
        }
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            if (newInput == null){
                return;
            }
            if (viewer instanceof TableViewer){
                TableViewer tableViewer = (TableViewer) viewer;
                if (tableViewer.getTable().isDisposed()) {
                    return;
                }
                tableViewer.setSorter(new ViewerSorter() {
                    public int compare(Viewer iviewer, Object e1, Object e2) {
                        if (e1 == null) {
                            return -1;
                        } else if (e2 == null) {
                            return 1;
                        } else {
                            return ((SmartKeyVariable)e1).getName().compareToIgnoreCase(((SmartKeyVariable)e2).getName());
                        }
                    }
                });
            }
        }
    }
    
    /**
     * Label provider for the smartKeys table.
     * @see org.eclipse.debug.ui.EnvironmentTab.EnvironmentVariableLabelProvider
     */
    class SmartKeyVariableLabelProvider extends LabelProvider implements ITableLabelProvider {
        public String getColumnText(Object element, int columnIndex) {
            String result = null;
            if (element != null) {
                SmartKeyVariable var = (SmartKeyVariable) element;
                switch (columnIndex) {
                case 0: // variable
                    result = var.getName();
                    break;
                case 1: // value
                    result = var.getValue();
                    break;
                }
            }
            return result;
        }
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }
    }
    
    /**
     * Dialog to input variable name and value.
     */
    class KeyValueInputDialog extends MessageDialog {
        
        private IInputValidator validator;
        private Text keyTextField;
        protected String key;
        private Text valueTextField;
        protected String value;
        
        /**
         * Default constructor from super class. Do not use.
         * 
         * @param parentShell
         * @param dialogTitle
         * @param dialogTitleImage
         * @param dialogMessage
         * @param dialogImageType
         * @param dialogButtonLabels
         * @param defaultIndex
         */
        public KeyValueInputDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
            super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
                    dialogImageType, dialogButtonLabels, defaultIndex);
            key = "";
            value = "";
            
            
        }
        
        /**
         * Constructor with default values for many fields.
         * This is the preferred constructor.
         * 
         * @param shell parent shell
         * @param title dialog title
         * @param message dialog message
         * @param vali validator for the text
         */
        public KeyValueInputDialog(Shell shell, String title, String message, IInputValidator vali) {
            this(shell, title, null, message, QUESTION, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
            validator = vali;
            
        }
        
        /**
         * @return key
         */
        public String getKey() {
            return key;
        }
        
        /**
         * @param name key
         */
        public void setKey(String name) {
            key = name;
        }
        
        /**
         * @return value
         */
        public String getValue() {
            return value;
        }
        
        /**
         * @param val value
         */
        public void setValue(String val) {
            value = val;
        }
        
        /**
         * Creates and returns the contents of an area of the dialog which appears
         * below the message and above the button bar.
         * 
         * This implementation creates two labels and two textfields.
         * 
         * @param parent parent composite to contain the custom area
         * @return the custom area control, or <code>null</code>
         */
        protected Control createCustomArea(Composite parent) {
            
            // create the top level composite for the dialog area
            Composite composite = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.numColumns = 2;
            composite.setLayout(layout);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            
            Label keyLabel = new Label(composite, SWT.LEFT);
            keyLabel.setLayoutData(new GridData());
            keyLabel.setText("key:");
            
            keyTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
            keyTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            keyTextField.setText(key);
            keyTextField.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    if (validator != null) {
                        key = keyTextField.getText();
                        if (key != null) {
                            key = key.trim();
                        }
                        String error = validator.isValid(key);
                        Button ok = getButton(IDialogConstants.OK_ID);
                        ok.setEnabled(error == null);
                    } else {
                        key = keyTextField.getText();
                    }
                }});
            
            Label valueLabel = new Label(composite, SWT.LEFT);
            valueLabel.setLayoutData(new GridData());
            valueLabel.setText("value:");
            
            valueTextField = new Text(composite, SWT.SINGLE | SWT.BORDER);
            valueTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            valueTextField.setText(value);
            valueTextField.addModifyListener(new ModifyListener() {
                public void modifyText(ModifyEvent e) {
                    value = valueTextField.getText();
                }});
            
            return composite;
        }
    }
    
    /**
     * Dialog to input variables from the current smartKeys.
     */
    class SmartKeyVariableInputDialog extends MessageDialog {
        
        private List skVarList;
        protected int[] selections;
        protected String[] items;
        
        /**
         * Default constructor from super class. Do not use.
         * 
         * @param parentShell
         * @param dialogTitle
         * @param dialogTitleImage
         * @param dialogMessage
         * @param dialogImageType
         * @param dialogButtonLabels
         * @param defaultIndex
         */
        public SmartKeyVariableInputDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType, String[] dialogButtonLabels, int defaultIndex) {
            super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
                    dialogImageType, dialogButtonLabels, defaultIndex);
            selections = new int[0];
            items = new String[0];
            setShellStyle(getShellStyle() | SWT.RESIZE);
        }
        
        /**
         * Constructor with default values for many fields.
         * This is the preferred way to construct this class.
         * 
         * @param shell parent shell
         * @param title dialog title
         * @param message dialog message
         * @param items items for the list
         */
        public SmartKeyVariableInputDialog(Shell shell, String title, String message, String[] items) {
            this(shell, title, null, message, QUESTION, new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 0);
            this.items = items;
        }
        
        /**
         * Prevents the dialog from appearing too wide and narrow.
         */
        protected Point getInitialSize() {
            Point p = super.getInitialSize();
            
            int sum = 0;
            int maxLen = 0;
            for (int i = 0; i < items.length; i++) {
                int l = items[i].length();
                sum += l;
                if (l > maxLen) {
                    maxLen = l;
                }
            }
            sum /= items.length;
            
            FontData[] fd = skVarList.getFont().getFontData();
            p.x = (p.x-100) * sum / maxLen + 100; // scale the list size down by the average length of an item
            p.y += fd[0].getHeight() * 8 * 2; // add 8 lines to the default height (*2 to get it right. don't know why this is needed)
            p.x *= 2; // don't know why this is needed
            return p;
        }
        
        /**
         * @return indexes of selected items
         */
        public int[] getSelections() {
            return selections;
        }
        
        /**
         * Creates and returns the contents of an area of the dialog which appears
         * below the message and above the button bar.
         * 
         * This implementation creates two labels and two textfields.
         * 
         * @param parent parent composite to contain the custom area
         * @return the custom area control, or <code>null</code>
         */
        protected Control createCustomArea(Composite parent) {
            
            Composite composite = new Composite(parent, SWT.NULL);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));
            composite.setLayout(new GridLayout());
            
            skVarList = new List(composite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
            skVarList.setLayoutData(new GridData(GridData.FILL_BOTH));
            skVarList.setItems(items);
            skVarList.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent event) {
                    selections = skVarList.getSelectionIndices();
                }});
            
            return composite;
        }
    }
    
    /**
     * Creates a new string list field editor.
     * 
     * @param name preference name
     * @param labelText text for description label
     * @param parent parent component
     */
    public StyleListFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
        smartKeys = new HashMap();
    }
    
    /**
     * Validates the input of the dialog.
     * @param newText the contents of the dialog's text field
     * @return error message, or null if text is valid
     */
    
    /**
     * @return 2
     */
    public int getNumberOfControls() {
        return 2;
    }
    
    /**
     * 
     * @param numColumns number of columns in the page layout
     */
    protected void adjustForNumColumns(int numColumns) {
        ((GridData)table.getLayoutData()).horizontalSpan = numColumns - 1;
    }
    
    /**
     * 
     * @param parent parent component
     * @param numColumns number of columns in the page layout
     */
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        table = createTable(parent);
        ((GridData)table.getLayoutData()).horizontalSpan = numColumns - 1;
        createButtons(parent);
    }
    
    /**
     * @see org.eclipse.debug.ui.EnvironmentTab#createEnvironmentTable(org.eclipse.swt.widgets.Composite)
     * @param parent parent component
     * @return table container
     */
    protected Composite createTable(Composite parent) {
        Font font = parent.getFont();
        
        // Create table composite
        Composite tableComposite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.heightHint = 150;
        tableComposite.setLayout(layout);
        tableComposite.setLayoutData(gridData);
        tableComposite.setFont(font);
        
        // Create label
        getLabelControl(tableComposite);
        
        // Create table
        smartKeyTable = new TableViewer(tableComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        Table table = smartKeyTable.getTable();
        TableLayout tableLayout = new TableLayout();
        table.setLayout(tableLayout);
        table.setHeaderVisible(true);
        table.setFont(font);
        gridData = new GridData(GridData.FILL_BOTH);
        smartKeyTable.getControl().setLayoutData(gridData);
        smartKeyTable.setContentProvider(new SmartKeyVariableContentProvider());
        smartKeyTable.setLabelProvider(new SmartKeyVariableLabelProvider());
        smartKeyTable.setColumnProperties(new String[] { SK_TABLE_KEY, SK_TABLE_VALUE });
        smartKeyTable.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                handleTableSelectionChanged(event);
            }
        });
        smartKeyTable.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                if (!smartKeyTable.getSelection().isEmpty()) {
                    handleSkEditButtonSelected();
                }
            }
        });
        
        // Create columns
        ColumnWeightData columnLayout = new ColumnWeightData(50);
        tableLayout.addColumnData(columnLayout);
        TableColumn tc = new TableColumn(table, SWT.NONE, 0);
        tc.setResizable(columnLayout.resizable);
        tc.setText(TexlipsePlugin.getResourceString("preferenceKeyValueTableColumn1"));
        columnLayout = new ColumnWeightData(50);
        tableLayout.addColumnData(columnLayout);
        tc = new TableColumn(table, SWT.NONE, 1);
        tc.setResizable(columnLayout.resizable);
        tc.setText(TexlipsePlugin.getResourceString("preferenceKeyValueTableColumn2"));
        return tableComposite;
    }
    
    /**
     * 
     * @param parent parent component
     * @return button container
     */
    protected Composite createButtons(Composite parent) {
        
        Composite buttonComposite = new Composite(parent, SWT.NULL);
        buttonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        buttonComposite.setLayout(new GridLayout());
        
        Label empty = new Label(buttonComposite, SWT.NONE);
        empty.setLayoutData(new GridData());
        
        skAddButton = new Button(buttonComposite, SWT.PUSH);
        skAddButton.setText(TexlipsePlugin.getResourceString("preferenceKeyValueAddButton"));
        skAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        skAddButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                handleSkAddButtonSelected();
            }});
        
        skRemoveButton = new Button(buttonComposite, SWT.PUSH);
        skRemoveButton.setText(TexlipsePlugin.getResourceString("preferenceKeyValueRemoveButton"));
        skRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        skRemoveButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                handleSkRemoveButtonSelected();
            }});
        skRemoveButton.setEnabled(false);
        
        skEditButton = new Button(buttonComposite, SWT.PUSH);
        skEditButton.setText(TexlipsePlugin.getResourceString("preferenceKeyValueEditButton"));
        skEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        skEditButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                handleSkEditButtonSelected();
            }});
        skEditButton.setEnabled(false);
        
        Label filler = new Label(buttonComposite, SWT.NONE);
        filler.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        
        return buttonComposite;
    }
    
    /**
     * Handle add-button presses.
     */
    private void handleSkAddButtonSelected() {
        
        KeyValueInputDialog dialog =
            new KeyValueInputDialog(getLabelControl().getShell(),
                    TexlipsePlugin.getResourceString("preferenceKeyValueAddDialogTitle"),
                    TexlipsePlugin.getResourceString("preferenceKeyValueAddDialogLabel"), this);
        
        int code = dialog.open();
        if (code == KeyValueInputDialog.OK) {
            
            String key = dialog.getKey();
            if (key == null || key.length() == 0) {
                return;
            }
            String value = dialog.getValue();
            
            smartKeys.put(key, value);
            smartKeyTable.add(new SmartKeyVariable(key, value));
        }
    }
    
    /**
     * Handle remove-button presses.
     */
    private void handleSkRemoveButtonSelected() {
        
        IStructuredSelection sel = (IStructuredSelection) smartKeyTable.getSelection();
        smartKeyTable.getControl().setRedraw(false);
        for (Iterator i = sel.iterator(); i.hasNext(); ) {
            SmartKeyVariable var = (SmartKeyVariable) i.next();   
            smartKeyTable.remove(var);
            smartKeys.remove(var.getName());
        }
        smartKeyTable.getControl().setRedraw(true);
    }
    
    /**
     * Handle edit-button presses.
     */
    private void handleSkEditButtonSelected() {
        
        IStructuredSelection sel = (IStructuredSelection) smartKeyTable.getSelection();
        SmartKeyVariable var = (SmartKeyVariable) sel.iterator().next();
        if (var == null) {
            return;
        }
        
        KeyValueInputDialog dialog =
            new KeyValueInputDialog(getLabelControl().getShell(),
                    TexlipsePlugin.getResourceString("preferenceKeyValueEditDialogTitle"),
                    TexlipsePlugin.getResourceString("preferenceKeyValueEditDialogLabel"), this);
        
        dialog.setKey(var.getName());
        dialog.setValue(var.getValue());
        
        int code = dialog.open();
        if (code == KeyValueInputDialog.OK) {
            
            String key = dialog.getKey();
            if (key == null || key.length() == 0) {
                return;
            }
            String value = dialog.getValue();
            
            smartKeyTable.remove(var);
            smartKeys.remove(var.getName());
            
            smartKeys.put(key, value);
            smartKeyTable.add(new SmartKeyVariable(key, value));
        }
    }
    
    /**
     * Responds to a selection changed event in the smartKeys table
     * @param event the selection change event
     */
    protected void handleTableSelectionChanged(SelectionChangedEvent event) {
        int size = ((IStructuredSelection)event.getSelection()).size();
        skEditButton.setEnabled(size == 1);
        skRemoveButton.setEnabled(size > 0);
    }
    
    /**
     * Loads the smartKeys variables from preference store.
     */
    protected void doLoad() {
        doLoadFrom(getPreferenceStore().getString(getPreferenceName()));
    }
    
    /**
     * Loads the smartKeys variables from the given string.
     * @param str
     */
    protected void doLoadFrom(String str) {
        smartKeys.clear();
        
        if (str == null) {
            return;
        }
        
        String[] binds = str.split(SEPARATOR);
        if (binds == null) {
            return;
        }
        
        for (int i = 0; i < binds.length; i++) {
            
            int index = binds[i].indexOf('=');
            if (index <= 0) {
                continue;
            }
            
            smartKeys.put(binds[i].substring(0, index),
                    binds[i].substring(index+1));
        }
        
        smartKeyTable.setInput(smartKeys);
    }
    
    /**
     * Loads the default smartKeys variables from preference store.
     */
    protected void doLoadDefault() {
        doLoadFrom(getPreferenceStore().getDefaultString(getPreferenceName()));
    }
    
    /**
     * Stores the smartKeys variables to single string.
     */
    protected void doStore() {
        StringBuffer sb = new StringBuffer();
        
        String[] keys = (String[]) smartKeys.keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            
            sb.append(keys[i]);
            sb.append('=');
            sb.append(smartKeys.get(keys[i]));
            if (i < keys.length-1) {
                sb.append(',');
            }
        }
        
        getPreferenceStore().setValue(getPreferenceName(), sb.toString());
    }
    
    public String isValid(String newText) {
        return null;
    }
}
