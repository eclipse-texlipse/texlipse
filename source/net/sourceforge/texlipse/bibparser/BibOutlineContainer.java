package net.sourceforge.texlipse.bibparser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.sourceforge.texlipse.model.ReferenceEntry;

/**
 * implements Cloneable 
 * @author Oskar Ojala
 */
public class BibOutlineContainer {

    private String name;
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
    
    private static int MAX_PARTITIONSIZE = 6;
    private static int MAX_PARTITIONNUMBER = 20;
    
    public BibOutlineContainer(List entries, boolean topLevel) {
        this.childEntries = entries;
        this.topLevel = topLevel;
        this.sorting = SORTNATURAL;
    }

    private BibOutlineContainer(List entries, String name) {
        this.childEntries = entries;
        this.name = name;
        this.topLevel = false;
    }

    private BibOutlineContainer childCopy(String sorting) {
        BibOutlineContainer newboc = new BibOutlineContainer(new ArrayList(), topLevel);
        newboc.sorting = sorting;
        
        for (Iterator iter = childEntries.iterator(); iter.hasNext();) {
            ReferenceEntry re = (ReferenceEntry) iter.next();
            newboc.childEntries.add(re);
        }
        return newboc;
    }


    public BibOutlineContainer buildYearSort() {
        // make a shallow copy
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

        //newboc.partition();
        return newboc;
    }

    public BibOutlineContainer buildAuthorSort() {
        // make a shallow copy
        BibOutlineContainer newboc = childCopy(SORTAUTHOR);
        newboc.authorSort();
        return newboc;
    }

    private void authorSort() {
        // duplicate entries with several authors
        ArrayList copyChildren = new ArrayList();
        for (Iterator iter = childEntries.iterator(); iter.hasNext();) {
            ReferenceEntry re = (ReferenceEntry) iter.next();
            if (re.author.indexOf(" and ") != -1) {
                String[] authors = re.author.split(" and ");
                // TODO we make some simple copies of this entry and
                // because they will only be in the outline, we won't
                // copy every value
                
                // FIXME we need something like this for the author formats...
//                if (authors[0].indexOf(',') == -1) {
//                    int pos = authors[0].lastIndexOf(' ');
//                    if (pos != -1) {
//                        re.author = authors[0].substring(pos+1) + ", " + authors[0].substring(0, pos); 
//                    }
//                }
                
                for (int i = 1; i < authors.length; i++) {
                    ReferenceEntry copyRe = new ReferenceEntry(re.key);
                    copyRe.position = re.position;
                    copyRe.refFile = re.refFile;
                    
                    copyRe.author = authors[i];
                    copyChildren.add(copyRe);
                }
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
        //partition();
    }

    public BibOutlineContainer buildJournalSort() {
        // make a shallow copy
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
        //newboc.partition();
        return newboc;
    }

    public BibOutlineContainer buildIndexSort() {
        // make a shallow copy
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
        //newboc.partition();
        return newboc;
    }


    private String differentiatingPrefix(String s1, String s2) {
        int i = 0;
        int shorter = Math.min(s1.length(), s2.length());
        for (; i < shorter; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return s1.substring(0, i+1);
            }
        }
        // the other one was shorter
        if (s1.length() == i) {
            return s1;
        } else {
            return s1.substring(0, i + 1);
        }
    }


    public void partition() {
        if (childEntries.size() < MAX_PARTITIONSIZE) {
            return;
        }
        childContainers = new ArrayList();

        // FIXME some problems with this
        
        // calculate hierarchy levels and partitions
        int totalPartitions = (int) Math.ceil((double) childEntries.size() / (double) MAX_PARTITIONSIZE);
        int partitionSize = (int) Math.ceil((double) childEntries.size() /
                (double) Math.min(totalPartitions, MAX_PARTITIONNUMBER));
        
        String prevName = ((ReferenceEntry) childEntries.get(0)).key;
        String nextName = "...";
        while (childEntries.size() > 0) {
            int partitionEnd = Math.min(childEntries.size(), partitionSize);

            // probably doesn't work...
            //List children = childEntries.subList(0, partitionEnd);
            //childEntries.removeRange(0, partitionEnd);

            // if it doesn't work...
            ArrayList children = new ArrayList();
            ListIterator liter = childEntries.listIterator();
            for (int i = 0; i < partitionEnd; i++) {
                ReferenceEntry re = (ReferenceEntry) liter.next();
                children.add(re);
                liter.remove();
            }

            String pre1 = differentiatingPrefix(((ReferenceEntry) children.get(0)).key, prevName);
            prevName = ((ReferenceEntry) children.get(children.size()-1)).key;
            nextName = childEntries.size() > 0 ? ((ReferenceEntry) childEntries.get(0)).key : "...";
            String pre2 = differentiatingPrefix(prevName, nextName);
            
            BibOutlineContainer boc = new BibOutlineContainer(children, pre1 + "..." + pre2);
            boc.partition();
            childContainers.add(boc);
        }
        childEntries = null;
    }
    
    public String toString() {
        return name;
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
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns the sorting.
     */
    public String getSorting() {
        return sorting;
    }

}
