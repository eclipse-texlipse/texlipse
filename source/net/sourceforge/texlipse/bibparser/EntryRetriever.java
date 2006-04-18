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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.texlipse.bibparser.analysis.DepthFirstAdapter;
import net.sourceforge.texlipse.bibparser.node.ABibeBibEntry;
import net.sourceforge.texlipse.bibparser.node.ABibstreBibEntry;
import net.sourceforge.texlipse.bibparser.node.ABibtaskBibEntry;
import net.sourceforge.texlipse.bibparser.node.ABibtex;
import net.sourceforge.texlipse.bibparser.node.AConcat;
import net.sourceforge.texlipse.bibparser.node.AEntryDef;
import net.sourceforge.texlipse.bibparser.node.AEntrybraceEntry;
import net.sourceforge.texlipse.bibparser.node.AEntryparenEntry;
import net.sourceforge.texlipse.bibparser.node.AIdValOrSid;
import net.sourceforge.texlipse.bibparser.node.AKeyvalDecl;
import net.sourceforge.texlipse.bibparser.node.ANumValOrSid;
import net.sourceforge.texlipse.bibparser.node.AStrbraceStringEntry;
import net.sourceforge.texlipse.bibparser.node.AStrparenStringEntry;
import net.sourceforge.texlipse.bibparser.node.AValueBValOrSid;
import net.sourceforge.texlipse.bibparser.node.AValueQValOrSid;
import net.sourceforge.texlipse.bibparser.node.TIdentifier;
import net.sourceforge.texlipse.bibparser.node.TStringLiteral;
import net.sourceforge.texlipse.bibparser.node.Token;
import net.sourceforge.texlipse.model.ParseErrorMessage;
import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.core.resources.IMarker;


/**
 * Retrieves the BibTeX entries from the AST.
 * 
 * This class is a visitor, that is applied on the AST that is a result of parsing a
 * BibTeX-file. See <a href="http://www.sablecc.org">http://www.sablecc.org</a> for
 * more information on the structure of the AST and the visitors.
 * 
 * @author Oskar Ojala
 */
public final class EntryRetriever extends DepthFirstAdapter {
    
    private final class EntryText {
        Token token;
        Set definedFields;
        public EntryText(Token t, Set df) {
            token = t;
            definedFields = df;
        }
    }
    
    //private ArrayList<ParseErrorMessage> warnings = new ArrayList<ParseErrorMessage>();
    private List warnings = new ArrayList();
   
    private List tasks = new ArrayList(); // type: ParseErrorMessage
    
    //private List<ReferenceEntry> entries = new ArrayList<ReferenceEntry>();
    private List entries = new ArrayList();
    
    private ReferenceEntry currEntry;
    private StringBuffer currEntryInfo;    
    private Token currEntryType;
    private String currField;
    private String crossref;

    /**
     * Currently defined fields for an entry
     */
    private Set currDefinedFields;

    /**
     * All defined keys -- can be used for testing whether a key is unique
     */
    private Set allDefinedKeys;
    
    private static final Map predefAbbrevs = new HashMap();
    private Map abbrevs;
    private Map crossrefs; // String->List(EntryText)
    
    /**
     * A list of required fields for the different BibTeX entries
     */
    //private static final Map<String, List<String>> requiredFieldsPerType = new HashMap<String, List<String>>();
    private static final Map requiredFieldsPerType = new HashMap();
    
    static {
        predefAbbrevs.put("jan", "January");
        predefAbbrevs.put("feb", "February");
        predefAbbrevs.put("mar", "March");
        predefAbbrevs.put("apr", "April");
        predefAbbrevs.put("may", "May");
        predefAbbrevs.put("jun", "June");
        predefAbbrevs.put("jul", "July");
        predefAbbrevs.put("aug", "August");
        predefAbbrevs.put("sep", "September");
        predefAbbrevs.put("oct", "October");
        predefAbbrevs.put("nov", "November");
        predefAbbrevs.put("dec", "December");

        String[] article = {"author", "title", "journal", "year"};                     
        String[] book = {"title", "publisher", "year"};
        String[] booklet = {"title"};
        String[] conference = {"author", "title", "booktitle", "year"};
        String[] inbook = {"title", "publisher", "year"};
        String[] incollection = {"author", "title", "booktitle", "publisher", "year"};
        String[] inproceedings = {"author", "title", "booktitle", "year"};;
        String[] manual = {"title"};
        String[] mastersthesis = {"author", "title", "school", "year"};
        String[] phdthesis = {"author", "title", "school", "year"};        
        String[] techreport = {"author", "title", "institution", "year"};
        String[] proceedings = {"title", "year"};
        String[] unpublished = {"author", "title", "note"};

//        requiredFieldsPerType.put("article", new ArrayList<String>(Arrays.asList(article)));
//        requiredFieldsPerType.put("book", new ArrayList<String>(Arrays.asList(book)));
//        requiredFieldsPerType.put("booklet", new ArrayList<String>(Arrays.asList(booklet)));
//        requiredFieldsPerType.put("conference", new ArrayList<String>(Arrays.asList(conference)));
//        requiredFieldsPerType.put("inbook", new ArrayList<String>(Arrays.asList(inbook)));
//        requiredFieldsPerType.put("incollection", new ArrayList<String>(Arrays.asList(incollection)));
//        requiredFieldsPerType.put("inproceedings", new ArrayList<String>(Arrays.asList(inproceedings)));
//        requiredFieldsPerType.put("manual", new ArrayList<String>(Arrays.asList(manual)));
//        requiredFieldsPerType.put("mastersthesis", new ArrayList<String>(Arrays.asList(mastersthesis)));        
//        requiredFieldsPerType.put("phdthesis", new ArrayList<String>(Arrays.asList(phdthesis)));
//        requiredFieldsPerType.put("techreport", new ArrayList<String>(Arrays.asList(techreport)));
//        requiredFieldsPerType.put("proceedings", new ArrayList<String>(Arrays.asList(proceedings)));
//        requiredFieldsPerType.put("unpublished", new ArrayList<String>(Arrays.asList(unpublished)));

        requiredFieldsPerType.put("article", new ArrayList(Arrays.asList(article)));
        requiredFieldsPerType.put("book", new ArrayList(Arrays.asList(book)));
        requiredFieldsPerType.put("booklet", new ArrayList(Arrays.asList(booklet)));
        requiredFieldsPerType.put("conference", new ArrayList(Arrays.asList(conference)));
        requiredFieldsPerType.put("inbook", new ArrayList(Arrays.asList(inbook)));
        requiredFieldsPerType.put("incollection", new ArrayList(Arrays.asList(incollection)));
        requiredFieldsPerType.put("inproceedings", new ArrayList(Arrays.asList(inproceedings)));
        requiredFieldsPerType.put("manual", new ArrayList(Arrays.asList(manual)));
        requiredFieldsPerType.put("mastersthesis", new ArrayList(Arrays.asList(mastersthesis)));        
        requiredFieldsPerType.put("phdthesis", new ArrayList(Arrays.asList(phdthesis)));
        requiredFieldsPerType.put("techreport", new ArrayList(Arrays.asList(techreport)));
        requiredFieldsPerType.put("proceedings", new ArrayList(Arrays.asList(proceedings)));
        requiredFieldsPerType.put("unpublished", new ArrayList(Arrays.asList(unpublished)));    
    }

    public EntryRetriever() {
        this.currDefinedFields = new HashSet();
        
        this.allDefinedKeys = new HashSet();
        this.abbrevs = new HashMap(predefAbbrevs);
        this.crossrefs = new HashMap();
    }
    
    /**
     * @return The entries as a list of <code>ReferenceEntry</code>s
     */
    public List getEntries() {
        return entries;
    }
    
    /**
     * @return A list of warnings in the file
     */
    public List getWarnings() {
        return warnings;
    }
    
    /**
     * @return A list of task markers in the file
     */
    public List getTasks() {
        return tasks;
    }

    /**
     * Finish the parse by setting all remaining warnings
     */
    public void finishParse() {
        // Set warnings for unfulfilled cross references
        Set keys = crossrefs.entrySet();
        for (Iterator iter = keys.iterator(); iter.hasNext();) {
            Map.Entry mapping = (Map.Entry) iter.next();
            List crefs = (List) mapping.getValue();
            for (Iterator iter2 = crefs.iterator(); iter2.hasNext();) {
                EntryText et = (EntryText) iter2.next();
                setMissingWarnings(et.token, et.definedFields);
                warnings.add(new ParseErrorMessage(et.token.getLine(),
                        et.token.getPos() - 1, et.token.getText().length(),
                        "Cross reference " + mapping.getKey() + " does not exist",
                        IMarker.SEVERITY_WARNING));
            }
        }
    }
    
    public void inABibtex(ABibtex node) {
    }
    
    public void outABibtex(ABibtex node) {
    }
    
    public void inABibeBibEntry(ABibeBibEntry node) {
    }

    public void inABibstreBibEntry(ABibstreBibEntry node) {
    }

    public void inABibtaskBibEntry(ABibtaskBibEntry node) {
    }

    public void outABibeBibEntry(ABibeBibEntry node) {
    }

    public void outABibstreBibEntry(ABibstreBibEntry node) {
    }

    public void outABibtaskBibEntry(ABibtaskBibEntry node) {
        int start = node.getTaskcomment().getText().indexOf("TODO");
        String taskText = node.getTaskcomment().getText().substring(start + 4).trim();
        
        tasks.add(new ParseErrorMessage(node.getTaskcomment().getLine(),
                node.getTaskcomment().getPos(),
                taskText.length(), taskText, IMarker.SEVERITY_INFO));
    }

    private void inAnAbbrev(TIdentifier tid, TStringLiteral tsl) {
        if (abbrevs.put(tid.getText(), tsl.getText()) != null) {
            warnings.add(new ParseErrorMessage(tid.getLine(),
                    tid.getPos() - 1, tid.getText().length(),
                    "String key " + tid.getText() + " is not unique",
                    IMarker.SEVERITY_WARNING));
        }
    }
    
    public void inAStrbraceStringEntry(AStrbraceStringEntry node) {
        inAnAbbrev(node.getIdentifier(), node.getStringLiteral());
    }
    
    public void outAStrbraceStringEntry(AStrbraceStringEntry node) {
    }
    
    public void inAStrparenStringEntry(AStrparenStringEntry node) {
        inAnAbbrev(node.getIdentifier(), node.getStringLiteral());
    }
    
    public void outAStrparenStringEntry(AStrparenStringEntry node) {
    }
    
    private void inBibtexEntry(TIdentifier tid) {
        currEntry = new ReferenceEntry(tid.getText());
        currEntry.startLine = tid.getLine();
        currEntryInfo = new StringBuffer();
        
        if (!allDefinedKeys.add(currEntry.key)) {
            warnings.add(new ParseErrorMessage(currEntry.startLine,
                    tid.getPos() - 1, currEntry.key.length(),
                    "BibTex key " + currEntry.key + " is not unique",
                    IMarker.SEVERITY_WARNING));
        }
    }

    private void setMissingWarnings(Token t, Set fields) {
        List reqFieldList = (List) requiredFieldsPerType.get(t.getText());
        if (reqFieldList != null) {
            if (!fields.containsAll(reqFieldList)) {
                for (Iterator iter = reqFieldList.iterator(); iter.hasNext();) {
                    String reqField = (String) iter.next();
                    if (!fields.contains(reqField)) {
                        // FIXME key
                        warnings.add(new ParseErrorMessage(t.getLine(),
                                t.getPos()-1, t.getText().length(),
                                t + currEntry.key +
                                " is missing required field " + reqField,
                                IMarker.SEVERITY_WARNING));                 
                    }
                }
            }
        }   
    }
    
    private void outBibtexEntry(Token endToken) {
        if (currEntry.author == null) {
            currEntry.author = "-";
        }
        if (currEntry.year == null) {
            currEntry.year = "-";
        }
        if (currEntry.journal == null) {
            currEntry.journal = "-";
        }
        currEntry.info = currEntryInfo.toString();
        currEntry.endLine = endToken.getLine();
        entries.add(currEntry);
        // TODO useless -- uses the wrong token
        //currEntry.endLine = node.getIdentifier().getLine();

        if (crossref != null) {
            List crefs = (List) crossrefs.get(crossref);
            if (crefs == null) {
                crefs = new ArrayList();
            }
            crefs.add(new EntryText(currEntryType,
                    new HashSet(currDefinedFields)));
            crossrefs.put(crossref, crefs);
            crossref = null;
        } else {
            setMissingWarnings(currEntryType, currDefinedFields);
        }
        if (crossrefs.containsKey(currEntry.key)) {
            List crefs = (List) crossrefs.remove(currEntry.key);
            for (Iterator iter = crefs.iterator(); iter.hasNext();) {
                EntryText et = (EntryText) iter.next();
                et.definedFields.addAll(currDefinedFields);
                setMissingWarnings(et.token, et.definedFields);
            }
        }
        currDefinedFields.clear();
    }
    
    /**
     * Called when entering a bibliography entry, starts
     * forming an entry for the entry list
     *
     * @param node an <code>AEntry</code> value
     */
    public void inAEntrybraceEntry(AEntrybraceEntry node) {
        inBibtexEntry(node.getIdentifier());
    }
    
    /**
     * Called when exiting a bibliography entry, adds the formed
     * entry into the entry list
     *
     * @param node an <code>AEntry</code> value
     */
    public void outAEntrybraceEntry(AEntrybraceEntry node) {
        outBibtexEntry(node.getRBrace());
    }
    
    public void inAEntryparenEntry(AEntryparenEntry node) {
        inBibtexEntry(node.getIdentifier());
    }
    
    public void outAEntryparenEntry(AEntryparenEntry node) {
        outBibtexEntry(node.getRParen());
    }
    
    public void inAEntryDef(AEntryDef node) {
    }
    
    /**
     * Handles the type of the bibliography entry
     *
     * @param node an <code>AEntryDef</code> value
     */
    public void outAEntryDef(AEntryDef node) {        
        currEntryInfo.append(node.getEntryName().getText().substring(1));
        currEntryInfo.append('\n');
        currEntryType = node.getEntryName();
        currEntryType.setText(currEntryType.getText().substring(1).toLowerCase());
    }
    
    public void inAKeyvalDecl(AKeyvalDecl node) {
        currField = node.getIdentifier().getText().toLowerCase();
        currEntryInfo.append(currField);
        currEntryInfo.append(": ");
                
        if (!currDefinedFields.add(currField)) {
            warnings.add(new ParseErrorMessage(node.getIdentifier().getLine(),
                    node.getIdentifier().getPos() - 1, currField.length(),
                    "Field " + currField + " appears more than once in entry " + currEntry.key,
                    IMarker.SEVERITY_WARNING));
        }
    }
    
    public void outAKeyvalDecl(AKeyvalDecl node) {
        currEntryInfo.append('\n');
    }
    
    public void inAConcat(AConcat node) {
    }
    
    public void outAConcat(AConcat node) {
    }
    
    public void inAValueBValOrSid(AValueBValOrSid node) {
    }

    public void inAValueQValOrSid(AValueQValOrSid node) {
    }

    public void outAValueBValOrSid(AValueBValOrSid node) {
        outAValueValOrSid(node.getStringLiteral().getText(),
                node.getStringLiteral());
    }

    public void outAValueQValOrSid(AValueQValOrSid node) {
        TStringLiteral tsl = node.getStringLiteral();
        if (tsl != null) {
            outAValueValOrSid(tsl.getText(), tsl);
        } else {
            warnings.add(new ParseErrorMessage(currEntry.startLine,
                    1, currEntryType.getText().length(),
                    currField + " is empty in " + currEntry.key,
                    IMarker.SEVERITY_WARNING));
        }
    }
    
    private void outAValueValOrSid(String text, Token tsl) {
        String fieldValue = text.replaceAll("\\s+", " ");
        currEntryInfo.append(fieldValue);
        
        if ("author".equals(currField) || "editor".equals(currField)) {
            currEntry.author = fieldValue;
        } else if ("journal".equals(currField)) {
            currEntry.journal = fieldValue;
        } else if ("year".equals(currField)) {
            currEntry.year = fieldValue;
        } else if ("crossref".equals(currField)) {
            crossref = fieldValue;
        }
        
        // Test for empty fields
        if (fieldValue.equalsIgnoreCase("")) {
            warnings.add(new ParseErrorMessage(tsl.getLine(),
                    tsl.getPos(), 0,
                    currField + " is empty in " + currEntry.key,
                    IMarker.SEVERITY_WARNING));                 
        }
    }

    public void inANumValOrSid(ANumValOrSid node) {
    }
  
    public void outANumValOrSid(ANumValOrSid node) {
        outAValueValOrSid(node.getNumber().getText(), node.getNumber());
    }
    
    public void inAIdValOrSid(AIdValOrSid node) {
    }
    
    public void outAIdValOrSid(AIdValOrSid node) {
        TIdentifier tid = node.getIdentifier();
        String expansion = (String) abbrevs.get(tid.getText());
        if (expansion != null) {
            outAValueValOrSid(expansion, tid);
        } else {
            warnings.add(new ParseErrorMessage(tid.getLine(),
                    tid.getPos()-1, tid.getText().length(),
                    "The abbreviation " + tid.getText() + " is undefined",
                    IMarker.SEVERITY_WARNING));
        }
    }
}
