/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.viewer.util;

/**
 * Interface to call when a location of file should be shown in the editor.
 * Used by FileLocationServer.
 * 
 * @author Kimmo Karlsson
 */
public interface FileLocationListener {

    /**
     * Opens the specified file in the editor and highlights the given line.
     * @param file file name
     * @param lineNumber line number
     */
    void showLineOfFile(String file, int lineNumber);

}
