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
