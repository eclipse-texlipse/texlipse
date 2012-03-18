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
 * Run the external makeindex program with Nomenclature style file.
 * 
 * @author Antonio Ramos Garrido
 */
public class MakeindexNomenclRunner extends AbstractProgramRunner {

    public MakeindexNomenclRunner(RunnerDescription description) {
        super(description);
    }
    
    /**
     * Parse the output of the makeindex program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        //TODO: makeindex error parsing
        return false;
    }
}
