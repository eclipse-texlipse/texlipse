/*
 * $Id$
 *
 * Copyright (c) 2005 by the TeXlipse Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions.project;

import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;


/**
 * Simple handler to force a BibTex run on next build.
 *
 * @author Boris von Loesch
 */
public class RunBibOnNextBuildHandler extends AbstractHandler {

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        IProject project = TexlipseHandlerUtil.getProject(event);
        TexlipseProperties.setSessionProperty(project, TexlipseProperties.BIBFILES_CHANGED, true);
        return null;
    }

}
