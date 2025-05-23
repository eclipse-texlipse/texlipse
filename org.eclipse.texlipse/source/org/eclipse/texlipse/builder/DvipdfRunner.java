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
 * Run the external dvipdf program.
 * 
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 */
public class DvipdfRunner extends AbstractProgramRunner {

    public DvipdfRunner() {
        super();
    }

    protected String getWindowsProgramName() {
        return "dvipdfm.exe";
    }

    protected String getUnixProgramName() {
        return "dvipdf";
    }

    public String getDescription() {
        return "Dvipdf program";
    }

    public String getInputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_DVI;
    }

    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PDF;
    }

    /**
     * Parse the output of dvipdf program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        if (output.indexOf("Unable to open ") >= 0) {
            PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
                            "Error", "Unable to create the pdf file. Please close all pdf viewers!");
                }
            });
            return true;
        }
        //TODO: more dvipdf error parsing
        return false;
    }
}
