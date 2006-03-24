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

import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.model.ReferenceManager;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.model.TexDocumentModel;
import net.sourceforge.texlipse.spelling.SpellChecker;
import net.sourceforge.texlipse.templates.TexContextType;
import net.sourceforge.texlipse.templates.TexTemplateCompletion;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ContextInformationValidator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;


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

    private TexTemplateCompletion templatesCompletion = new TexTemplateCompletion(TexContextType.TEX_CONTEXT_TYPE);

    private TexDocumentModel model;
    private ReferenceManager refManager;
    private ISourceViewer fviewer;
    
    //private static final String bracePair = "{}";
    public static final int assistLineLength = 60;
    
    /**
     * A regexp pattern for resolving the command used for referencing (in the 1st group)
     */
    private static final Pattern comCapt = Pattern.compile("([a-z]+)\\s*(?:\\[.*?\\]\\s*)?");

    /**
     * Receives the document model from the editor (one model/editor view)
     * and creates a new completion processor.
     *  
     * @param tdm The document model for this editor
     */
    //public TexCompletionProcessor(TexDocumentModel tdm) {
    public TexCompletionProcessor(TexDocumentModel tdm, ISourceViewer viewer) {
        this.model = tdm;
        this.fviewer = viewer;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    	ICompletionProposal[] proposals = null;
    	ICompletionProposal[] templateProposals = null;

        if (refManager == null)
            this.refManager = this.model.getRefMana();
        
        String completeDoc = viewer.getDocument().get();

	IDocument doc = viewer.getDocument();
	Point selectedRange = viewer.getSelectedRange();
	if (selectedRange.y > 0) {
	    try {
	        // Retrieve selected text
	        String text = doc.get(selectedRange.x, selectedRange.y);

                // Compute completion proposals
                return computeStyleProposals(text, selectedRange);
           } catch (BadLocationException e) {
           }
	}


        if (offset >= 2) {
        	// don't offer completions if the last thing typed was \\
            if (completeDoc.substring(offset-2, offset).endsWith("\\\\"))
                return null;
        }

        int seqStartIdx = resolveCompletionStart(completeDoc, offset - 1);
        String seqStart;
        if (seqStartIdx == -1 && offset != 0)
            return null;
        else if (offset == 0)
        	seqStart = " ";
        else
        	seqStart = completeDoc.substring(seqStartIdx, offset);

        // Now resolve if we want to complete commands, references or templates
        if (seqStart.startsWith("\\")) {
            String replacement = seqStart.substring(1);
            proposals = computeCommandCompletions(offset, replacement.length(), replacement);
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
                proposals = computeBibCompletions(offset, replacement.length(), replacement);
            //} else if (refCommand.startsWith("ref") || refCommand.startsWith("pageref")) {
            } else if (refCommand.startsWith("ref") || refCommand.startsWith("pageref") || 
                    refCommand.startsWith("vref")) {
                proposals = computeRefCompletions(offset, replacement.length(), replacement);
            }
        } 
                
        if (Character.isWhitespace(seqStart.charAt(0))) {
            //---------------------spell-checking-code-starts----------------------
            // spell checking can't help with words not starting with a letter...
            ICompletionProposal[] prop = SpellChecker.getSpellingProposal(offset);
            if (prop != null && prop.length > 0) {
                return prop;
            }
            //---------------------spell-checking-code-ends------------------------
            
            String replacement = seqStart.substring(1);
            templateProposals = computeTemplateCompletions(offset, replacement.length(), replacement, viewer);
        } else {
            templateProposals = computeTemplateCompletions(offset, seqStart.length(), seqStart, viewer);
        }

        // Concatenate the lists if necessary
        if ((proposals != null) && (templateProposals != null)) {
            ICompletionProposal[] value = new ICompletionProposal[proposals.length
                    + templateProposals.length];
            
            System.arraycopy(proposals, 0, value, 0, proposals.length);
            System.arraycopy(templateProposals, 0, value, proposals.length, templateProposals.length);
            return value;
        } else {
            if (proposals != null) {
                return proposals;
            } else  if (templateProposals.length != 0) {
                return templateProposals;
            } else {
                // TODO consider this
                model.setStatusLineErrorMessage(" No completions available.");
                return new ICompletionProposal[0];
            }
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

	    // FIXME -- for testing
        // Retrieve selected range
        Point selectedRange = viewer.getSelectedRange();
        if (selectedRange.y > 0) {
            
            // Text is selected. Create a context information array.
            ContextInformation[] contextInfos = new ContextInformation[STYLELABELS.length];
            
            // Create one context information item for each style
            for (int i = 0; i < STYLELABELS.length; i++)
                contextInfos[i] = new ContextInformation(null, STYLELABELS[i]+" Style");
            return contextInfos;
        }
        return new ContextInformation[0];
        
        //return null;
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
        return new ContextInformationValidator(this);
	    //return null;
    }

    /**
     * Resolves the completion replacement string so that the activation
     * character is the first character following the returned offset.
     * 
     * @param doc The document
     * @param offset The offset from where to search backwards
     * @return The offset where the activation char is found or -1 if it was not found
     */
    private int resolveCompletionStart(String doc, int offset) {
        while (offset >= 0) {
            if (Character.isWhitespace(doc.charAt(offset))
                    || doc.charAt(offset) == '{' || doc.charAt(offset) == '\\')
                break;
            offset--;
        }
        return offset;
    }

//    private int resolveCompletionEnd(String doc, int offset) {
//        while (offset < doc.length()) {
//            if (!Character.isLetter(doc.charAt(offset)))
//                break;
//            offset++;
//        }
//        return offset;
//    }

    
    /**
     * Resolves the command used for referencing (ie. from '\foo{bar' it resolves bar)
     * 
     * @param doc The document
     * @param offset The offset from where to search backwards
     * @return The string containing the LaTeX command for this reference or
     * null if there isn't any
     */
    private String resolveRefCommand(String doc, int offset) {
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
        ReferenceEntry[] bibEntries = refManager.getCompletionsBib(prefix);
        if (bibEntries == null)
            return null;

        ICompletionProposal[] result = new ICompletionProposal[bibEntries.length];
        
        for (int i=0; i < bibEntries.length; i++) {
        	String infoText = bibEntries[i].info.length() > assistLineLength ?
        			wrapString(bibEntries[i].info, assistLineLength)
					: bibEntries[i].info;
            result[i] = new CompletionProposal(bibEntries[i].key,
                    offset - replacementLength, replacementLength,
                    bibEntries[i].key.length(), null, bibEntries[i].key, null,
                    infoText);
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
        ReferenceEntry[] refEntries = refManager.getCompletionsRef(prefix);
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
     * Computes and returns command-proposals
     * 
     * @param offset Current cursor offset
     * @param replacementLength The length of the string to be replaced
     * @param prefix The already typed prefix of the entry to assist with
     * @return An array of completion proposals to use directly or null
     */
    private ICompletionProposal[] computeCommandCompletions(int offset, int replacementLength, String prefix) {
        //CommandEntry[] comEntries = refManager.getCompletionsCom(prefix);
        TexCommandEntry[] comEntries = refManager.getCompletionsCom(prefix, TexCommandEntry.NORMAL_CONTEXT);
        if (comEntries == null)
            return null;

        ICompletionProposal[] result = new ICompletionProposal[comEntries.length];

//        String braces;
        for (int i=0; i < comEntries.length; i++) {
//        	String infoText = comEntries[i].info.length() > assistLineLength ?
//        			wrapString(comEntries[i].info, assistLineLength)
//					: comEntries[i].info;
//
//            if (comEntries[i].arguments == 0) {
//                result[i] = new CompletionProposal(comEntries[i].key,
//                        offset - replacementLength, replacementLength,
//                        comEntries[i].key.length(), null, comEntries[i].key, null,
//                        infoText);
//            } else {
//                braces = bracePair;
//                if (comEntries[i].arguments > 1) {
//                    // this is slightly inefficient, but it's not a big deal here
//                    for (int j=1; j < comEntries[i].arguments; j++)
//                        braces += bracePair;
//                }
//                result[i] = new CompletionProposal(comEntries[i].key + braces,
//                        offset - replacementLength, replacementLength,
//                        comEntries[i].key.length() + 1, null, comEntries[i].key, null,
//                        infoText);
//            }
            result[i] = new TexCompletionProposal(comEntries[i],
                    offset - replacementLength, 
                    replacementLength,
                    fviewer);
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
        ArrayList returnProposals = templatesCompletion.addTemplateProposals(viewer, offset, prefix);
        ICompletionProposal[] proposals = new ICompletionProposal[returnProposals.size()];
        
        // and fill with list elements
        returnProposals.toArray(proposals);
        
        return proposals;        
    }
    
    
    public static String wrapString(String input, int width) {
        StringBuffer sbout = new StringBuffer();

        // \n should suffice since we prettify in parsing...
        String[] paragraphs = input.split("\r\n|\n|\r");
        for (int i = 0; i < paragraphs.length; i++) {
        	// skip if short
        	if (paragraphs[i].length() < width) {
        		sbout.append(paragraphs[i]);
        		sbout.append("\n");
        		continue;
        	}
        	// imagine how much better this would be with functional programming...
            String[] words = paragraphs[i].split("\\s");
            int currLength = 0;
            for (int j = 0; j < words.length; j++) {
                if (words[j].length() + currLength <= width || currLength == 0) {
                	if (currLength > 0)
                		sbout.append(" ");
                    sbout.append(words[j]);
                    currLength += 1 + words[j].length(); 
                } else {
                    sbout.append("\n");
                    sbout.append(words[j]);
                    currLength = words[j].length();
                }
            }
            sbout.append("\n");
        }
        return sbout.toString();
    }


    // Some very quick style completions follow...
    // TODO improve this

    private final static String[] STYLETAGS = new String[] { 
        "\\bf", "\\it", "\\rm", "\\sf", "\\sc", "\\em", "\\huge", "\\Huge"
    };
    private final static String[] STYLELABELS = new String[] { 
        "bold", "italic", "roman", "sans serif", "small caps", "emphasize", "huge", "Huge"
    };

    private ICompletionProposal[] computeStyleProposals(String selectedText, Point selectedRange) {
        
        ICompletionProposal[] result = new ICompletionProposal[STYLETAGS.length];
        
        // Loop through all styles
        for (int i = 0; i < STYLETAGS.length; i++) {
            String tag = STYLETAGS[i];
            
            // Compute replacement text
            String replacement = "{" + tag + " " + selectedText + "}";
            
            // Derive cursor position
            int cursor = tag.length() + 2;
            
            // Compute a suitable context information
            IContextInformation contextInfo = 
                new ContextInformation(null, STYLELABELS[i]+" Style");
            
            // Construct proposal
            result[i] = new CompletionProposal(replacement, 
                    selectedRange.x, selectedRange.y,
                    cursor, null, STYLELABELS[i], 
                    contextInfo, replacement);
        }
        return result;
    }

}
