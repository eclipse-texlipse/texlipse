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

import org.eclipse.core.resources.IResource;
import org.eclipse.texlipse.properties.TexlipseProperties;


/**
 * Run the external dvips program.
 * 
 * @author Kimmo Karlsson
 */
public class DvipsRunner extends AbstractProgramRunner {

    public DvipsRunner() {
        super();
    }

    protected String getWindowsProgramName() {
        return "dvips.exe";
    }

    protected String getUnixProgramName() {
        return "dvips";
    }

    public String getDescription() {
        return "Dvips program";
    }

    public String getDefaultArguments() {
        return "-R0 -o %output %input";
    }

    public String getInputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_DVI;
    }

    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PS;
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
