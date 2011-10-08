package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.properties.TexlipseProperties;

/**
 * Run the external Xelatex program.
 * 
 * @author Boris von Loesch
 */
public class XelatexRunner extends LatexRunner {

    /**
     * Create a new ProgramRunner.
     */
    public XelatexRunner() {
        super();
    }
    
    protected String getWindowsProgramName() {
        return "xelatex.exe";
    }
    
    protected String getUnixProgramName() {
        return "xelatex";
    }
    
    public String getDescription() {
        return "XeLatex program";
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
