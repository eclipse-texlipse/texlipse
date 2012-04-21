package net.sourceforge.texlipse.builder;

import java.util.StringTokenizer;

import net.sourceforge.texlipse.builder.factory.RunnerDescription;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;


/**
 * Runs Biber, a bibliography sorting utility for BibLaTeX.
 *
 * @author Matthias Erll
 *
 */
public class BiberRunner extends AbstractProgramRunner {

    public BiberRunner(RunnerDescription description) {
        super(description);
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
            } else if (s.startsWith("WARN")) {
                createMarker(resource, null, s, IMarker.SEVERITY_WARNING);
            }
        }
        return hasErrors;
    }

}
