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

import org.eclipse.texlipse.properties.TexlipseProperties;

/**
 * Run the external pslatex program.
 * 
 * @author Kimmo Karlsson
 */
public class PslatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public PslatexRunner() {
        super();
    }
    
    protected String getWindowsProgramName() {
        return "pslatex.exe";
    }
    
    protected String getUnixProgramName() {
        return "pslatex";
    }
    
    public String getDescription() {
        return "PsLatex program";
    }
    
    /**
     * @return output file format (ps)
     */
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PS;
    }
}
