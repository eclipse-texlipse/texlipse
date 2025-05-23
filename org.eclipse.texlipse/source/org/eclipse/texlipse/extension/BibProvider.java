/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.extension;

import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.texlipse.model.ReferenceContainer;

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
