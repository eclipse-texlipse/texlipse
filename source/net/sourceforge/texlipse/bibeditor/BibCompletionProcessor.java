/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibeditor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.templates.BibTexTemplateCompletion;
import net.sourceforge.texlipse.templates.ProposalsComparator;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;


/**
 * Handles abbrev and template completions for the BibTeX
 * editor.
 * 
 * @author Oskar Ojala
 */
public class BibCompletionProcessor implements IContentAssistProcessor {

    private BibTexTemplateCompletion templatesCompletion = new BibTexTemplateCompletion();

    private BibDocumentModel model;
    private AbbrevManager abbrManager;
    
    private ProposalsComparator proposalsComparator = new ProposalsComparator();
    
    /**
     * Constructs a new completion processor
     * 
     * @param model The document model this processor is associated with.
     */
    public BibCompletionProcessor(BibDocumentModel model) {
        this.model = model;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        if (abbrManager == null)
            this.abbrManager = this.model.getAbbrManager();

        String completeDoc = viewer.getDocument().get();

        //get available and matching template completitions
        String latest=resolveLatestWord(completeDoc, offset);
        ICompletionProposal[] templates = computeTemplateCompletions(offset, latest.length(), latest, viewer);

        //get available and matching abbrev completitions
        ICompletionProposal[] abbrevs = null;        
        int completeStart = -1;
        for (int i = offset - 1; i > 0; i--) {
            char c = completeDoc.charAt(i);
            if (c == '=' || c == '#') {
                if (completeStart == -1)
                    completeStart = i + 1;
                break;
            } else if (Character.isWhitespace(c) && completeStart == -1)
                completeStart = i + 1;
            else if (c == '{' || c == '}' || c == '"' || c == ',')
                abbrevs = null;
        }
        if (completeStart == -1) {
            abbrevs = computeAbbrevCompletions(offset, 0, "");
        } else {
            abbrevs = computeAbbrevCompletions(offset, offset - completeStart, completeDoc.substring(completeStart, offset));
        }
        
        //make combined list of abbrev and template completition proposals
        int size = 0;
        if (abbrevs != null)
            size += abbrevs.length;
        if (templates != null)
            size += templates.length;

        if (size == 0)
            return null;
        else {
            int index=0;
            ICompletionProposal[] value = new ICompletionProposal[size];
            if (abbrevs != null){
        	  for (int i=0; i < abbrevs.length; i++) {
        	  	value[index] = abbrevs[i];
        	  	index++;
        	  }
            }
            if (templates != null){
          	  for (int i=0; i < templates.length; i++) {
          	  	value[index] = templates[i];
          	  	index++;
          	  }
            }
            return value;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
     */
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] {'=', '#'};
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    public String getErrorMessage() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    public IContextInformationValidator getContextInformationValidator() {
        return null;
    }

    /**
     * Computes the abbreviation completions available based on the prefix.
     * 
     * @param offset Cursor offset in the document
     * @param replacementLength Length of the text to be replaced
     * @param prefix The start of the abbreviation or ""
     * @return An array containing all possible completions
     */
    private ICompletionProposal[] computeAbbrevCompletions(int offset, int replacementLength, String prefix) {
        ReferenceEntry[] abbrevs = abbrManager.getCompletions(prefix);
        if (abbrevs == null)
            return null;

        ICompletionProposal[] result = new ICompletionProposal[abbrevs.length];
        
        for (int i = 0; i < abbrevs.length; i++) {         
            result[i] = new CompletionProposal(abbrevs[i].key,
                    offset - replacementLength, replacementLength,
                    abbrevs[i].key.length(), null, abbrevs[i].key, null,
                    abbrevs[i].info);
        }
        return result;
    }
   
    /**
     * Resolves the latest word immediately before the cursor position and returns it
     * 
     * @param doc Document to examine
     * @param offset Current cursor offset
     * @return the offset index,from where the latest word begins
     *   (if 0, then there is no latest word)  
     */
    public String resolveLatestWord(String doc, int offset) {
        int index = 1;
        while (offset - index >= 0) {
            if (Character.isWhitespace(doc.charAt(offset - index)))
                break;
            index++;
        }
        index--;
        if (index > 0)
            return doc.substring(offset-index, offset);
        return "";
    }

    /**
     * Computes the template completions available based on the prefix.
     * 
     * @param offset Cursor offset in the document
     * @param replacementLength Length of the text to be replaced
     * @param prefix The start of the abbreviation or ""
     * @param viewer The viewer associated with this document 
     * @return An array containing all possible completions
     */
    private ICompletionProposal[] computeTemplateCompletions(int offset, int replacementLength, String prefix, ITextViewer viewer) {
        List templateProposals = new ArrayList();
        this.templatesCompletion.addTemplateProposals(viewer, offset, templateProposals);
        ArrayList returnProposals = new ArrayList();
        
        for (Iterator iter = templateProposals.iterator(); iter.hasNext();) {
            ICompletionProposal proposal = (ICompletionProposal) iter.next();
            if (proposal.getDisplayString().startsWith(prefix)) {
                returnProposals.add(proposal);
            }
        }

        ICompletionProposal[] proposals = new ICompletionProposal[returnProposals.size()];
        returnProposals.toArray(proposals);
        
        Arrays.sort(proposals, proposalsComparator);
        return proposals;        
    }
}
