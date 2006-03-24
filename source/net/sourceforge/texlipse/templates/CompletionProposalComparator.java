/*
 * Created on Mar 25, 2005
 */
package net.sourceforge.texlipse.templates;

import java.util.Comparator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;

/**
 * @author Esa Seuranen
 *
 * Comparator for sorting completition proposals (ICompletitionProposal)
 */
public class CompletionProposalComparator implements Comparator {

	/**
	 * Compares two CompletionProposals accrding to their display strings
	 * 
	 * @param arg0 first ICompletionProposal
	 * @param arg1 second ICompletionProposal
	 * 
	 * @return the same as String.compareTo() does
	 */
	public int compare(Object arg0, Object arg1) {
		return((ICompletionProposal)arg0).getDisplayString().compareTo(((ICompletionProposal)arg1).getDisplayString());
	}

}
