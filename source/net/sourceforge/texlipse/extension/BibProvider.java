package net.sourceforge.texlipse.extension;

import java.util.List;

import net.sourceforge.texlipse.model.ReferenceContainer;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * Interface that allows to add any content to the \cite-autocomplete container
 *  
 * @author Manuel
 *
 */
public interface BibProvider {

	/**
	 * This method allows optional plugins to control the entries that show up in the \cite autocomplete pop-up
	 * 
	 * @param offset The position of the insertion
	 * @param replacementLength The length of the string to replace 
	 * @param prefix The prefix of the completion
	 * @param bibContainer The {@link ReferenceContainer} with all the currently available completions
	 * 
	 * @return The List with the favored CompletionProposals
	 */
	public List<ICompletionProposal> getCompletions(int offset, int replacementLength, String prefix, ReferenceContainer bibContainer);
	
}
