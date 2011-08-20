package net.sourceforge.texlipse.builder;

import java.util.StringTokenizer;

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IResource;


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
