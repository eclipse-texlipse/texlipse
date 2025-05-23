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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.texlipse.properties.TexlipseProperties;
import org.eclipse.ui.PlatformUI;


/**
 * Run the external ps2pdf program.
 * 
 * @author Kimmo Karlsson
 */
public class Ps2pdfRunner extends AbstractProgramRunner {

    public Ps2pdfRunner() {
        super();
    }

    protected String getWindowsProgramName() {
        return "ps2pdf.exe";
    }
    
    protected String getUnixProgramName() {
        return "ps2pdf";
    }
    
    public String getDescription() {
        return "Ps2pdf program";
    }
    
    public String getInputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PS;
    }
    
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PDF;
    }
    
    /**
     * Parse the output of the ps2pdf program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        if (output.indexOf("**** Could not open the file ") >= 0) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
                            "Error", "Unable to create the pdf file. Please close all pdf viewers!");
                }
            });
            return true;
        }
        //TODO: more ps2pdf error parsing
        return false;
    }
}
