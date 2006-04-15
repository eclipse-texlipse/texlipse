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
    
    //private ArrayList<ParseErrorMessage> warnings = new ArrayList<ParseErrorMessage>();
    private ArrayList warnings = new ArrayList();
    
    private List entries = new ArrayList(); //type: ReferenceEntry
    
//    private Hashtable<String, BibStringTriMap<ReferenceEntry>> sortIndex;
    private ReferenceEntry currEntry;
    private StringBuffer currEntryInfo;    
    private String currEntryType;
    private String currField;

    /**
     * Currently defined fields for an entry
     */
    private Set currDefinedFields;

    /**
     * All defined keys -- can be used for testing whether a key is unique
     */
    private Set allDefinedKeys;
    
    /**
     * A list of required fields for the different BibTeX entries
     */
    //private static Map<String, List<String>> requiredFieldsPerType;
    private static Map requiredFieldsPerType;
    
    public void initializeStatics() {
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

//        requiredFieldsPerType = new HashMap<String, List<String>>();
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

        requiredFieldsPerType = new HashMap();
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
        if (requiredFieldsPerType == null) {
            initializeStatics();
        }
        
        this.currDefinedFields = new HashSet();
        
        this.allDefinedKeys = new HashSet();
        
//        sortIndex = new Hashtable<String, BibStringTriMap<ReferenceEntry>>();
//
//        // indexkey is a special field representing the bibtex key 
//        sortIndex.put("indexkey", new BibStringTriMap<ReferenceEntry>(true));
//
//        // List of the different fields to be indexed and available for completion proposals
//        sortIndex.put("journal", new BibStringTriMap<ReferenceEntry>(false));
//        
//        BibStringTriMap<ReferenceEntry> author = new BibStringTriMap<ReferenceEntry>(false);
//        sortIndex.put("author", author);
//        sortIndex.put("editor", author);
//        
//        BibStringTriMap<ReferenceEntry> institution = new BibStringTriMap<ReferenceEntry>(false);
//        sortIndex.put("institution", institution);
//        sortIndex.put("school", institution);
//        
//        sortIndex.put("year", new BibStringTriMap<ReferenceEntry>(false));
//        sortIndex.put("booktitle", new BibStringTriMap<ReferenceEntry>(false));
    }
    
    /**
     * @return The entries as a list of <code>ReferenceEntry</code>s
     */
//    public ArrayList<ReferenceEntry> getEntries() {
//        return sortIndex.get("indexkey").getValues();
//    }
    public List getEntries() {
        return entries;
    }
    
    /**
     * @return A list of warnings in the file
     */
    public ArrayList getWarnings() {
        return warnings;
    }
    
    /**
     * @return The index structure of this bib file
     */
//    public Hashtable<String,BibStringTriMap<ReferenceEntry>> getSortIndex() {
//        return sortIndex;
//    }
    
    public void inABibtex(ABibtex node) {
    }
    
    public void outABibtex(ABibtex node) {
    }
    
    public void inAStrbraceStringEntry(AStrbraceStringEntry node) {
    }
    
    public void outAStrbraceStringEntry(AStrbraceStringEntry node) {
    }
    
    public void inAStrparenStringEntry(AStrparenStringEntry node) {
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
        
//        try {
//            sortIndex.get("indexkey").put(currEntry.key, currEntry);
//        } catch (NonUniqueException e) {
//        }
    }

    private void outBibtexEntry() {
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
        entries.add(currEntry);
        // useless -- uses the wrong token
        //currEntry.endLine = node.getIdentifier().getLine();

        List reqFieldList = (List) requiredFieldsPerType.get(currEntryType);
        if (reqFieldList != null) {
            if (!currDefinedFields.containsAll(reqFieldList)) {
                for (Iterator iter = reqFieldList.iterator(); iter.hasNext();) {
                    String reqField = (String) iter.next();
                    if (!currDefinedFields.contains(reqField)) {
                        warnings.add(new ParseErrorMessage(currEntry.startLine,
                                0, currEntryType.length() + 1,
                                currEntryType + " " + currEntry.key +
                                " is missing required field " + reqField,
                                IMarker.SEVERITY_WARNING));                 
                    }
                }
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
        outBibtexEntry();
    }
    
    public void inAEntryparenEntry(AEntryparenEntry node) {
        // FIXME check that this is correct
        inBibtexEntry(node.getIdentifier());
    }
    
    public void outAEntryparenEntry(AEntryparenEntry node) {
        outBibtexEntry();
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
        currEntryType = node.getEntryName().getText().substring(1).toLowerCase();
    }
    
    public void inAKeyvalDecl(AKeyvalDecl node) {
        currField = node.getIdentifier().getText().toLowerCase();
        currEntryInfo.append(currField);
        currEntryInfo.append(": ");
                
        if (!currDefinedFields.add(currField)) {
            warnings.add(new ParseErrorMessage(node.getIdentifier().getLine(),
                    node.getIdentifier().getPos() - 1, currField.length(),
                    "Field " + currField + " appears more than once in entry " + currField,
                    IMarker.SEVERITY_WARNING));
        }
        
        // Can't currently handle crossref correctly. Use a safe approximation
        // of assuming all required fields were added via crossref.
        //if (currField.equals("crossref"))
        //    currRequiredFields.clear();
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
        outAValueValOrSid(node.getStringLiteral());
    }

    public void outAValueQValOrSid(AValueQValOrSid node) {
        TStringLiteral tsl = node.getStringLiteral();
        if (tsl != null) {
            outAValueValOrSid(tsl);
        } else {
            warnings.add(new ParseErrorMessage(currEntry.startLine,
                    1, currEntryType.length(),
                    currField + " is empty in " + currEntry.key,
                    IMarker.SEVERITY_WARNING));
        }
    }
    
    private void outAValueValOrSid(Token tsl) {
        //currEntryInfo.append(node.getStringLiteral().getText().replaceAll("\\s+", " "));
        String fieldValue = tsl.getText().replaceAll("\\s+", " ");
        
        currEntryInfo.append(fieldValue);
        
        // TODO testing new nodes
        if ("author".equals(currField) || "editor".equals(currField)) {
            currEntry.author = fieldValue;
        } else if ("journal".equals(currField)) {
            currEntry.journal = fieldValue;
        } else if ("year".equals(currField)) {
            currEntry.year = fieldValue;
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
        outAValueValOrSid(node.getNumber());
    }
    
    public void inAIdValOrSid(AIdValOrSid node) {
    }
    
    public void outAIdValOrSid(AIdValOrSid node) {
        // FIXME we need to check that the node is defined
        //currEntryInfo.append(node.getIdentifier().getText());
        outAValueValOrSid(node.getIdentifier());
    }
}
