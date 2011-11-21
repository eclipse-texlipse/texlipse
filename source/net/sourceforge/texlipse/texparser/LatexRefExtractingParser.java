/*
 *
 * Copyright (c) 2004-2011 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.texparser;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.texparser.lexer.LexerException;
import net.sourceforge.texlipse.texparser.node.EOF;
import net.sourceforge.texlipse.texparser.node.TArgument;
import net.sourceforge.texlipse.texparser.node.TCbib;
import net.sourceforge.texlipse.texparser.node.TCbibstyle;
import net.sourceforge.texlipse.texparser.node.TClabel;
import net.sourceforge.texlipse.texparser.node.TCnew;
import net.sourceforge.texlipse.texparser.node.TCommentline;
import net.sourceforge.texlipse.texparser.node.TCpackage;
import net.sourceforge.texlipse.texparser.node.TCpindex;
import net.sourceforge.texlipse.texparser.node.TOptargument;
import net.sourceforge.texlipse.texparser.node.TStar;
import net.sourceforge.texlipse.texparser.node.TWhitespace;
import net.sourceforge.texlipse.texparser.node.Token;


/**
 * A LaTeX parser for extracting labels, BibTeX -information and
 * whether an index is to be generated or not.
 * 
 * @author Oskar Ojala
 */
public class LatexRefExtractingParser {
    
    private ArrayList<ReferenceEntry> labels;
    private ArrayList<TexCommandEntry> commands; //type: TexCommandEntry
    private List<String> bibs;
    private String bibstyle;
    private boolean biblatexMode;
    private String biblatexBackend;
    private boolean index;
    private boolean fatalErrors = false;

    private String preamble;
    
    /**
     * Extracts the preamble (if there is any) and stores a copy of it
     * in the field <code>preamble</code>. The preamble is assumed to
     * exist if the string contain the \documentclass command and it ends
     * where the document-environment begins or at the end of file.
     * 
     * The preamble stored will include the \beign{document} -command.
     * 
     * @param input The document
     */
    private void extractPreamble(String input) {
        this.preamble = TexParser.extractLaTeXPreamble(input);
    }

    private void initializeDatastructs() {
        //reserve enough space
        this.labels = new ArrayList<ReferenceEntry>(100);
        this.commands = new ArrayList<TexCommandEntry>();
        this.bibs = new ArrayList<String>();
        this.biblatexMode = false;
        this.biblatexBackend = null;
        this.index = false;
    }
    
    /**
     * Evaluates package loading options for biblatex and locates the backend
     * option.
     *
     * @param options string with options in format <code>key=value</code>,
     *  or simply <code>key</code>, each separated by commas
     * @return selected biblatex backend, if it was selected; otherwise null
     */
    private static String findBiblatexBackend(String options) {
        int beIdx = options.indexOf("backend=");
        if (beIdx > 0) {
            int startIdx = beIdx + 8; // move forward by length of "backend="
            int endIdx = options.indexOf(',', startIdx);
            if (endIdx > startIdx) {
                return options.substring(startIdx, endIdx).trim();
            }
            else if (endIdx == -1) {
                return options.substring(startIdx).trim();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Creates a new parser for extracting labels and BibTeX info.
     */
    public LatexRefExtractingParser() {
        initializeDatastructs();
    }
    
    /**
     * Parses the given string and extracts the labels and BibTeX info.
     * TexCommandEntry currentCommand = null;
     * @param input A string containing the LaTeX document
     * @throws IOException If the input is not readable
     */
    public void parse(String input) throws IOException {
        this.extractPreamble(input);
        LatexLexer lexer = new LatexLexer(new PushbackReader(new StringReader(input), 4096));
        boolean expectArg = false;
        boolean expectArg2 = false;
        Token prevToken = null;
        String packageOptions = null;
        
        //CommandEntry currentCommand = null;
        TexCommandEntry currentCommand = null;
        int argCount = 0;
        
        try {
            for (Token t = lexer.next(); !(t instanceof EOF); t = lexer.next()) { 
                if (expectArg) {
                    if (t instanceof TArgument) {
                        if (prevToken instanceof TClabel) {
                            //this.labels.add(new ReferenceEntry(t.getText()));
                            ReferenceEntry l = new ReferenceEntry(t.getText());
                            l.setPosition(t.getPos(), t.getText().length());
                            l.startLine = t.getLine();
                            l.setLabelInfo(input);
                            this.labels.add(l);
                        } else if (prevToken instanceof TCbib) {
                            String[] sBibs = t.getText().split(",");
                            for (String bib : sBibs) {
                                bibs.add(bib.trim());
                            }
                        } else if (prevToken instanceof TCbibstyle) {
                            this.bibstyle = t.getText();
                        } else if (prevToken instanceof TCnew) {
                            //currentCommand = new CommandEntry(t.getText().substring(1));
                            currentCommand = new TexCommandEntry(t.getText().substring(1), "", 0);
                            currentCommand.startLine = t.getLine();
                            expectArg2 = true;
                        } else if (prevToken instanceof TCpackage) {
                            if (t.getText().equals("biblatex")) {
                                biblatexMode = true;
                                if (packageOptions != null) {
                                    biblatexBackend = findBiblatexBackend(packageOptions);
                                    // reset
                                    packageOptions = null;
                                }
                            }
                        }
                        prevToken = null;
                        expectArg = false;
                    } else if (t instanceof TOptargument) {
                        if (prevToken instanceof TCpackage) {
                            packageOptions = t.getText();
                        }
                    } else if (!(t instanceof TWhitespace) && !(t instanceof TStar)
                            && !(t instanceof TCommentline)) {
                        
                        // this is an error condition, but we want a silent parse
                        prevToken = null;
                        expectArg = false;
                    }
                } else if (expectArg2) {
                    // we are capturing the second argument of a command with two arguments
                    // the only one of those that interests us is newcommand
                    if (t instanceof TArgument) {
                        currentCommand.info = t.getText();
                        commands.add(currentCommand);
                        argCount = 0;
                        expectArg2 = false;
                    } else if (t instanceof TOptargument) {
                        if (argCount == 0) {
                            try {
                                currentCommand.arguments = Integer.parseInt(t.getText());
                            } catch (NumberFormatException nfe) {
                                expectArg2 = false;
                            }
                        }
                        argCount++;
                    } else if (!(t instanceof TWhitespace) && !(t instanceof TCommentline)) {
                        argCount = 0;
                        expectArg2 = false;
                    }
                } else {
                    if (t instanceof TClabel || t instanceof TCbib || t instanceof TCbibstyle
                            || t instanceof TCnew || t instanceof TCpackage) {
                        prevToken = t;
                        expectArg = true;
                    } else if (t instanceof TCpindex)
                        this.index = true;
                }
            }
        } catch (LexerException e) {
            fatalErrors = true;
        }
    }
    
    /**
     * @return Returns the bibs.
     */
    public String[] getBibs() {
        return bibs.toArray(new String[0]);
    }
    /**
     * @return Returns the bibstyle.
     */
    public String getBibstyle() {
        return bibstyle;
    }
    /**
     * @return Whether biblatex mode is activated
     */
    public boolean isBiblatexMode() {
        return biblatexMode;
    }
    /**
     * @return The selected biblatex backend
     */
    public String getBiblatexBackend() {
        return biblatexBackend;
    }
    /**
     * @return Returns the index.
     */
    public boolean isIndex() {
        return index;
    }
    /**
     * @return Returns the labels.
     */
    public ArrayList<ReferenceEntry> getLabels() {
        return labels;
    }
    /**
     * @return Returns the commands.
     */
    public ArrayList<TexCommandEntry> getCommands() {
        return commands;
    }
    /**
     * @return Returns the preamble (contains \begin{document} at the end).
     */
    public String getPreamble() {
        return preamble;
    }
    /**
     * @return Returns the fatalErrors.
     */
    public boolean isFatalErrors() {
        return fatalErrors;
    }
}
