/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.builder.factory.RunnerDescription;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;


/**
 * Run the external ps2pdf program.
 * 
 * @author Kimmo Karlsson
 */
public class Ps2pdfRunner extends AbstractProgramRunner {

    public Ps2pdfRunner(RunnerDescription description) {
        super(description);
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
