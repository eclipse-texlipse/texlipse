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

import java.lang.reflect.InvocationTargetException;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;


/**
 * Basic Project creation wizard.
 * 
 * @author kpkarlss
 */
public class TexlipseProjectCreationWizard extends Wizard
     implements INewWizard, IExecutableExtension {

    // the attributes of the wizard
    private TexlipseProjectAttributes attributes;

    // the perspective configuration element 
    private IConfigurationElement configElement;

    /**
     * Create new wizard.
     */
    public TexlipseProjectCreationWizard() {
        super();
        attributes = new TexlipseProjectAttributes();
        setDialogSettings(TexlipsePlugin.getDefault().getDialogSettings());
        setWindowTitle(TexlipsePlugin.getResourceString("projectWizardTitle"));
    }

    /**
     * Add pages to the wizard.
     */
    public void addPages() {
        super.addPages();
        addPage(new TexlipseProjectCreationWizardPage(attributes));
        addPage(new TexlipseProjectFilesWizardPage(attributes));
    }

    /**
     * Finish the project creation, i.e. run the ProjectCreationOperation.
     * @return false on error
     */
    public boolean performFinish() {
        
        TexlipseProjectCreationOperation runnable = new TexlipseProjectCreationOperation(attributes);
        IRunnableWithProgress op = new WorkspaceModifyDelegatingOperation(runnable);

        boolean result = true;
        try {
            
            getContainer().run(false, true, op);
            
        } catch (InterruptedException e) {
            result = false;
        } catch (InvocationTargetException e) {
            handleTargetException(e.getTargetException());
            result = false;
        }
        
        BasicNewProjectResourceWizard.updatePerspective(configElement);
        return result;
    }

    /**
     * Handle the exceptions of project creation here.
     * @param target
     */
    private void handleTargetException(Throwable target) {
        
        if (target instanceof CoreException) {
            
            IStatus status = ((CoreException) target).getStatus();
            ErrorDialog.openError(getShell(), TexlipsePlugin.getResourceString("projectWizardErrorTitle"),
                    TexlipsePlugin.getResourceString("projectWizardErrorMessage"), status);

        } else {
            
            MessageDialog.openError(getShell(), TexlipsePlugin.getResourceString("projectWizardErrorTitle"),
                    target.getMessage());
        }
    }

    /**
     * Useful method for e.g. getting config element info.
     * 
     * @param config
     * @param propertyName
     * @param data
     */
    public void setInitializationData(IConfigurationElement config,
            String propertyName, Object data) throws CoreException {
        configElement = config;
    }

    /**
     * Useful method for e.g. loading images for the wizard.
     * 
     * @param workbench
     * @param selection
     */
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setNeedsProgressMonitor(true);
    }
}
