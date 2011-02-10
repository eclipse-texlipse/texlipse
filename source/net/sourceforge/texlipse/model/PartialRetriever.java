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

import java.util.Arrays;
import java.util.List;

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
     * Returns (if exist) the position of the entry with the given name in 
     * the array 
     * @param entryname Name of the wanted entry
     * @param entries   Sorted List of AbstractEntry
     * @param lowerCase If true, entries must be sorted case insensitive
     * @return The position inside the list or -1 if the entry was not found
     */
    public static int getEntry(String entryname, List<? extends AbstractEntry> entries, boolean lowerCase){
        if (entries == null || entries.size() == 0) return -1;
        String lEntryname = entryname.toLowerCase();
        
        int start = 0;
        int end = entries.size();
        while (end - start > 1 && !entries.get((start + end)/2).getkey(lowerCase).equals(lEntryname)){
            int c = entries.get((start + end)/2).getkey(lowerCase).compareTo(lEntryname);
            if (c < 0) start = (start + end)/2;
            else end = (start + end)/2;
        }
        
        if (lowerCase) {
            //This case is a bit more complicated since there could be different entries with the
            //same lower case letters
            int m = (start + end)/2;
            if (!entries.get(m).getkey(lowerCase).equals(lEntryname)) return -1;
            
            m--;
            while (m >= 0 && entries.get(m).getkey(lowerCase).equals(lEntryname)) m--;
            m++;
            
            while (m < entries.size() && entries.get(m).getkey(lowerCase).equals(lEntryname)) {
                if (entries.get(m).key.equals(entryname)) return m;
                m++;
            }
            return -1;
        }
        else {
            if (entries.get((start + end)/2).getkey(lowerCase).equals(lEntryname)) return (start + end)/2;
            else return -1;
        }
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
     * @param lowerCase If true, entries must be sorted case insensitive
     * @return A two-element array with the lower (inclusive) and upper
     * (exclusive) bounds or {-1,-1} if no matching entries were found.
     */    
    protected int[] getCompletionsBin(String start, List<? extends AbstractEntry> entries, boolean lowerCase) {
        return this.getCompletionsBin(start, entries, new int[] {0, entries.size()}, lowerCase);
    }

    protected int[] getCompletionsBin(String start, AbstractEntry[] entries) {
        return this.getCompletionsBin(start, Arrays.asList(entries), new int[] {0, entries.length}, false);
    }

    /**
     * Search the given (sorted) list of entries for all entries,
     * for which the start of the key matches the given search string.
     * 
     * This version uses binary search for finding the lower and upper
     * bounds, resulting in O(log n) performance.
     * 
     * @param start The start of the searchable string
     * @param entries The entries to search
     * @param initBounds The initial lower and upper bounds to start the
     * search from
     * @param lowerCase If true, assumes that the list is sorted lower case and 
     * @return A two-element array with the lower (inclusive) and upper
     * (exclusive) bounds or {-1,-1} if no matching entries were found.
     */
    protected int[] getCompletionsBin(String start, List<? extends AbstractEntry> entries, 
            int[] initBounds, boolean lowerCase) {
        int[] bounds = new int[] {-1,-1};
        int left = initBounds[0], right = initBounds[1] - 1;
        int middle = right/2;
        if (left > right) return bounds;
        if (lowerCase) start = start.toLowerCase();
        
        if (entries.get(left).getkey(lowerCase).startsWith(start))
            right = middle = left;

        // get upper bound (inclusive)
        while (left < middle) {
            if (entries.get(middle).getkey(lowerCase).compareTo(start) >= 0) {
                right = middle;
                middle = (left + middle)/2;
            } else {
                left = middle;
                middle = (middle + right)/2;
            }
        }
        if (!entries.get(right).getkey(lowerCase).startsWith(start))
            return bounds;

        bounds[0] = right;
        
        // get lower bound (exclusive)
        left = right;
        right = initBounds[1] - 1;
        
        if (entries.get(right).getkey(lowerCase).startsWith(start)) {
            bounds[1] = right + 1;
            return bounds;
        }
        middle = (left + right)/2;
        while (left < middle) {
            if (entries.get(middle).getkey(lowerCase).startsWith(start)) {
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
