/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.texlipse.model.CommandEntry;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.model.ReferenceManager;
import net.sourceforge.texlipse.model.TexDocumentModel;
import net.sourceforge.texlipse.templates.TexTemplateCompletion;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;


/**
 * Implements a LaTeX-content assistant for displaying a list of completions
 * for \ref, \pageref and \cite. For BibTeX-completions (\cite), the details
 * of the selected entry are displayed. The actualy completions are fetched
 * from the ReferenceManager, owned by the TexDocumentModel of the current 
 * document.
 * 
 * @author Oskar Ojala
 */
public class TexCompletionProcessor implements IContentAssistProcessor {

    private TexTemplateCompletion templatesCompletion = new TexTemplateCompletion();

    private TexDocumentModel model;
    private ReferenceManager refMana;
    
    /**
     * A regexp pattern for resolving the command used for referencing (in the 1st group)
     */
    private static Pattern comCapt = Pattern.compile("([a-z]+)\\s*(?:\\[.*?\\]\\s*)?");

    /**
     * Receives the document model from the editor (one model/editor view)
     * and creates a new completion processor.
     *  
     * @param tdm The document model for this editor
     */
    public TexCompletionProcessor(TexDocumentModel tdm) {
        this.model = tdm;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    	ICompletionProposal[] proposals=null;
    	ICompletionProposal[] templateProposals=null;
    		
        if (refMana == null)
            this.refMana = this.model.getRefMana();
        
        String completeDoc = viewer.getDocument().get();
        if (offset >= 2) {
            if (completeDoc.substring(offset-2, offset).endsWith("\\\\"))
                return null;
        }

        int seqStartIdx = resolveCompletionEnd(completeDoc, offset - 1);
        if (seqStartIdx == -1)
            return null;
        String seqStart = completeDoc.substring(seqStartIdx, offset);

        // Now resolve if we want to complete commands, references or templates
        if (seqStart.startsWith("\\")) {
            String replacement = seqStart.substring(1);
//E            return computeComCompletions(offset, replacement.length(), replacement);
            proposals=computeComCompletions(offset, replacement.length(), replacement);
        } else if (seqStart.startsWith("{")) {
            String refCommand = resolveRefCommand(completeDoc, seqStartIdx);
            if (refCommand == null)
                return null;

            String replacement = "";
            if (seqStart.length() > 1 && !seqStart.endsWith(",")) {
                String[] completions = seqStart.substring(1).split(",");
                replacement = completions[completions.length - 1];
            }
            
            if (refCommand.indexOf("cite") > -1) {
//E                return computeBibCompletions(offset, replacement.length(), replacement);
                proposals=computeBibCompletions(offset, replacement.length(), replacement);
            } else if (refCommand.startsWith("ref") || refCommand.startsWith("pageref")) {
//E                return computeRefCompletions(offset, replacement.length(), replacement);
                proposals=computeRefCompletions(offset, replacement.length(), replacement);
            }
        } 
//E        else if (Character.isWhitespace(seqStart.charAt(0))) {
//E            String replacement = seqStart.substring(1);
//E            return computeTemplateCompletions(offset, replacement.length(), replacement, viewer);            
//E        }
        
        if (Character.isWhitespace(seqStart.charAt(0))) {
            String replacement = seqStart.substring(1);
            templateProposals=computeTemplateCompletions(offset, replacement.length(), replacement, viewer);            
        } else {
            templateProposals=computeTemplateCompletions(offset, seqStart.length(), seqStart, viewer);                    	
        }

        if((proposals!=null)&&(templateProposals!=null)){
        	ICompletionProposal[] value= new ICompletionProposal[proposals.length+templateProposals.length];
        	int i=0,j;
        	for(j=0;j<proposals.length;j++) {
        		value[i]=proposals[j];
        		i++;
        	}
        	for(j=0;j<templateProposals.length;j++) {
        		value[i]=templateProposals[j];
        		i++;
        	}
        	return value;
        } else {
        	if (proposals!=null) return proposals;
        	else return templateProposals;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    public IContextInformation[] computeContextInformation(ITextViewer viewer,
            int offset) {
        /*
        IContextInformation[] result= new IContextInformation[5];
        for (int i= 0; i < result.length; i++)
            result[i]= new ContextInformation(
                    "CompletionProcessor.ContextInfo.display.pattern",
            "CompletionProcessor.ContextInfo.value.pattern");
        return result;
        */
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    public char[] getCompletionProposalAutoActivationCharacters() {
        //return new char[] {'{', ',', '\\'};
        return new char[] {'{', '\\'};
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    public char[] getContextInformationAutoActivationCharacters() {
//        return new char[] {'#'};
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
     * Resolves the completion replacement string so that the activation
     * character is the first character following the returned offset.
     * 
     * @param doc The document
     * @param offset The offset from where to search backwards
     * @return The offset where the activation char is found or -1 if it was not found
     */
    public int resolveCompletionEnd(String doc, int offset) {
        while (offset >= 0) {
            if (Character.isWhitespace(doc.charAt(offset))
                    || doc.charAt(offset) == '{' || doc.charAt(offset) == '\\')
                break;
            offset--;
        }
        return offset;
    }

    /**
     * Resolves the command used for referencing (ie. from '\foo{bar' it resolves bar)
     * 
     * @param doc The document
     * @param offset The offset from where to search backwards
     * @return The string containing the LaTeX command for this reference or
     * null if there isn't any
     */
    public String resolveRefCommand(String doc, int offset) {
        int lastIndex = doc.lastIndexOf('\\', offset);
        if (lastIndex == -1)
            return null;
        String command = doc.substring(lastIndex + 1, offset);
        Matcher m = comCapt.matcher(command);
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }
    
    /**
     * Computes and returns BibTeX-proposals.
     * 
     * @param offset Current cursor offset
     * @param replacementLength The length of the string to be replaced
     * @param prefix The already typed prefix of the entry to assist with
     * @return An array of completion proposals to use directly or null
     */
    private ICompletionProposal[] computeBibCompletions(int offset, int replacementLength, String prefix) {
        ReferenceEntry[] bibEntries = refMana.getCompletionsBib(prefix);
        if (bibEntries == null)
            return null;

        ICompletionProposal[] result = new ICompletionProposal[bibEntries.length];
        
        for (int i=0; i < bibEntries.length; i++) {         
            result[i] = new CompletionProposal(bibEntries[i].key,
                    offset - replacementLength, replacementLength,
                    bibEntries[i].key.length(), null, bibEntries[i].key, null,
                    bibEntries[i].info);
        }
        return result;        
    }

    /**
     * Computes and returns reference-proposals (labels).
     * 
     * @param offset Current cursor offset
     * @param replacementLength The length of the string to be replaced
     * @param prefix The already typed prefix of the entry to assist with
     * @return An array of completion proposals to use directly or null
     */
    private ICompletionProposal[] computeRefCompletions(int offset, int replacementLength, String prefix) {
        ReferenceEntry[] refEntries = refMana.getCompletionsRef(prefix);
        if (refEntries == null)
            return null;

        ICompletionProposal[] result = new ICompletionProposal[refEntries.length];
        
        for (int i=0; i < refEntries.length; i++) {         
            result[i] = new CompletionProposal(refEntries[i].key,
                    offset - replacementLength, replacementLength,
                    refEntries[i].key.length(), null, refEntries[i].key, null, null);
        }
        return result;        
    }

    /**
     * Computes and returns commnad-proposals
     * 
     * @param offset Current cursor offset
     * @param replacementLength The length of the string to be replaced
     * @param prefix The already typed prefix of the entry to assist with
     * @return An array of completion proposals to use directly or null
     */
    private ICompletionProposal[] computeComCompletions(int offset, int replacementLength, String prefix) {
        CommandEntry[] comEntries = refMana.getCompletionsCom(prefix);
        if (comEntries == null)
            return null;

        ICompletionProposal[] result = new ICompletionProposal[comEntries.length];
        
        String bracePair = "{}";
        String braces = bracePair;
        for (int i=0; i < comEntries.length; i++) {
            if (comEntries[i].arguments == 0) {
            result[i] = new CompletionProposal(comEntries[i].key,
                    offset - replacementLength, replacementLength,
                    comEntries[i].key.length(), null, comEntries[i].key, null,
                    comEntries[i].info);
            } else {
                braces = bracePair;
                if (comEntries[i].arguments > 1) {
                    // this is slightly inefficient, but it's not a big deal here
                    for (int j=1; j < comEntries[i].arguments; j++)
                        braces += bracePair;
                }
                result[i] = new CompletionProposal(comEntries[i].key + braces,
                        offset - replacementLength, replacementLength,
                        comEntries[i].key.length() + 1, null, comEntries[i].key, null,
                        comEntries[i].info);
            }
        }
        return result;
    }

    
    /**
     * Calculates and returns the template completions proposals.
     * 
     * @param offset Current cursor offset
     * @param replacementLength The length of the string to be replaced
     * @param prefix The already typed prefix of the entry to assist with
     * @param viewer The text viewer of this document
     * @return An array of completion proposals to use directly or null
     */
    private ICompletionProposal[] computeTemplateCompletions(int offset, int replacementLength, String prefix, ITextViewer viewer) {
        ArrayList returnProposals = this.templatesCompletion.addTemplateProposals(viewer, offset, prefix);
        ICompletionProposal[] proposals = new ICompletionProposal[returnProposals.size()];
        
        // and fill with list elements
        returnProposals.toArray(proposals);
        
        return proposals;        
    }
}
