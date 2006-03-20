package net.sourceforge.texlipse.editor;

import net.sourceforge.texlipse.model.ReferenceManager;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.model.TexDocumentModel;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * ContentAssistProcessor for math commands.
 * @author Boris von Loesch
 *
 */
public class TexMathCompletionProcessor implements IContentAssistProcessor {

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
			e.printStackTrace();
			return null;
		}
		//Check for new line
		if (line.endsWith("\\\\")) return null;
		
		int backpos = line.lastIndexOf('\\');
		if (backpos == -1) return null;
		String command = line.substring(backpos);
		if (command.indexOf(' ') >= 0 || command.indexOf('{') >= 0 || command.indexOf('(') >= 0) 
			return null;
		if (refMana == null) this.refMana = model.getRefMana();
		
    	TexCommandEntry[] comEntries = refMana.getCompletionsCom(command.substring(1), 
    			TexCommandEntry.MATH_CONTEXT);
        
    	if (comEntries == null) return null;

        int len = command.length() - 1;
        ICompletionProposal[] result = new ICompletionProposal[comEntries.length];
        for (int i=0; i < comEntries.length; i++) {
        	result[i] = new TexCompletionProposal(comEntries[i], offset - len, 
        			len, fviewer);
        }

        return result;
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

}
