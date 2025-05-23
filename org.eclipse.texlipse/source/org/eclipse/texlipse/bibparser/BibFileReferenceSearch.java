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

package org.eclipse.texlipse.bibparser;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

import org.eclipse.core.resources.IProject;
import org.eclipse.texlipse.model.ReferenceEntry;
import org.eclipse.texlipse.properties.TexlipseProperties;

/**
 * Searches the bibref-directory for files that have the same names
 * as the keys from the given BibTeX-entries. If found, it will store
 * the file objects to the corresponding BibTeX-entries 
 * (<code>ReferenceEntry</code>s)
 */
public class BibFileReferenceSearch implements Runnable {
    
    //private Hashtable<String, BibStringTriMap<ReferenceEntry>> sortIndex;
    private IProject project;
    private String directory;
    
    /*
    public BibFileReferenceSearch(Hashtable<String, BibStringTriMap<ReferenceEntry>> sortIndex,
            IProject project) {
        this.sortIndex = sortIndex;
        this.project = project;
    }*/
    
    /** 
     * Search the directory listed in project properties for files matching
     * the bibtex reference key and add the file to the ReferenceEntry for
     * later viewing
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {
        /*
        if (sortIndex == null)
            return;
        ArrayList<ReferenceEntry> index = sortIndex.get("indexkey").getValues();
        if (index == null)
            return;
        
        directory = TexlipseProperties.getProjectProperty(project, TexlipseProperties.BIBREF_DIR_PROPERTY);
        Hashtable<String,File> fileNames = new Hashtable<String,File>();
        
        File[] files = new File(directory).listFiles();
        if (files == null)
            return;
        for (File f : files) {
            String s = f.getName().toLowerCase();
            if (s.indexOf(".") > -1)
                s = s.substring(0, s.indexOf(".")); // TODO basename
            fileNames.put(s, f);
        }		
        
        for (ReferenceEntry re : index) {
            String key = re.key.toLowerCase();
            File f = fileNames.get(key);
            if (f != null) 
                re.refFile = f;							
        }
        */	
    }
}
