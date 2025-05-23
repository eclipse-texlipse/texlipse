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

package org.eclipse.texlipse.viewer.util;

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
