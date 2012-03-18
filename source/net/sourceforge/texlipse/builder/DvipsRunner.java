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

import net.sourceforge.texlipse.builder.factory.RunnerDescription;

import org.eclipse.core.resources.IResource;


/**
 * Run the external dvips program.
 * 
 * @author Kimmo Karlsson
 */
public class DvipsRunner extends AbstractProgramRunner {

    public DvipsRunner(RunnerDescription description) {
        super(description);
    }

    /**
     * Parse the output of the dvips program.
     *  
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        //TODO: dvips error parsing
        return false;
    }
}
