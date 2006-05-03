/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.sourceforge.texlipse.model.ReferenceEntry;

/**
 * A container for BibTeX outlines. Can be sorted in different ways.
 *  
 * @author Oskar Ojala
 */
public class BibOutlineContainer {

    private String startName;
    private String endName;
    private String sorting; // TODO ENUMs here    
    private List childContainers; // BibOutlineContainer
    private List childEntries; // ReferenceEntry
    private boolean topLevel;
    
    // TODO enums
    public static final String SORTNATURAL = "natural";
    public static final String SORTYEAR = "year";
    public static final String SORTAUTHOR = "author";
    public static final String SORTJOURNAL = "journal";
    public static final String SORTINDEX = "index";
    
    private static int MAX_PARTITIONSIZE = 15;
    
    /**
     * Creates a new container
     * 
     * @param entries The initial entries
     * @param topLevel Whether or not this represents the top level of the hierarchy
     */
    public BibOutlineContainer(List entries, boolean topLevel) {
        this.childEntries = entries;
        this.topLevel = topLevel;
        this.sorting = SORTNATURAL;
    }

    /**
     * Creates a new container
     * 
     * @param entries The initial entries
     * @param sName Name of the first entry
     * @param eName Name of the last entry
     */
    private BibOutlineContainer(List entries, String sName, String eName) {
        this.childEntries = entries;
        this.startName = sName;
        this.endName = eName;
        this.topLevel = false;
    }

    /**
     * Makes a copy of this container so that first level children are copied
     * as new objects but lower levels are not
     * 
     * @param sorting The type of sorting that the new container will have
     * @return A copy of this container
     */
    private BibOutlineContainer childCopy(String sorting) {
        BibOutlineContainer newboc = new BibOutlineContainer(new ArrayList(), topLevel);
        newboc.sorting = sorting;
        
        for (Iterator iter = childEntries.iterator(); iter.hasNext();) {
            ReferenceEntry re = (ReferenceEntry) iter.next();
            newboc.childEntries.add(re.copy());
        }
        return newboc;
    }

    
    /**
     * Builds a container sorted by authors
     * 
     * @return New container sorted by authors
     */
    public BibOutlineContainer buildAuthorSort() {
        // Make a shallow copy
        BibOutlineContainer newboc = childCopy(SORTAUTHOR);
        newboc.authorSort();
        
        // Replace the keys with the names of the authors
        for (Iterator iter = newboc.childEntries.iterator(); iter.hasNext();) {
            ReferenceEntry re = (ReferenceEntry) iter.next();
            re.key = re.author + "; " + re.key;
        }
        // Build a tree structure
        newboc.partition();
        return newboc;
    }

    //private static final Pattern rmBraces = Pattern.compile("(^|[^\\\\])(:?\\{|\\})");
    
    /**
     * Does an author sort on this container
     */
    private void authorSort() {
        // duplicate entries with several authors
        ArrayList copyChildren = new ArrayList();
        for (Iterator iter = childEntries.iterator(); iter.hasNext();) {
            ReferenceEntry re = (ReferenceEntry) iter.next();
            re.author = re.author.replaceAll("\\\\(.)", "$1");

            String[] authors = re.author.split(" and ");
            
            // TODO fails e.g. on "Foo {Bar (Tutor)}", need to check braces
            // formats the author so that the last name is first
            for (int i = 0; i < authors.length; i++) {
                if (authors[i].indexOf(',') == -1
                        && !(authors[i].startsWith("{") && authors[i].endsWith("}"))) {
                    int pos = authors[i].lastIndexOf(' ');
                    if (pos != -1) {
                        authors[i] = authors[i].substring(pos+1) + ", " + authors[i].substring(0, pos); 
                    }
                }
                // Remove braces
                authors[i] = authors[i].replaceAll("(^|[^\\\\])(:?\\{|\\})", "$1");
            }

            // We make some simple copies of this entry and because they will
            // only be in the outline, we won't copy every value
            re.author = authors[0];
            for (int i = 1; i < authors.length; i++) {
                ReferenceEntry copyRe = new ReferenceEntry(re.key);
                copyRe.position = re.position;
                copyRe.refFile = re.refFile;
                copyRe.author = authors[i];
                copyChildren.add(copyRe);
            }
        }
        // merge the lists
        childEntries.addAll(copyChildren);
        // sort
//        Collections.sort(this.childEntries, new Comparator<ReferenceEntry>() {
//            public int compare(ReferenceEntry A, ReferenceEntry B) {
//                return A.author.compareTo(B.author);
//            }
//        });
        Collections.sort(this.childEntries, new Comparator() {
            public int compare(Object A, Object B) {
                return ((ReferenceEntry) A).author.compareTo(((ReferenceEntry) B).author);
            }
        });
    }

    /**
     * Builds a container sorted by year
     * 
     * @return New container sorted by year
     */
    public BibOutlineContainer buildYearSort() {
        BibOutlineContainer newboc = childCopy(SORTYEAR);
//        Collections.sort(newboc.childEntries, new Comparator<ReferenceEntry>() {
//            public int compare(ReferenceEntry A, ReferenceEntry B) {
//                return A.year.compareTo(B.year);
//            }
//        });
        Collections.sort(newboc.childEntries, new Comparator() {
            public int compare(Object A, Object B) {
                return ((ReferenceEntry) A).year.compareTo(((ReferenceEntry) B).year);
            }
        });

        for (Iterator iter = newboc.childEntries.iterator(); iter.hasNext();) {
            ReferenceEntry re = (ReferenceEntry) iter.next();
            re.key = re.year + "; " + re.key;
        }

        newboc.partition();
        return newboc;
    }

    /**
     * Builds a container sorted by journal
     * 
     * @return New container sorted by journal
     */
    public BibOutlineContainer buildJournalSort() {
        BibOutlineContainer newboc = childCopy(SORTJOURNAL);
//        Collections.sort(newboc.childEntries, new Comparator<ReferenceEntry>() {
//            public int compare(ReferenceEntry A, ReferenceEntry B) {
//                return A.journal.compareTo(B.journal);
//            }
//        });
        Collections.sort(newboc.childEntries, new Comparator() {
            public int compare(Object A, Object B) {
                return ((ReferenceEntry) A).journal.compareTo(((ReferenceEntry) B).journal);
            }
        });
        for (Iterator iter = newboc.childEntries.iterator(); iter.hasNext();) {
            ReferenceEntry re = (ReferenceEntry) iter.next();
            re.key = re.journal + "; " + re.key;
        }
        newboc.partition();
        return newboc;
    }

    /**
     * Builds a container sorted by index
     * 
     * @return New container sorted by index
     */
    public BibOutlineContainer buildIndexSort() {
        BibOutlineContainer newboc = childCopy(SORTINDEX);
//        Collections.sort(newboc.childEntries, new Comparator<ReferenceEntry>() {
//            public int compare(ReferenceEntry A, ReferenceEntry B) {
//                return A.key.compareTo(B.key);
//            }
//        });
        Collections.sort(newboc.childEntries, new Comparator() {
            public int compare(Object A, Object B) {
                return ((ReferenceEntry) A).key.compareTo(((ReferenceEntry) B).key);
            }
        });
        newboc.partition();
        return newboc;
    }


    /**
     * Calculate the shortest differentiating prefix between
     * the strings that is at least 4 characters.
     * 
     * @param s1 First string
     * @param s2 Second string
     * @return Differentiating prefix
     */
    private String differentiatingPrefix(String s1, String s2) {
        int i = 0;
        int shorter = Math.min(s1.length(), s2.length());
        for (; i < shorter; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                int l = Math.max(4, i+1);
                return s1.substring(0, l);
            }
        }
        // the other one was shorter
        if (s1.length() == i) {
            return s1;
        } else {
            return s1.substring(0, i + 1);
        }
    }


    /**
     * Partitions this container
     */
    public void partition() {
        if (childEntries.size() < MAX_PARTITIONSIZE) {
            return;
        }
        //childContainers = new ArrayList();

        // TODO polish
        
        // calculate hierarchy levels and partitions
        int totalPartitions = (int) Math.ceil((double) childEntries.size() / (double) MAX_PARTITIONSIZE);

        ArrayList bottomContainers = new ArrayList();
        ReferenceEntry[] childArray = new ReferenceEntry[childEntries.size()];
        childEntries.toArray(childArray);
        
        // total levels
//        int levels = 1;
//        for (int entries = MAX_PARTITIONSIZE; entries < totalPartitions; entries *= MAX_PARTITIONSIZE) {
//            levels++;
//        }
        
        
        String prevName = ((ReferenceEntry) childEntries.get(0)).key;
        String nextName = "...";
        for (int i = 0; i < totalPartitions; i++) {
            //int partitionEnd = Math.min(childEntries.size(), MAX_PARTITIONSIZE);
            int partitionSize = Math.min(childEntries.size() - MAX_PARTITIONSIZE * i,
                    MAX_PARTITIONSIZE);

            ReferenceEntry[] newChildren = new ReferenceEntry[partitionSize];
            System.arraycopy(childArray, MAX_PARTITIONSIZE * i, newChildren, 0, partitionSize);
            
//            String pre1 = differentiatingPrefix(((ReferenceEntry) children.get(0)).key, prevName);
//            prevName = ((ReferenceEntry) children.get(children.size()-1)).key;
//            nextName = childEntries.size() > 0 ? ((ReferenceEntry) childEntries.get(0)).key : "...";
//            String pre2 = differentiatingPrefix(prevName, nextName);

            String pre1 = differentiatingPrefix(newChildren[0].key, prevName);
            prevName = newChildren[newChildren.length-1].key;
            nextName = childEntries.size() > 0 ? childArray[0].key : "...";
            String pre2 = differentiatingPrefix(prevName, nextName);

            //BibOutlineContainer boc = new BibOutlineContainer(children, pre1 + "..." + pre2);
            BibOutlineContainer boc = new BibOutlineContainer(Arrays.asList(newChildren), pre1, pre2);
            bottomContainers.add(boc);
        }
        
        // subdivide
        while (bottomContainers.size() > MAX_PARTITIONSIZE) {
            ArrayList midContainers = new ArrayList();
            while (bottomContainers.size() > 0) {
                int partitionEnd = Math.min(bottomContainers.size(), MAX_PARTITIONSIZE);
                
                // probably doesn't work...
                //List children = childEntries.subList(0, partitionEnd);
                //childEntries.removeRange(0, partitionEnd);
                
                // if it doesn't work...
                ArrayList children = new ArrayList();
                ListIterator liter = bottomContainers.listIterator();
                for (int j = 0; j < partitionEnd; j++) {
                    children.add(liter.next());
                    liter.remove();
                }

                String sName = ((BibOutlineContainer) children.get(0)).startName;
                String eName = ((BibOutlineContainer) children.get(0)).endName;
                BibOutlineContainer boc = new BibOutlineContainer(children, sName, eName);
                midContainers.add(boc);
            }
            bottomContainers = midContainers;
        }
        
        childContainers = bottomContainers;
        childEntries = null;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return startName + "..." + endName;
    }
    
    /**
     * @return Returns the childContainers.
     */
    public List getChildContainers() {
        return childContainers;
    }

    /**
     * @return Returns the childEntries.
     */
    public List getChildEntries() {
        return childEntries;
    }

    /**
     * @return Returns the sorting.
     */
    public String getSorting() {
        return sorting;
    }

}
