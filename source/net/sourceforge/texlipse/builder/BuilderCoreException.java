/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

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
