/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.viewer;

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
