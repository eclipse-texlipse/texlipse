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
 * Run the external Xelatex program.
 * 
 * @author Boris von Loesch
 */
public class XelatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public XelatexRunner() {
        super();
    }
    
    protected String getWindowsProgramName() {
        return "xelatex.exe";
    }
    
    protected String getUnixProgramName() {
        return "xelatex";
    }
    
    public String getDescription() {
        return "XeLatex program";
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
