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

package org.eclipse.texlipse.builder;

import org.eclipse.core.resources.IProject;


/**
 * Builders implementing this interface are notified before the build process starts,
 * so they can reevaluate per-session properties of the project and adjust the build
 * process accordingly.
 *
 * @author Matthias Erll
 *
 */
public interface AdaptableBuilder {

    /**
     * Reads the relevant per-session properties for the project and changes parameters
     * of the build process.
     *
     * @param project project to build
     */
    public void updateBuilder(IProject project);

}
