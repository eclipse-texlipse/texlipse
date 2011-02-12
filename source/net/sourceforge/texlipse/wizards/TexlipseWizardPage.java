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

import java.util.HashMap;
import java.util.Iterator;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;


/**
 * A page on the project creation wizard.
 * 
 * @author kpkarlss
 */
public abstract class TexlipseWizardPage extends WizardPage {

    // the status of the input fields
    protected HashMap<Object, IStatus> statusMap;
    protected TexlipseProjectAttributes attributes;
    
    /**
     * Creates a slightly more helpful wizardpage.
     * @param pageNumber page number
     * @param attributes
     */
    protected TexlipseWizardPage(int pageNumber, TexlipseProjectAttributes attributes) {
        super("page" + pageNumber);
        this.attributes = attributes;
        setTitle(TexlipsePlugin.getResourceString("projectWizardPageTitle"));
        setDescription(TexlipsePlugin.getResourceString("projectWizardPageDescription"));
        statusMap = new HashMap<Object, IStatus>();
    }

    /**
     * Create the layout of the page.
     * @param parent parent component in the UI
     */
    public void createControl(Composite parent) {
        
        // set layout
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gd = new GridLayout();
        gd.numColumns = 2;
        composite.setLayout(gd);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL));

        // create input fields
        createComponents(composite);
        
        // initialize status messages to ok
        statusMap.put(composite, createStatus(IStatus.OK, ""));

        // set page control to the created component
        setControl(composite);
    }
    
    /**
     * Add a separator to the page.
     * @param parent parent component
     */
    protected void addSeparator(Composite parent) {
        
        Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
        gridData.horizontalSpan = 2;
        separator.setLayoutData(gridData);
    }
    
    /**
     * Add a spacer to the page.
     * @param parent parent component
     * @param span cell span
     */
    protected void addSpacer(Composite parent, int span) {
        
        Label spacer = new Label(parent, SWT.LEFT);
        GridData gd = null;
        if (span == 1) {
            gd = new GridData();
        } else {
            gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
            gd.horizontalSpan = span;
        }
        spacer.setLayoutData(gd);
    }
    
    /**
     * Subclasses should use this method to create page components.
     * @param parent parent component
     * @return number of components that use a status message
     */
    protected abstract void createComponents(Composite parent);

    /**
     * Create a status message for the status bar.
     *  
     * @param severity
     * @param message
     * @return
     */
    protected IStatus createStatus(int severity, String message) {
        return new Status(severity, TexlipsePlugin.getPluginId(), severity,
                message, null);
    }

    /**
     * Update the status to the status bar.
     * 
     * The statusbar message is an error message, if at least one of the
     * fields has an invalid value. If the current field has an invalid value,
     * the corresponding error message is displayed. Otherwise,
     * the first error message found (starting from the top) is displayed.
     * 
     * @param lastStatus
     * @param number  
     */
    protected void updateStatus(IStatus lastStatus, Object key) {
        
        IStatus status = null;
        boolean allOk = true;
        
        // update the status cache
        statusMap.put(key, lastStatus);
        
        // see if we got an error
        if (lastStatus.matches(IStatus.ERROR)) {
            status = lastStatus;
            allOk = false;
        } else {
        
            // see if some other value is invalid
            Iterator<IStatus> iter = statusMap.values().iterator();
            while (iter.hasNext()) {
                IStatus i = iter.next();
                if (!i.matches(IStatus.OK)) status = i;
                if (i.matches(IStatus.ERROR)) {
                    allOk = false;
                    break;
                }
            }
        }
        
        // enable/disable next-button
        setPageComplete(allOk);
        
        // only set status if this page is visible
        Control ctrl = getControl();
        if (ctrl != null && ctrl.isVisible()) {
            
            if (status == null) {
                status = lastStatus;
            }
            
            applyToStatusLine(this, status);
        }
    }

    /**
     * Add a status bar message.
     * 
     * @param page
     * @param status
     */
    protected static void applyToStatusLine(DialogPage page, IStatus status) {

        String errorMessage = null;
        String warningMessage = null;
        String statusMessage = status.getMessage();
        
        if (statusMessage.length() > 0) {
            if (status.matches(IStatus.ERROR)) {
                errorMessage = statusMessage;
            } else if (!status.isOK()) {
                warningMessage = statusMessage;
            }
        }
        page.setErrorMessage(errorMessage);
        page.setMessage(warningMessage, status.getSeverity());
    }
}
