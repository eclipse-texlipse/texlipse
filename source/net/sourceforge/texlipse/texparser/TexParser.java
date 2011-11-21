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

import net.sourceforge.texlipse.model.DocumentReference;
import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.ParseErrorMessage;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.texparser.lexer.LexerException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;



/**
 * Parser front-end for parsing LaTeX files and extracting some relevant
 * information from them. The front-end provides an upper-level interface
 * to the parser and does some pre-processing and error-handling.
 * 
 * @author Oskar Ojala
 */
public class TexParser {

    private IDocument inputDoc;
    private LatexParser lparser;
//    private LatexLexer llexer;
    
    private List<ParseErrorMessage> errors;
    private boolean fatalErrors;
    
    private String preamble;

    /**
     * @param input The string representing the document to parse
     */
    public TexParser(IDocument input) {
    	this.inputDoc = input;
        this.lparser = new LatexParser();
        this.fatalErrors = false;
    }

    
    /**
     * Removes trailing whitespace from the document. This is needed since
     * the lexer and Eclipse have a different view of how the positions in
     * the trailing whitespace work out, so it's best to just remove it.
     * 
     * @param input The document to process
     * @return The document with trailing whitespace removed
     * 
     * @see Character.isWhitespace
     */
    private String rmTrailingWhitespace(String input) {
        int lastChar = input.length() - 1;
        while (lastChar >= 0 && Character.isWhitespace(input.charAt(lastChar)))
            lastChar--;
        lastChar++;
        if (lastChar < input.length())
            return input.substring(0, lastChar);
        return input;
    }
    
    static String extractLaTeXPreamble(String input) {
        /*if (LatexParserUtils.findCommand(input, "\\documentclass", 0) == -1
                && LatexParserUtils.findCommand(input, "\\documentstyle", 0) == -1) {
            return null;
        }*/
        
        IRegion region = LatexParserUtils.findBeginEnvironment(input, "document", 0);
        if (region != null) {
            return input.substring(0, region.getOffset() + region.getLength());
        } else {
            return null;
        }
    }
    
    /**
     * Extracts the preamble (if there is any) and stores a copy of it
     * in the field <code>preamble</code>. The preamble is assumed to
     * exist if the string contain the \documentclass command and it ends
     * where the document-environment begins or at the end of file.
     * 
     * The preamble stored will include the \begin{document} -command.
     * 
     * @param input The document
     */
    private void extractPreamble(String input) {
/*
        // These regexps lead to stack overflows in the regexp parser in some occasions.

        // (?:\r|\n|^)(?:(?:\\%|[^%\r\n])*?(?:\\%|[^\\%]))?\\document(?:class|style)(?:\W|$)
        Pattern docclass = Pattern.compile("(?:\\r|\\n|^)(?:(?:\\\\%|[^%\\r\\n])*?(?:\\\\%|[^\\\\%]))?\\\\document(?:class|style)(?:\\W|$)");
        Matcher m = docclass.matcher(input);
        if (m.find()) {
            
            // (?:\r|\n|^)(?:(?:\\%|[^%\r\n])*?(?:\\%|[^\\%]))?\\begin\s*\{document\}
            Pattern begindoc = Pattern.compile("(?:\\r|\\n|^)(?:(?:\\\\%|[^%\\r\\n])*?(?:\\\\%|[^\\\\%]))?\\\\begin\\s*\\{document\\}");
            Matcher m2 = begindoc.matcher(input);
            if (m2.find(m.end() - 1)) {
                this.preamble = input.substring(0, m2.end());
                return;
            }
        }
        this.preamble = null;
        return;*/
        
        this.preamble = extractLaTeXPreamble(input);
    }

    
    /**
     * Parses the input
     * 
     * @throws IOException
     */
    public void parseDocument(boolean checkForMissingSections) throws IOException {
        parseDocument(inputDoc.get(), checkForMissingSections);
    }
    
    /**
     * Parses the document
     * 
     * @throws IOException
     */
    public void parseDocument(String input, boolean checkForMissingSections) throws IOException {
        
        // remove trailing ws (this is because a discrepancy in the lexer's 
        // and IDocument's line counting for trailing whitespace)
        input = this.rmTrailingWhitespace(input);
        
        this.extractPreamble(input);
        
        try {
            // start the parse
            LatexLexer lexer = new LatexLexer(new PushbackReader(new StringReader(input), 4096));
            //LatexLexer lexer = this.getLexer(input); 
            if (this.preamble != null) {
                OutlineNode on = new OutlineNode("Preamble",
                        OutlineNode.TYPE_PREAMBLE,
                        1, null);
                lparser.parse(lexer, on, checkForMissingSections);
            } else {
                lparser.parse(lexer, checkForMissingSections);
            }
            this.errors = lparser.getErrors();
            this.fatalErrors = lparser.isFatalErrors();
        } catch (LexerException e) {
            // we must parse the lexer exception into a suitable format
            String msg = e.getMessage();
            int first = msg.indexOf('[');
            int last = msg.indexOf(']');
            String numseq = msg.substring(first + 1, last);
            String[] numbers = numseq.split(",");
            this.errors = new ArrayList<ParseErrorMessage>(1);
            this.errors.add(new ParseErrorMessage(Integer.parseInt(numbers[0]),
                    Integer.parseInt(numbers[1]),
                    2,
                    msg.substring(last+2),
                    IMarker.SEVERITY_ERROR));
            this.fatalErrors = true;
        }
    }

    /**
     * @return The outline tree
     */
    public ArrayList<OutlineNode> getOutlineTree() {
    	return lparser.getOutlineTree();
    }

    /**
     * @return The labels <code>ArrayList<ReferenceEntry></code>
     */
    public List<ReferenceEntry> getLabels() {
    	List<ReferenceEntry> labels = lparser.getLabels();
    	for (ReferenceEntry label : labels) {
    		label.setLabelInfo(inputDoc.get());
    	}
        return labels;
    }
    


    /**
     * @return The cite-references
     */
    public List<DocumentReference> getCites() {
        return lparser.getCites();
    }
    
    /**
     * @return Returns the errors.
     */
    public List<ParseErrorMessage> getErrors() {
        return errors;
    }
    
    /**
     * @return The bibliography files to include
     */
    public String[] getBibs() {
    	return lparser.getBibs();
    }
    
    /**
     * @return The style of the bibiliography entries
     */
    public String getBibstyle() {
    	return lparser.getBibstyle();
    }

    /**
     * @return Whether Biblatex mode is activated
     */
    public boolean isBiblatexMode() {
        return lparser.isBiblatexMode();
    }

    /**
     * @return The selected biblatex backend
     */
    public String getBiblatexBackend() {
        return lparser.getBiblatexBackend();
    }

    /**
     * @return Whether the parsed file contains a bibliography print command.
     *  This is only relevant if biblatex mode is enabled.
     */
    public boolean isLocalBib() {
        return lparser.isLocalBib();
    }

    /**
     * @return Returns the preamble.
     */
    public String getPreamble() {
        return preamble;
    }
    
    /**
     * @return True if the document has an index, false otherwise
     */
    public boolean isIndex() {
        return lparser.isIndex();
    }
    
    /**
     * @return True if there were fatal errors due to which parsing couldn't be successfully completed
     */
    public boolean isFatalErrors() {
        return fatalErrors;
    }
    
    /**
     * @return Get all \ref -references
     */
    public List<DocumentReference> getRefs() {
        return lparser.getRefs();
    }
    
    /**
     * @return Get user-defined commands
     */
    public ArrayList<TexCommandEntry> getCommands() {
        return lparser.getCommands();
    }
    
    /**
     * @return The tasks to mark
     */
    public List<ParseErrorMessage> getTasks() {
        return lparser.getTasks();
    }
    
    /**
     * @return The input commands in this document
     */
    public List<OutlineNode> getInputs() {
        return lparser.getInputs();
    }
    
}
