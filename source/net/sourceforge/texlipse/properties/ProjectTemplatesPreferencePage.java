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
import net.sourceforge.texlipse.templates.ProjectTemplateManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * Delete project templates.
 * 
 * @author Kimmo Karlsson
 */
public class ProjectTemplatesPreferencePage extends PreferencePage
		implements IWorkbenchPreferencePage {

    // the list component showin template names
    private List templateList;

    /**
     * Construct a new page.
     */
	public ProjectTemplatesPreferencePage(){
		super();
        setPreferenceStore(TexlipsePlugin.getDefault().getPreferenceStore());
        setDescription(TexlipsePlugin.getResourceString("preferenceProjectTemplatePageDescription"));
	}
    
	/**
     * Creates the page components.
	 */
    protected Control createContents(Composite parent) {
        
        Composite field = new Composite(parent, SWT.NONE);
        GridData fgd = new GridData(GridData.FILL_BOTH);
        fgd.horizontalSpan = 3;
        field.setLayoutData(fgd);
        GridLayout gl = new GridLayout();
        gl.numColumns = 2;
        field.setLayout(gl);
        
        Label label = new Label(field, SWT.LEFT);
        label.setText(TexlipsePlugin.getResourceString("preferenceProjectTemplateLabel"));
        label.setLayoutData(new GridData());
        
        Label empty = new Label(field, SWT.LEFT);
        empty.setLayoutData(new GridData());
        
        templateList = new List(field, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        templateList.setItems(ProjectTemplateManager.loadUserTemplateNames());
        templateList.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Composite column = new Composite(field, SWT.NONE);
        column.setLayoutData(new GridData(GridData.FILL_BOTH));
        column.setLayout(new GridLayout());
        
        Button remove = new Button(column, SWT.PUSH);
        remove.setText(TexlipsePlugin.getResourceString("preferenceProjectTemplateRemoveButton"));
        remove.setLayoutData(new GridData());
        remove.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent event) {
                int s = templateList.getSelectionIndex();
                if (s >= 0) {
                    boolean reallyDelete = MessageDialog.openConfirm(new Shell(),
                            TexlipsePlugin.getResourceString("preferenceProjectTemplateConfirmTitle"),
                            TexlipsePlugin.getResourceString("preferenceProjectTemplateConfirmDelete").replaceAll("%s", templateList.getItem(s)));
                    if (reallyDelete) {
                        String item = templateList.getItem(s);
                        ProjectTemplateManager.deleteUserTemplate(item);
                        templateList.remove(s);
                    }
                }
            }});
        
        Label spacer = new Label(column, SWT.LEFT);
        spacer.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL));
        
        return field;
	}

    /**
	 * Page initialization. Does nothing.
	 */
	public void init(IWorkbench workbench) {
	}
}
