/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.bibparser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.texlipse.bibparser.lexer.LexerException;
import org.eclipse.texlipse.bibparser.node.Start;
import org.eclipse.texlipse.bibparser.parser.Parser;
import org.eclipse.texlipse.bibparser.parser.ParserException;
import org.eclipse.texlipse.model.ParseErrorMessage;


/**
 * BibTeX parser front-end. After creation, the parsing is done by calling
 * the getEntries() -method, after which getAbbrevs() and getErrors() should
 * be called (otherwise the data returned by these two is essentially meaningless.)
 * 
 * @author Oskar Ojala
 */
public class BibParser {
    
    private String filename;
    private Reader reader;
    
    private List<ParseErrorMessage> errors;
    private List warnings;
    private List tasks;
    private Start ast;
    
    /**
     * Constructs a new BibTeX parser.
     * 
     * @param filename The file to parse
     */
    public BibParser(String filename) {
        this.filename = filename;
        this.errors = new ArrayList<ParseErrorMessage>();
        this.warnings = new ArrayList();
    }
    
    /**
     * Constructs a new BibTeX parser.
     * 
     * @param r A reader to the BibTeX-data to parse
     */
    public BibParser(Reader r) {
        this.reader = r;
        this.errors = new ArrayList();
        this.warnings = new ArrayList();
    }
    
    /**
     * Parses the document, constructs a list of the entries and returns
     * them.
     * 
     * @return BibTeX entries (<code>ReferenceEntry</code>)
     */
//    public List<ReferenceEntry> getEntries() throws IOException, FileNotFoundException {
    public List getEntries() throws IOException, FileNotFoundException {
        try {
            BibLexer l;
            if (filename != null) {
                l = new BibLexer(new PushbackReader (new FileReader(filename), 1024));
            } else {
                l = new BibLexer(new PushbackReader(reader, 1024));
            }
            
            Parser p = new Parser(l);
            this.ast = p.parse();
            
            EntryRetriever er = new EntryRetriever();
            ast.apply(er);
            er.finishParse();
            warnings = er.getWarnings();
            tasks = er.getTasks();
            
            // FIXME
            // Search for files of the referenced material to be able to display
            //new Thread(new BibFileReferenceSearch(sortIndex, project)).start();
            
            return er.getEntries();
            
            // TODO modularize SableCC error parsing
        } catch (LexerException le) {
            String msg = le.getMessage();
            int first = msg.indexOf('[');
            int last = msg.indexOf(']');
            String numseq = msg.substring(first + 1, last);
            String[] numbers = numseq.split(",");
            this.errors.add(new ParseErrorMessage(Integer.parseInt(numbers[0]),
                    Integer.parseInt(numbers[1]) - 1,
                    2,
                    msg.substring(last+2),
                    IMarker.SEVERITY_ERROR));
        } catch (ParserException pe) {
            String msg = pe.getMessage();
            int last = msg.indexOf(']');
            this.errors.add(new ParseErrorMessage(pe.getToken().getLine(),
                    pe.getToken().getPos(),
                    pe.getToken().getText().length(),
                    msg.substring(last+2),
                    IMarker.SEVERITY_ERROR));
        }
        return null;
    }
    
    /**
     * @return Returns the abbreviations (<code>ReferenceEntry</code>)
     */
    public List getAbbrevs() {
        if (ast != null) {
            AbbrevRetriever ar = new AbbrevRetriever();
            ast.apply(ar);
            return ar.getAbbrevs();
        }
        return null;
    }
    
    /**
     * @return Returns the errors.
     */
    public List<ParseErrorMessage> getErrors() {
        return errors;
    }    
    
    /**
     * @return Returns the warnings.
     */
    public List getWarnings() {
        return warnings;
    }
    
    /**
     * @return Returns the tasks
     */
    public List getTasks() {
        return tasks;
    }
}
