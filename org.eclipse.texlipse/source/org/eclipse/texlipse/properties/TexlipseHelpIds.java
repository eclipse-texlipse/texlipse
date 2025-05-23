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

package org.eclipse.texlipse.properties;

import org.eclipse.texlipse.TexlipsePlugin;

/**
 * The context help IDs.
 * @author Kimmo Karlsson
 */
public class TexlipseHelpIds {

    private static final String PACKAGE = TexlipsePlugin.class.getPackage().getName() + ".";
    
    public static final String BUILDER_LIST = PACKAGE + "builder_list";
    public static final String BUILDER_CONSOLE = PACKAGE + "console_out";
    public static final String BUILDER_TEX_DIR = PACKAGE + "tex_dir";

    public static final String WRAP_LENGTH = PACKAGE + "wrap_len";

    public static final String VIEWER_LIST = PACKAGE + "viewer_list";
    public static final String VIEWER_PORT = PACKAGE + "viewer_port";

}
