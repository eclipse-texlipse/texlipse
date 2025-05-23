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

package org.eclipse.texlipse.actions;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.editor.TexAutoIndentStrategy;
import org.eclipse.texlipse.editor.TexEditor;
import org.eclipse.texlipse.properties.TexlipseProperties;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;


/**
 * Listens for word wrap toggle -actions, toggling wrap on or off.
 * 
 * @author Laura Takkinen
 * @author Oskar Ojala
 * @author Leonardo Montecchi
 */
public class TexWordWrapAction implements IEditorActionDelegate, IActionDelegate2 {
    private IEditorPart targetEditor;
    private IAction sourceButton;

    private boolean enabled;
    private boolean hard;

    private String ICON_WRAP = "icons/wrap.gif";
    private String ICON_WRAP_SOFT = "icons/wrap-soft.gif";
    private String ICON_WRAP_HARD = "icons/wrap-hard.gif";

    public TexWordWrapAction() {
        loadWrapConfig();
        TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new WrapPropertyChangeListener());
    }

    /**
     * Listens wrap type changes
     * 
     * @author Laura Takkinen
     */
    private class WrapPropertyChangeListener implements IPropertyChangeListener {

        /**
         * Changes between wrap types (soft <-> hard) if wrap toggle
         * button is checked.
         */
        public void propertyChange(PropertyChangeEvent event) {
            String ev = event.getProperty();
            if (ev.equals("wrapType") || ev.equals("wrapDefault")) {
                loadWrapConfig();
                setWrap();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate2#init(org.eclipse.jface.action.IAction)
     */
    public void init(IAction action) {
        sourceButton = action;
        setWrap();
        action.setChecked(enabled);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        this.targetEditor = targetEditor;
        action.setEnabled(this.targetEditor instanceof TexEditor);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        cycleState();
        setWrap();
        action.setChecked(enabled);
    }

    /**
     * Cycles between the three wrap states:
     * off -> soft -> hard -> off ...
     *
     * @author Leonardo Montecchi
     */
    private void cycleState() {
        if (!enabled) {
            enabled = true;
            hard = false;
        } else {
            if (hard) {
                hard = false;
                enabled = false;
            } else {
                hard = true;
            }
        }

        saveWrapConfig();
    }

    /**
     * Load wrap configuration from Eclipse properties into button state
     *
     * @author Leonardo Montecchi
     */
    private void loadWrapConfig() {
        this.enabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.WORDWRAP_DEFAULT);
        this.hard = TexlipsePlugin.getDefault().getPreferenceStore().getString(TexlipseProperties.WORDWRAP_TYPE) == TexlipseProperties.WORDWRAP_TYPE_HARD;

        if(sourceButton != null) {
            sourceButton.setChecked(enabled);
        }
    }

    /**
     * Save wrap configuration from button state into Eclipse properties
     *
     * @author Leonardo Montecchi
     */
    private void saveWrapConfig() {
        String currentType = hard ? TexlipseProperties.WORDWRAP_TYPE_HARD : TexlipseProperties.WORDWRAP_TYPE_SOFT; 
        TexlipsePlugin.getDefault().getPreferenceStore().setValue(TexlipseProperties.WORDWRAP_DEFAULT, enabled);
        TexlipsePlugin.getDefault().getPreferenceStore().setValue(TexlipseProperties.WORDWRAP_TYPE, currentType);
    }

    /**
     * Activates the selected wrap mode (set editor properties).
     */
    private void setWrap() {
        ISourceViewer viewer = getTextEditor() != null ? getTextEditor().getViewer() : null;
        if (enabled) {
            if (hard) {
                TexAutoIndentStrategy.setHardWrap(true);
                if (viewer != null) {
                    viewer.getTextWidget().setWordWrap(false);
                }
            } else {
                TexAutoIndentStrategy.setHardWrap(false);
                if (viewer != null) {
                    viewer.getTextWidget().setWordWrap(true);
                }
            }
        } else {
            TexAutoIndentStrategy.setHardWrap(false);
            if (viewer != null) {
                viewer.getTextWidget().setWordWrap(false);
            }
        }

        updateIcon();
    }

    /**
     * Gets the instance of TexEditor
     * @return current TexEditor instance
     */
    private TexEditor getTextEditor() {
        if (this.targetEditor == null) return null;
        if (this.targetEditor instanceof TexEditor) {
            return (TexEditor) targetEditor;
        } else {
            throw new RuntimeException("Expecting TeX editor. Found: "+targetEditor.getClass().getName());
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate2#dispose()
     */
    public void dispose() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate2#runWithEvent(org.eclipse.jface.action.IAction, org.eclipse.swt.widgets.Event)
     */
    public void runWithEvent(IAction action, Event event) {
        run(action);
    }

    /**
     * Updates the button icon based on current wrap state
     *
     * @author Leonardo Montecchi
     */
    private void updateIcon() {
        Bundle bundle = FrameworkUtil.getBundle(getClass());

        String imgToLoad = ICON_WRAP;
        String toolTip = "Word wrap *disabled*\r\nClick the button to switch mode";
        if (enabled) {
            if (hard) {
                imgToLoad = ICON_WRAP_HARD;
                toolTip = "*Hard* wrap enabled\r\nClick the button to switch mode";
            } else {
                imgToLoad = ICON_WRAP_SOFT;
                toolTip = "*Soft* wrap enabled\r\nClick the button to switch mode";
            }
        }

        URL imgUrl = FileLocator.find(bundle, new Path(imgToLoad),null);
        ImageDescriptor updatedImage = ImageDescriptor.createFromURL(imgUrl);
        sourceButton.setImageDescriptor(updatedImage);
        sourceButton.setToolTipText(toolTip);
    }
}
