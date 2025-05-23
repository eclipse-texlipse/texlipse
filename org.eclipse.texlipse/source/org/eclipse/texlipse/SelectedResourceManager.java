/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Maintains the context used to expand variables. The context is based on
 * the selected resource.
 * 
 * Also contains a reference to the last selected tex file.
 * 
 * @see org.eclipse.debug.internal.ui.stringsubstitution.SelectedResourceManager
 * @author Eclipse.org
 * @author Kimmo Karlsson
 */
public class SelectedResourceManager implements IWindowListener, ISelectionListener {

    // singleton
    private static SelectedResourceManager fgDefault;
    
    // last file that was made active
    private IResource fSelectedResource = null;
    
    // last piece of text that was made active
    private String fSelectedText = null;
    
    // last tex file that was made active
    private IResource fSelectedTexResource = null;

    private int fSelectedLine;
    
    /**
     * Private constructor to use with the singleton.
     */
    private SelectedResourceManager() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null) { //may be running headless
            workbench.addWindowListener(this);
            IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
            if (activeWindow != null) {
                windowActivated(activeWindow);
            }
        } 
    }
    
    /**
     * Returns the singleton resource selection manager
     * 
     * @return VariableContextManager
     */
    public static SelectedResourceManager getDefault() {
        if (fgDefault == null) {
            fgDefault = new SelectedResourceManager(); 
        }
        return fgDefault;
    }
    
    /**
     * @see org.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui.IWorkbenchWindow)
     */
    public void windowActivated(IWorkbenchWindow window) {
        ISelectionService service = window.getSelectionService(); 
        service.addSelectionListener(this);
        IWorkbenchPage page = window.getActivePage();
        if (page != null) {
            IWorkbenchPart part = page.getActivePart();
            if (part != null) {             
                ISelection selection = service.getSelection();
                if (selection != null) {
                    selectionChanged(part, selection);
                }
            }
        }
    }

    /**
     * @see org.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui.IWorkbenchWindow)
     */
    public void windowClosed(IWorkbenchWindow window) {
        window.getSelectionService().removeSelectionListener(this);
    }

    /**
     * @see org.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui.IWorkbenchWindow)
     */
    public void windowDeactivated(IWorkbenchWindow window) {
        window.getSelectionService().removeSelectionListener(this);
    }

    /**
     * @see org.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui.IWorkbenchWindow)
     */
    public void windowOpened(IWorkbenchWindow window) {
    }

    /**
     * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        IResource selectedResource = null;
        if (selection instanceof IStructuredSelection) {
            Object result = ((IStructuredSelection)selection).getFirstElement();
            if (result instanceof IResource) {
                selectedResource = (IResource) result;
            } else if (result instanceof IAdaptable) {
                selectedResource = (IResource)((IAdaptable) result).getAdapter(IResource.class);
            }
        }
        
        if (selectedResource == null) {
            // If the active part is an editor, get the file resource used as input.
            if (part instanceof IEditorPart) {
                IEditorPart editorPart = (IEditorPart) part;
                IEditorInput input = editorPart.getEditorInput();
                selectedResource = (IResource) input.getAdapter(IResource.class);
            } 
        }
        
        if (selectedResource != null) {
            fSelectedResource = selectedResource;
            if ("tex".equalsIgnoreCase(fSelectedResource.getFileExtension()) ||
                    "ltx".equalsIgnoreCase(fSelectedResource.getFileExtension())) {
                fSelectedTexResource = fSelectedResource;
            }
        }
        
        if (selection instanceof ITextSelection) {
            fSelectedText = ((ITextSelection)selection).getText();
            fSelectedLine = ((ITextSelection)selection).getStartLine();
        }
    }
    
    /**
     * Returns the currently selected resource in the active workbench window,
     * or <code>null</code> if none. If an editor is active, the resource adapater
     * associated with the editor is returned.
     * 
     * @return selected resource or <code>null</code>
     */
    public IResource getSelectedResource() {
        return fSelectedResource;
    }
    
    /**
     * Returns the current text selection as a <code>String</code>, or <code>null</code> if
     * none.
     * 
     * @return the current text selection as a <code>String</code> or <code>null</code>
     */
    public String getSelectedText() {
        return fSelectedText;
    }
    
    /**
     * Returns the current text selection as a <code>String</code>, or <code>null</code> if
     * none.
     * 
     * @return the current text selection as a <code>String</code> or <code>null</code>
     */
    public IResource getSelectedTexResource() {
        return fSelectedTexResource;
    }
    
    /**
     * Returns the number of the currently selected line.
     * @return the number of the currently selected line
     */
    public int getSelectedLine() {
        return fSelectedLine;
    }
}
