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

import org.eclipse.core.resources.IResource;
import org.eclipse.texlipse.properties.TexlipseProperties;


/**
 * Run the external makeindex program with Nomenclature style file.
 * 
 * @author Antonio Ramos Garrido
 */
public class MakeindexNomenclRunner extends AbstractProgramRunner {

    public MakeindexNomenclRunner() {
        super();
    }

    protected String getWindowsProgramName() {
        return "makeindex.exe";
    }
    
    protected String getUnixProgramName() {
        return "makeindex";
    }
    
    public String getDescription() {
        return "Makeindex program (nomencl)";
    }
    
    public String getDefaultArguments() {
        return "%input -s nomencl.ist -o %output";
    }
    public String getInputFormat() {
        return TexlipseProperties.INPUT_FORMAT_NOMENCL;
    }
    
    public String getOutputFormat() {
    	return TexlipseProperties.OUTPUT_FORMAT_NOMENCL;
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
