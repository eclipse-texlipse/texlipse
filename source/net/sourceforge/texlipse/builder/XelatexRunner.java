package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.builder.factory.RunnerDescription;


/**
 * Run the external Xelatex program.
 * 
 * @author Boris von Loesch
 */
public class XelatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public XelatexRunner(RunnerDescription description) {
        super(description);
    }

}
