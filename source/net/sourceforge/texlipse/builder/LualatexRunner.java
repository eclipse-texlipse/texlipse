package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.properties.TexlipseProperties;

/**
 * Run the external Lualatex program.
 * 
 * @author Boris von Loesch
 */
public class LualatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public LualatexRunner() {
        super();
    }
    
    protected String getWindowsProgramName() {
        return "lualatex.exe";
    }
    
    protected String getUnixProgramName() {
        return "lualatex";
    }
    
    public String getDescription() {
        return "LuaLatex program";
    }
    
    /**
     * Enable SyncTeX
     */
    public String getDefaultArguments() {
        return "-synctex=1 "+super.getDefaultArguments();
    }

    /**
     * @return output file format (pdf)
     */
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PDF;
    }

}
