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
 * A comparator class for ICompletition proposals, so that
 * proposals can be sorted.
 */
public class ProposalsComparator implements Comparator {
    
    /**
     * Compares two ICompletionProposals according to their display Strings
     * 
     * @return same as String.compareToIgnoreCase()
     */
    public int compare(Object o1, Object o2) {
        ICompletionProposal p1 = (ICompletionProposal) o1;
        ICompletionProposal p2 = (ICompletionProposal) o2;
        
        return p1.getDisplayString().compareToIgnoreCase(p2.getDisplayString());
    }
}
