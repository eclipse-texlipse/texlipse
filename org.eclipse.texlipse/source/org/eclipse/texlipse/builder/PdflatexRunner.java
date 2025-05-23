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
 * Run the external pdflatex program.
 * 
 * @author Kimmo Karlsson
 */
public class PdflatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public PdflatexRunner() {
        super();
    }
    
    protected String getWindowsProgramName() {
        return "pdflatex.exe";
    }
    
    protected String getUnixProgramName() {
        return "pdflatex";
    }
    
    public String getDescription() {
        return "PdfLatex program";
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
