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
