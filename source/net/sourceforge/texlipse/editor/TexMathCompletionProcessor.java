package net.sourceforge.texlipse.editor;

import java.util.ArrayList;

import net.sourceforge.texlipse.model.ReferenceManager;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.model.TexDocumentModel;
import net.sourceforge.texlipse.templates.TexContextType;
import net.sourceforge.texlipse.templates.TexTemplateCompletion;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
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
    private ReferenceManager refMana;
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
    
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
            int offset) {
        String line = "";
        try {
            int currLine = viewer.getDocument().getLineOfOffset(offset);
            line = viewer.getDocument().get(viewer.getDocument().getLineOffset(currLine), 
                    offset - viewer.getDocument().getLineOffset(currLine));
        } catch (BadLocationException e) {
            // FIXME
            e.printStackTrace();
            return null;
        }
        //Check for new line
        if (line.endsWith("\\\\")) return null;
        
//      TexCommandEntry[] comEntries = null;
        ICompletionProposal[] templateProposals = null;
        ICompletionProposal[] proposals = null;
        
        int backpos = line.lastIndexOf('\\');
        int templatepos = line.lastIndexOf(' ') < backpos ? backpos : line.lastIndexOf(' ') + 1;
        String replacement = templatepos < 0 ? line : line.substring(templatepos);
        
        //if (backpos == -1) return null;
        if (backpos > 0) {
            String command = line.substring(backpos);
            if (!(command.indexOf(' ') >= 0 || command.indexOf('{') >= 0 || command.indexOf('(') >= 0)) { 
                
                if (refMana == null) this.refMana = model.getRefMana();
                
                TexCommandEntry[] comEntries = refMana.getCompletionsCom(command.substring(1), TexCommandEntry.MATH_CONTEXT);
                
                int len = command.length() - 1;
                proposals = new ICompletionProposal[comEntries.length];
                for (int i=0; i < comEntries.length; i++) {
                    proposals[i] = new TexCompletionProposal(comEntries[i], offset - len, 
                            len, fviewer);
                }
            }
        }
        templateProposals = computeTemplateCompletions(offset, replacement.length(), replacement, viewer);
        
        //if (comEntries == null) return null;
        
        
        
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
                model.setStatusLineErrorMessage("Math: No completions available.");
                return new ICompletionProposal[0];
            }
        }
        
        
        //return result;
    }
    
    public IContextInformation[] computeContextInformation(ITextViewer viewer,
            int offset) {
        return null;
    }
    
    public char[] getCompletionProposalAutoActivationCharacters() {
        return new char[] {'\\'};
    }
    
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }
    
    public String getErrorMessage() {
        return null;
    }
    
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
