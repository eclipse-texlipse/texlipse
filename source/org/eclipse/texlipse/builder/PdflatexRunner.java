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
