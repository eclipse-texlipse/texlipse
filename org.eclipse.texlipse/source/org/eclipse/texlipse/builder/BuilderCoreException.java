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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Exception to indicate erros in Latex source, not the build process itself.
 * 
 * @author Kimmo Karlsson
 */
public class BuilderCoreException extends CoreException {

    /**
     * @param status status
     */
    public BuilderCoreException(IStatus status) {
        super(status);
    }
}
