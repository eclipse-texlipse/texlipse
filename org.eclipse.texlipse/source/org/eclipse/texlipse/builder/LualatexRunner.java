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
 * Run the external Lualatex program.
 * 
 * @author Boris von Loesch
 */
public class LualatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public LualatexRunner() {
        super();
    }
    
    protected String getWindowsProgramName() {
        return "lualatex.exe";
    }
    
    protected String getUnixProgramName() {
        return "lualatex";
    }
    
    public String getDescription() {
        return "LuaLatex program";
    }
    
    /**
     * Enable SyncTeX
     */
    public String getDefaultArguments() {
        return "-synctex=1 "+super.getDefaultArguments();
    }

    /**
     * @return output file format (pdf)
     */
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PDF;
    }

}
