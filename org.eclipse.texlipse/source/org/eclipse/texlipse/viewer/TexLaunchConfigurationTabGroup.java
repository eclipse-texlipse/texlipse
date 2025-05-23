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

package org.eclipse.texlipse.viewer;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * @author kimmo
 */
public class TexLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    /**
     * Creates the tabs contained in this tab group for the specified
     * launch mode. The tabs control's are not created. This is the
     * first method called in the lifecycle of a tab group.
     * 
     * @param dialog the launch configuration dialog this tab group
     *  is contained in
     * @param mode the mode the launch configuration dialog was
     *  opened in
     */
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        
        fTabs = new ILaunchConfigurationTab[3];
        fTabs[0] = new TexLaunchConfigurationTab();
        fTabs[1] = new EnvironmentTab();
        fTabs[2] = new CommonTab();
    }
}
