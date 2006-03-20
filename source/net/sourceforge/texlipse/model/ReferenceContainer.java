/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Container for referencing data (BibTeX and labels.) Holds the reference
 * lists of each file as well as a sorted array of all references, so that
 * not all files need to be reparsed when the data changes.
 * 
 * @author Oskar Ojala
 */
public class ReferenceContainer {

    private HashMap referenceHash;
    private ReferenceEntry[] sortedReferences;
    private int size;
    
    /**
     * Creates a new reference container and initializes its datastructures.
     */
    public ReferenceContainer() {
        referenceHash = new HashMap(4);
        sortedReferences = null;
        size = 0;
    }
    
    /**
     * Adds a reference source <code>refs</code> 
     * associated with <code>key</code> into this object. Does not
     * update the sorted array of references.
     * 
     * @param key The key associated with these references in the internal set
     * @param refs The references to store
     */
    public void addRefSource(String key, ArrayList refs) {
        //ArrayList<ReferenceEntry> refs
        // Add filename to all references
        for (Iterator iter = refs.iterator(); iter.hasNext();) {
            ReferenceEntry r = (ReferenceEntry) iter.next();
            r.fileName = key;
        }
        
        size += refs.size();
        ArrayList al = (ArrayList) referenceHash.put(key, refs);
        if (al != null)
            size -= al.size();
    }
    
    /**
     * Updates the contents of this object if the given key exists
     * in the internal set. Updating includes replacing the old 
     * data with the new and re-creating the sorted reference array.
     * 
     * If the key doesn't exist, then nothing is done. Thus, this
     * method is suitable for use by the BibTeX editor to update
     * the BibTeX references in case these are used by the LaTeX editor.
     * 
     * @param key The key with which to associate the reference source
     * @param refs The reference source
     * @return True if the internal set was changed
     */
    public boolean updateRefSource(String key, ArrayList refs) {
        if (referenceHash.containsKey(key)) {
            this.addRefSource(key, refs);
            this.organize();
            return true;
        }
        return false;
    }

    /**
     * Organizes the data from the reference hashes to the sorted array
     * <code>sortedReferences</code>.
     */
    public void organize() {
        if (referenceHash.size() == 0)
            return;
        ArrayList allRefs = new ArrayList(size);
        if (referenceHash.size() > 1) {
            for (Iterator iter = referenceHash.values().iterator(); iter.hasNext();) {
                ArrayList refList = (ArrayList) iter.next();
                allRefs.addAll(refList);
            }
        } else if (referenceHash.size() == 1) {
            Iterator iter = referenceHash.values().iterator();
            allRefs = (ArrayList) iter.next();
        }
        sortedReferences = new ReferenceEntry[allRefs.size()];
        allRefs.toArray(sortedReferences);
        Arrays.sort(sortedReferences);
    }
    
    /**
     * Compares the set contained to the given keys in order to determine
     * whether the new keyset has changed or not. If it has, the names
     * of the new keys are returned as a list. If the internal set contains
     * keys which aren't in the given keyset, then the these kesy and their
     * datastructures are removed from the internal set.
     * 
     * The naming of this method is due to the fact that it's really useful for
     * BibTeX files (ie. keys), but not for labels, as the BibTeX files are
     * all defined in one command and thus fetched simultaneously, whereas
     * labels are built incrementally.
     * 
     * @param newBibs An array containing the new keys to store into the set
     * @return An empty list if there were no new keys, otherwise a list containing
     * the names of the new keys
     */
    public LinkedList updateBibHash(String[] newBibs) {
        LinkedList toParse = new LinkedList();
        HashMap newHash = new HashMap(newBibs.length);
        int newSize = 0;
        
        for (int i=0; i < newBibs.length; i++) {
            ArrayList al = (ArrayList) referenceHash.get(newBibs[i]);
            if (al != null) {
                newHash.put(newBibs[i], al);
                newSize += al.size();
            } else {
                toParse.add(newBibs[i]);
            }
        }
        referenceHash = newHash;
        size = newSize;
        
        return toParse;
    }
    
    /**
     * Checks whether this container is fresh: it is up to date
     * if it contains exactly the same keys (or in practice BibTeX-files) 
     * as those given as arguments.
     * 
     * @param newBibs Array of the new keys
     * @return <code>true</code> if this container is up to date, false otherwise
     */
    public boolean checkFreshness(String[] newBibs) {
        if (newBibs.length != referenceHash.size())
            return false;
        
        for (int i=0; i < newBibs.length; i++)
            if (!referenceHash.containsKey(newBibs[i]))
                return false;
        
        return true;
    }
    
    /**
     * Tests (using binary search) if the given key exists in this container.
     * 
     * @param key The key to look for
     * @return True if <code>key</code> was found, false if it was not found
     */
    public boolean binTest(String key) {
        if (sortedReferences == null || sortedReferences.length == 0)
            return false;
        int left = 0;
        int right = sortedReferences.length - 1;
        int center = right >> 1;
        int comparison;
        
        while (right >= left) {
            comparison = key.compareTo(sortedReferences[center].key);
            if (comparison < 0) {
                right = center - 1;
            } else if (comparison > 0) {
                left = center + 1;
            } else { //if (val == buf[center])
                return true;
            }
            center = (left + right) >> 1;
        }
        return false;
    }
    
    /**
     * Takes a list of errors (<code>DocumentReference</code>) and
     * checks which of them are "false alarms" (ie. they exist in this container)
     * 
     * This is used because the reference declarations might not be available
     * when the reference occurs in the document, so after updating and
     * organizing this container, this method should be used to find out
     * whit references are actually resolved.
     * 
     * @param errors A list of errors in the form of <code>DocumentReference</code>
     */
    public void removeFalseEntries(ArrayList errors) {
        for (Iterator iter = errors.iterator(); iter.hasNext();) {
            DocumentReference docRef = (DocumentReference) iter.next();
            if (binTest(docRef.getKey())) {
                iter.remove();
            }
        }
    }
    
    /**
     * Returns all the references in this container alphabetically sorted.
     * 
     * @return Returns the sortedReferences.
     */
    public ReferenceEntry[] getSortedReferences() {
        return sortedReferences;
    }
}
