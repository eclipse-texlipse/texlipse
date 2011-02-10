/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.ReferenceManager;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.model.TexDocumentModel;
import net.sourceforge.texlipse.templates.TexContextType;
import net.sourceforge.texlipse.templates.TexTemplateCompletion;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * ContentAssistProcessor for math commands.
 * 
 * @author Boris von Loesch
 * @author Oskar Ojala
 */
public class TexMathCompletionProcessor implements IContentAssistProcessor {
    
    private TexTemplateCompletion templatesCompletion = new TexTemplateCompletion(TexContextType.MATH_CONTEXT_TYPE);
    private TexDocumentModel model;
    private ISourceViewer fviewer;
    
    /**
     * Receives the document model from the editor (one model/editor view)
     * and creates a new completion processor.
     *  
     * @param tdm The document model for this editor
     * @param viewer The ISourceviewer for this editor
     */
    public TexMathCompletionProcessor(TexDocumentModel tdm, ISourceViewer viewer) {
        this.model = tdm;
        fviewer = viewer;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        model.removeStatusLineErrorMessage();
        String lineStart = "";
        
        IDocument doc = viewer.getDocument();
        try {
            int lineStartOffset = doc.getLineOffset(doc.getLineOfOffset(offset));
            lineStart = doc.get(lineStartOffset, offset - lineStartOffset);
        } catch (BadLocationException e) {
            TexlipsePlugin.log("TexCompletionProcessor: ", e);
            return new ICompletionProposal[0];
        }
        
        if (lineStart.endsWith("\\\\"))
            return null;
                
        int backpos = lineStart.lastIndexOf('\\');
        int templatepos = Math.max(Math.max(lineStart.lastIndexOf(' '),
                lineStart.lastIndexOf('$')), lineStart.lastIndexOf('}')) + 1;

        String replacement = lineStart.substring(templatepos);

        ICompletionProposal[] templateProposals = computeTemplateCompletions(offset, replacement.length(), replacement, viewer);
        
        ICompletionProposal[] proposals = null;
        if (backpos >= 0) {
            String command = lineStart.substring(backpos + 1);
            if (!(command.indexOf(' ') >= 0
                    || command.indexOf('{') >= 0
                    || command.indexOf('(') >= 0)) { 
                
                CompletionProposal cp = null;
                if ("\\".equals(command) || "end".startsWith(command)) {
                    String endString = TexCompletionProcessor.environmentEnd(doc.get(), offset);
                    if (endString != null) {
                        cp = new CompletionProposal("\\"+endString, 
                                offset - replacement.length(), 
                                replacement.length(), endString.length()+1);
                    }
                }


                ReferenceManager refManager = model.getRefMana();
                List<TexCommandEntry> comEntries = refManager.getCompletionsCom(command, TexCommandEntry.MATH_CONTEXT);
                if (comEntries != null){
                    int start = 0;
                    if (cp == null) {
                        proposals = new ICompletionProposal[comEntries.size()];
                    } else {
                        proposals = new ICompletionProposal[comEntries.size()+1];
                        proposals[0] = cp;
                        start = 1;
                    }
                    int len = command.length();
                    for (int i=0; i < comEntries.size(); i++) {
                        proposals[start + i] = new TexCompletionProposal(comEntries.get(i), offset - len, 
                                len, fviewer);
                    }
                } else {
                    if (cp != null) {
                        proposals = new ICompletionProposal[1];
                        proposals[0] = cp;
                    }
                }
            }
        }
        
        // Concatenate the lists if necessary
        if ((proposals != null) && (templateProposals != null)) {
            ICompletionProposal[] value = new ICompletionProposal[proposals.length
                                                                  + templateProposals.length];
            System.arraycopy(proposals, 0, value, 0, proposals.length);
            System.arraycopy(templateProposals, 0, value, proposals.length, templateProposals.length);
            return value;
        } else {
            if (templateProposals.length == 0) {
                model.setStatusLineErrorMessage(" No completions available.");
            }
            return templateProposals;
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
        return new char[] {'\\'};
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
    
//  copied from TexCompletion Processor    
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
        ArrayList returnProposals = templatesCompletion.addTemplateProposals(viewer, offset, prefix);
        ICompletionProposal[] proposals = new ICompletionProposal[returnProposals.size()];
        
        // and fill with list elements
        returnProposals.toArray(proposals);
        
        return proposals;        
    }
    
    
}
