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

package org.eclipse.texlipse.bibeditor;

import org.eclipse.texlipse.model.PartialRetriever;
import org.eclipse.texlipse.model.ReferenceEntry;

/**
 * Manages the abbreviations in a BibTeX file, used to
 * do content assist on abbreviation names.
 * 
 * @author Oskar Ojala
 */
public class AbbrevManager extends PartialRetriever {
    
    private ReferenceEntry[] abbrevs;
    
    /**
     * @param abbrevs The abbrevs to set.
     */
    public void setAbbrevs(ReferenceEntry[] abbrevs) {
        this.abbrevs = abbrevs;
    }
    
    /**
     * Returns all abbrev references matching the given string
     * 
     * @param start The beginning of an abbrev
     * 
     * @return References entries of abbrev, whose beginning
     *   matched the start
     */
    public ReferenceEntry[] getCompletions(String start) {   
        if ((abbrevs == null)||(abbrevs.length==0))
            return null;
        if (start.equals(""))
            return abbrevs;
        
        int[] bounds = this.getCompletionsBin(start, abbrevs);
        if (bounds[0] == -1)
            return null;
        
        ReferenceEntry[] compls = new ReferenceEntry[bounds[1] - bounds[0]];
        System.arraycopy(abbrevs, bounds[0], compls, 0, bounds[1] - bounds[0]);
        return compls;
    }
}
