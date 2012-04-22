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

import java.io.IOException;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.factory.RunnerDescription;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;


/**
 * Run the external latex program.
 * 
 * @author Kimmo Karlsson
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public class LatexRunner extends AbstractProgramRunner {

    private FlsAnalyzer flsAnalyzer;

    /**
     * Check if '-recorder' command argument is present for latex runner and
     * adjust cycle detector settings accordingly
     *
     * @param resource resource to be built
     */
    private void checkRecorderFlag(IResource resource) {
        final String arg = getProgramArguments();
        // Check if the flag is already present
        if (arg.contains("-recorder")) {
            flsAnalyzer = new FlsAnalyzer(resource);
        }
        else {
            flsAnalyzer = null;
        }
    }

    /**
     * Create a new ProgramRunner.
     */
    public LatexRunner(RunnerDescription description) {
        super(description);
    }

    @Override
    public void run(IResource resource) throws CoreException {
        checkRecorderFlag(resource);
        super.run(resource);
    }

    protected String[] getQueryString() {
        return new String[] { "\nPlease type another input file name:" , "\nEnter file name:" };
    }

    /**
     * Parse the output of the LaTeX program.
     * 
     * @param resource the input file that was processed
     * @param output the output of the external program
     * @return true, if error messages were found in the output, false otherwise
     */
    protected boolean parseErrors(IResource resource, String output) {
        final IProject project = resource.getProject();
        final LatexLogAnalyzer parser = new LatexLogAnalyzer(resource);
        parser.parseText(output);

        if (parser.hasParsingStackErrors()) {
            TexlipsePlugin.log("Error while parsing the LaTeX output. " +
                    "Please consult the console output", null);
        }
        if (parser.needsLatexRerun()) {
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.SESSION_LATEX_RERUN, "true");
        }
        if (parser.needsBibRerun()) {
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.SESSION_BIBTEX_RERUN, "true");
        }

        final Set<IPath> inputFiles = parser.getInputFiles();
        final Set<String> externalNames = parser.getExternalNames();
        if (flsAnalyzer != null) {
            try {
                flsAnalyzer.parse();
                inputFiles.addAll(flsAnalyzer.getInputFiles());
                externalNames.addAll(flsAnalyzer.getExternalNames());
            }
            catch (IOException e) {
                createMarker(resource, -1, TexlipsePlugin
                        .getResourceString("builderErrorCannotReadFls"));
            }
        }
        TexlipseProperties.setSessionProperty(project,
                TexlipseProperties.SESSION_LATEX_INPUTFILE_SET, inputFiles);
        TexlipseProperties.setSessionProperty(project,
                TexlipseProperties.SESSION_LATEX_EXTERNALINPUT_SET, externalNames);

        return parser.hasErrors();
    }

}
