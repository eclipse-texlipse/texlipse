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
import net.sourceforge.texlipse.model.ReferenceEntry;


/**
 * Retrieves the BibTeX abbreviations (defined with @string{...}) from the AST.
 * 
 * This class is a visitor, that is applied on the AST that is a result of parsing a
 * BibTeX-file. See <a href="http://www.sablecc.org">http://www.sablecc.org</a> for
 * more information on the structure of the AST and the visitors.
 * 
 * @author Oskar Ojala
 */
public final class AbbrevRetriever extends DepthFirstAdapter {

    private ArrayList abbrevs = new ArrayList(); //type: ReferenceEntry
    
    /**
     * @return The abbreviations as a list of <code>ReferenceEntry</code>s
     */
    public ArrayList getAbbrevs() {
        return abbrevs;
    }
    
    public void inABibtex(ABibtex node) {
    }
    
    public void outABibtex(ABibtex node) {
    }
    
    public void inAStrbraceStringEntry(AStrbraceStringEntry node) {
        abbrevs.add(new ReferenceEntry(node.getIdentifier().getText(), node.getStringLiteral().getText()));
    }
    
    public void outAStrbraceStringEntry(AStrbraceStringEntry node) {
    }

    public void inAStrparenStringEntry(AStrparenStringEntry node) {
        abbrevs.add(new ReferenceEntry(node.getIdentifier().getText(), node.getStringLiteral().getText()));
    }

    public void outAStrparenStringEntry(AStrparenStringEntry node) {
    }

    public void inAEntrybraceEntry(AEntrybraceEntry node) {
    }
    
    public void outAEntrybraceEntry(AEntrybraceEntry node) {
    }

    public void inAEntryparenEntry(AEntryparenEntry node) {
    }

    public void outAEntryparenEntry(AEntryparenEntry node) {
    }

    public void inAEntryDef(AEntryDef node) {
    }
    
    public void outAEntryDef(AEntryDef node) {
    }
    
    public void inAKeyvalDecl(AKeyvalDecl node) {
    }
    
    public void outAKeyvalDecl(AKeyvalDecl node) {
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
    }

    public void outAValueQValOrSid(AValueQValOrSid node) {
    }

    public void inANumValOrSid(ANumValOrSid node) {
    }
    
    public void outANumValOrSid(ANumValOrSid node) {
    }
    
    public void inAIdValOrSid(AIdValOrSid node) {
    }
    
    public void outAIdValOrSid(AIdValOrSid node) {
    }
}
