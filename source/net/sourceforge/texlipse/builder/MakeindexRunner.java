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

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IResource;


/**
 * Run the external makeindex program.
 * 
 * @author Kimmo Karlsson
 */
public class MakeindexRunner extends AbstractProgramRunner {

    public MakeindexRunner() {
        super();
    }

    protected String getWindowsProgramName() {
        return "makeindex.exe";
    }
    
    protected String getUnixProgramName() {
        return "makeindex";
    }
    
    public String getDescription() {
        return "Makeindex program";
    }
    
    public String getInputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_IDX;
    }
    
    public String getOutputFormat() {
        return null;
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
