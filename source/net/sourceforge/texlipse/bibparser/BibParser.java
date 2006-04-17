/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibparser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import net.sourceforge.texlipse.bibparser.lexer.LexerException;
import net.sourceforge.texlipse.bibparser.node.Start;
import net.sourceforge.texlipse.bibparser.parser.Parser;
import net.sourceforge.texlipse.bibparser.parser.ParserException;
import net.sourceforge.texlipse.model.ParseErrorMessage;
import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;


/**
 * BibTeX parser front-end. After creation, the parsing is done by calling
 * the getEntries() -method, after which getAbbrevs() and getErrors() should
 * be called (otherwise the data returned by these two is essentially meaningless.)
 * 
 * @author Oskar Ojala
 */
public class BibParser {
    
    private IProject project;
    private String filename;
    private Reader reader;
    
    private ArrayList errors;
    private ArrayList warnings;
//    private Hashtable<String, BibStringTriMap<ReferenceEntry>> sortIndex;    
    private Start ast;
    
    /**
     * Constructs a new BibTeX parser.
     * 
     * @param filename The file to parse
     */
    public BibParser(String filename, IProject project) {
        this.filename = filename;
        this.project = project;
        this.errors = new ArrayList();
        this.warnings = new ArrayList();
    }
    
    /**
     * Constructs a new BibTeX parser.
     * 
     * @param r A reader to the BibTeX-data to parse
     */
    public BibParser(Reader r, IProject project) {
        this.project = project;
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
//            sortIndex = er.getSortIndex();
            
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
    public ArrayList getAbbrevs() {
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
    public ArrayList getErrors() {    	
        return errors;
    }    
    
    /**
     * @return Returns the warnings.
     */
    public ArrayList getWarnings() {    	
        return warnings;
    }
    
    /**
     * @return Returns the index structure of the bib file.
     */
//    public Hashtable<String, BibStringTriMap<ReferenceEntry>> getSortIndex() {
//        return sortIndex;
//    }    
    
}
