/*******************************************************************************
 * Copyright (c) 2017 the TeXlipse team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/
/*
 * Created on Mar 25, 2005
 */
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
