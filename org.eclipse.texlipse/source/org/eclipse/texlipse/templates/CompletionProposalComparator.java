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

package org.eclipse.texlipse.templates;

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
