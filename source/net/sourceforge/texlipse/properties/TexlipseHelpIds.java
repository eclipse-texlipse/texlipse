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
