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

import java.util.StringTokenizer;

import org.eclipse.core.resources.IResource;
import org.eclipse.texlipse.properties.TexlipseProperties;


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
    
    public String getDefaultArguments() {
        return "%input -s %style";
    }
    
    /**
     * Replace also the style file parameter from the arguments.
     * @param resource .tex file to compile
     * @return argument string for latex program
     */
    public String getArguments(IResource resource) {
        
        String args = super.getArguments(resource);
        String style = TexlipseProperties.getProjectProperty(resource.getProject(),
                TexlipseProperties.MAKEINDEX_STYLEFILE_PROPERTY);
        
        if (style != null && style.length() > 0) {
            args = args.replaceAll("%style", style);
        }
        else {
            args = args.replaceAll("-s\\s+%style", "");
        }
        
        return args;
    }
    
    public String getInputFormat() {
        return TexlipseProperties.INPUT_FORMAT_IDX;
    }
    
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_IDX;
    }
    
    /**
     * Parse the output of the makeindex program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        
        boolean errorsFound = false;
        StringTokenizer st = new StringTokenizer(output, "\r\n");
        
        while (st.hasMoreTokens()) {
            String line = st.nextToken();
            if (line.endsWith("not found.")) {
                errorsFound = true;
                createMarker(resource, null, line);
            }
        }
        
        return errorsFound;
    }
}
