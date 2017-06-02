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
