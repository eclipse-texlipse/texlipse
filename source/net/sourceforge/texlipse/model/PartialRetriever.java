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

/**
 * This class provides methods for retrieving partial matches from arrays.
 * Sorted arrays containing <code>AbstractEntry</code> -objects can be searched
 * for entries whose <code>key</code> starts with the given string.
 * 
 * This class provides both a linear search algorithm (for a reference to test new
 * algorithms with) as well as a binary search algorithm.
 * 
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public abstract class PartialRetriever {

    /**
     * Search the given (sorted) array of entries for all entries,
     * for which the start of the key matches the given search string.
     * 
     * This version uses a trivial linear search, which performs in O(n).
     * 
     * @param start The start of the searchable string
     * @param entries The entries to search
     * @return A two-element array with the lower (inclusive) and upper
     * (exclusive) bounds or {-1,-1} if no matching entries were found.
     */
    protected int[] getCompletionsLin(String start, AbstractEntry[] entries) {
        int startIdx = -1, endIdx = -1;
        for (int i=0; i < entries.length; i++) {
            if (entries[i].key.startsWith(start)) {
                if (startIdx == -1)
                    startIdx = i;
            } else if (startIdx != -1) {
                endIdx = i;
                break;
            }
        }
        if (startIdx != -1 && endIdx == -1)
            endIdx = startIdx + 1;
        return new int[] {startIdx, endIdx};
    }

    /**
     * Returns (if exist) the position of the entry with the given name in 
     * the array 
     * @param entryname Name of the wanted entry
     * @param entries	Sorted array of AbstractEntry
     * @return The position inside the array or -1 if the entry was not found
     */
    protected int getEntry(String entryname, AbstractEntry[] entries){
    	if (entries == null) return -1;
    	int start = 0;
    	int end = entries.length;
    	while (end-start>1 && !entries[(start+end)/2].key.equals(entryname)){
    		int c = entries[(start+end)/2].key.compareTo(entryname);
    		if (c < 0) start = (start+end)/2;
    		else end = (start+end)/2;
    	}
    	if (entries[(start+end)/2].key.equals(entryname)) return (start+end)/2;
    	else return -1;
    }
    
    /**
     * Search the given (sorted) array of entries for all entries,
     * for which the start of the key matches the given search string.
     * 
     * This version uses binary search for finding the lower and upper
     * bounds, resulting in O(log n) performance.
     * 
     * @param start The start of the searchable string
     * @param entries The entries to search
     * @return A two-element array with the lower (inclusive) and upper
     * (exclusive) bounds or {-1,-1} if no matching entries were found.
     */    
    protected int[] getCompletionsBin(String start, AbstractEntry[] entries) {
        return this.getCompletionsBin(start, entries, new int[] {0, entries.length});
    }

    /**
     * Search the given (sorted) array of entries for all entries,
     * for which the start of the key matches the given search string.
     * 
     * This version uses binary search for finding the lower and upper
     * bounds, resulting in O(log n) performance.
     * 
     * @param start The start of the searchable string
     * @param entries The entries to search
     * @param initBounds The initial lower and upper bounds to start the
     * search from
     * @return A two-element array with the lower (inclusive) and upper
     * (exclusive) bounds or {-1,-1} if no matching entries were found.
     */
    protected int[] getCompletionsBin(String start, AbstractEntry[] entries, int[] initBounds) {
        int[] bounds = new int[] {-1,-1};
        int left = initBounds[0], right = initBounds[1] - 1;
        int middle = right/2;
        
        if (entries[left].key.startsWith(start))
            right = middle = left;

        // get upper bound (inclusive)
        while (left < middle) {
            if (entries[middle].key.compareTo(start) >= 0) {
                right = middle;
                middle = (left + middle)/2;
            } else {
                left = middle;
                middle = (middle + right)/2;
            }
        }
        if (!entries[right].key.startsWith(start))
            return bounds;

        bounds[0] = right;
        
        // get lower bound (exclusive)
        left = right;
        right = initBounds[1] - 1;
        
        if (entries[right].key.startsWith(start)) {
            bounds[1] = right + 1;
            return bounds;
        }
        middle = (left + right)/2;
        while (left < middle) {
            if (entries[middle].key.startsWith(start)) {
                left = middle;
                middle = (right + middle)/2;
            } else {
                right = middle;
                middle = (middle + left)/2;
            }
        }
        bounds[1] = right;
        return bounds;
    }    
}
