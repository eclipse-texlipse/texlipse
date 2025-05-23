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
 * Runs Biber, a bibliography sorting utility for BibLaTeX.
 *
 * @author Matthias Erll
 *
 */
public class BiberRunner extends AbstractProgramRunner {

    public BiberRunner() {
        super();
    }

    public String getDescription() {
        return "Biber (BibLaTeX)";
    }

    public String getInputFormat() {
        return TexlipseProperties.INPUT_FORMAT_BCF;
    }

    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_BBL;
    }

    @Override
    protected String getWindowsProgramName() {
        return "biber.exe";
    }

    @Override
    protected String getUnixProgramName() {
        return "biber";
    }

    @Override
    protected boolean parseErrors(IResource resource, String output) {
        boolean hasErrors = false;
        StringTokenizer st = new StringTokenizer(output, "\r\n");
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.startsWith("FATAL")) {
                createMarker(resource, null, s);
                hasErrors = true;
            }
        }
        return hasErrors;
    }

}
