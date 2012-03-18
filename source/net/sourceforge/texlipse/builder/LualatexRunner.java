package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.builder.factory.RunnerDescription;


/**
 * Run the external Lualatex program.
 * 
 * @author Boris von Loesch
 */
public class LualatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public LualatexRunner(RunnerDescription description) {
        super(description);
    }

}
