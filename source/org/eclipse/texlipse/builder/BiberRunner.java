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
