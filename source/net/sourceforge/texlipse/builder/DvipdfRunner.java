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
 * Run the external dvipdf program.
 * 
 * @author Kimmo Karlsson
 */
public class DvipdfRunner extends AbstractProgramRunner {

    public DvipdfRunner() {
        super();
    }

    protected String getWindowsProgramName() {
        return "dvipdfm.exe";
    }

    protected String getUnixProgramName() {
        return "dvipdf";
    }

    public String getDescription() {
        return "Dvipdf program";
    }

    public String getInputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_DVI;
    }

    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PDF;
    }

    /**
     * Parse the output of dvipdf program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        //TODO: dvipdf error parsing
        return false;
    }
}
