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

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * A preference page that is contributed to the Preferences dialog. 
 * 
 * Preferences are stored in the preference store that belongs to
 * the main plug-in class.
 * 
 * @author Kimmo Karlsson
 */
public class TexlipsePreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {
    
    /**
     * Creates the preference page.
	 */
	public TexlipsePreferencePage() {
		super(GRID);
		setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
		setDescription(TexlipsePlugin.getResourceString("preferencePageDescription"));
	}

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
	public void createFieldEditors() {
    }

    /**
     * Initializes this preference page for the given workbench.
     * <p>
     * This method is called automatically as the preference page is being created
     * and initialized. Clients must not call this method.
     * </p>
     *
     * @param workbench the workbench
     */
	public void init(IWorkbench workbench) {
        // nothing to do
	}

    /**
     * Add a horizontal line to the page.
     * @param span number of horizontal columns to span
     * @param parent parent container
     */
    public static void addSeparator(int span, Composite parent) {
        Label empty = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
        GridData lgd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        lgd.horizontalSpan = span;
        empty.setLayoutData(lgd);
    }

    /**
     * Add a horizontal line to the page.
     * This is equivalent to calling: addSeparator(2, parent);
     * @param parent parent container
     */
    public static void addSeparator(Composite parent) {
        addSeparator(2, parent);
    }
    
    /**
     * Add some empty horizontal space to the page.
     * @param span number of horizontal columns to span
     * @param verticalFill fill container vertically
     * @param parent parent component
     */
    public static void addSpacer(int span, boolean verticalFill, Composite parent) {
        Label spacer = new Label(parent, SWT.LEFT);
        GridData spacerData = new GridData(GridData.FILL_HORIZONTAL);
        spacerData.horizontalSpan = span;
        if (verticalFill) {
            spacerData.verticalAlignment = GridData.FILL;
            spacerData.grabExcessVerticalSpace = true;
        }
        spacer.setLayoutData(spacerData);
    }
    
    /**
     * Add some empty horizontal space to the page.
     * This is equivalent to calling: addSpacer(span, false, parent);
     * @param span number of horizontal columns to span
     * @param parent parent component
     */
    public static void addSpacer(int span, Composite parent) {
        addSpacer(span, false, parent);
    }
    
    /**
     * Add some empty horizontal space to the page.
     * This is equivalent to calling: addSpacer(2, false, parent);
     * @param parent parent component
     */
    public static void addSpacer(Composite parent) {
        addSpacer(2, false, parent);
    }

    /**
     * Adds a plain text label with wrapping to the page.
     * @param span number of columns to span
     * @param str the text string
     * @param parent parent component
     */
    public static void addLabelField(int span, String str, Composite parent) {
        //
        // Bugfix for wrapping issue in Eclipse 3.0.1 for Windows (actually SWT library):
        // if FieldEditorPreferencePage contains a plain Label, that has too long 
        // text, the wrapping doesn't get enabled, and other labels in the page are also
        // not wrapped,
        if (System.getProperty("os.name").indexOf("indow") > 0) {
            
            int index = str.indexOf(' ', str.length()/2);
            String part1 = str.substring(0, index);
            String part2 = str.substring(index+1);
            
            Label label1 = new Label(parent, SWT.LEFT);
            label1.setText(part1);
            GridData ld1 = new GridData();
            ld1.horizontalSpan = span;
            label1.setLayoutData(ld1);
            
            Label label2 = new Label(parent, SWT.LEFT);
            label2.setText(part2);
            GridData ld2 = new GridData();
            ld2.horizontalSpan = span;
            label2.setLayoutData(ld2);
        
        } else {
            
            Label label = new Label(parent, SWT.LEFT | SWT.WRAP);
            label.setText(str);
            GridData ld = new GridData();
            ld.horizontalSpan = span;
            label.setLayoutData(ld);
        }
    }
}
