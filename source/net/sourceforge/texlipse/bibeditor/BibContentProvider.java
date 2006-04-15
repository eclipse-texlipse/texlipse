/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibeditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.bibparser.BibOutlineContainer;
import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


/**
 * BibTeX-editor content outline content provider.
 * 
 * @author Oskar Ojala
 */
public class BibContentProvider implements ITreeContentProvider {
    
    private IPositionUpdater fPositionUpdater = new DefaultPositionUpdater(BibOutlinePage.SEGMENTS);
//    private List rootElements = new ArrayList(0);
    //private Hashtable<String, BibStringTriMap<ReferenceEntry>> sortIndex = new Hashtable<String, BibStringTriMap<ReferenceEntry>>();
    private Map contentIndex = new HashMap();
    private BibOutlineContainer content;
    private String sortBy = BibOutlineContainer.SORTNATURAL;
    private IDocument document;
    
    /**
     * Creates a new content provider for the given document.
     * 
     * @param document The document associated with this editor view
     */
    public BibContentProvider(IDocument document) {
        this.document = document;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput != null) {
            BibOutlineContainer inp = (BibOutlineContainer) newInput;
            content = inp;
            if (inp.getSorting().equals(BibOutlineContainer.SORTNATURAL)) {
                contentIndex.clear();
                contentIndex.put(BibOutlineContainer.SORTNATURAL, inp);
                
                if (!BibOutlineContainer.SORTNATURAL.equals(sortBy)) {
                    content = changeSort(sortBy);
                }
            }
            
//            this.rootElements = inp.getChildEntries();
//            if (newInput instanceof Hashtable) {
//                this.sortIndex = (Hashtable) newInput;
//            }
            if (document != null)
                document.addPositionUpdater(fPositionUpdater);
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
//        if (rootElements != null) {
//            rootElements.clear();
//            rootElements = null;
//        }
        if (contentIndex != null) {
            contentIndex.clear();
            contentIndex = null;
        }
        if (content != null) {
            content = null;
        }
    }
    
//    private Object[] getElements(BibStringTriMap<ReferenceEntry> element, String prefix) {
//        if (element != null) {
//            Object[] res;
//            if (element.isSplit()) {
//                res = element.getNextKeyLetters(prefix).toArray();
//                for (int i = 0; i < res.length; i++) {
//                    res[i] = prefix + res[i] + "...";
//                }
//            } else {
//                if (element.isUnique()) {
//                    res = element.getValues().toArray();	
//                } else {
//                    res = element.getKeys().toArray();
//                    Arrays.sort(res, Collator.getInstance());
//                }
//            }
//            return res;
//        } else {
//            return null;
//        }
//    }
    
    
    private Object[] getContainerChildren(BibOutlineContainer boc) {
        if (boc.getChildEntries() != null) {
            return boc.getChildEntries().toArray();
        } else {
            return boc.getChildContainers().toArray();
        }        
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object element) {
        return getContainerChildren(content); // (BibOutlineContainer) element
        //return rootElements.toArray();
        
//    	if (sortIndex != null) {
//    		BibStringTriMap<ReferenceEntry> elements = sortIndex.get(sortBy.toLowerCase());
//    		return getElements(elements, "");
//    	} else return null;    	
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren(Object element) {
        return !(element instanceof ReferenceEntry);
        //return element == rootElements;
        //return (element instanceof String);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent(Object element) {
       // if (element instanceof ReferenceEntry)
       //     return rootElements;
        return null;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren(Object element) {
        if (element instanceof BibOutlineContainer) {
            return getContainerChildren((BibOutlineContainer) element);
        }
        
//        if (element == rootElements)
//            return rootElements.toArray();
        
//        if (element instanceof String) { //Only String elements have children
//        	String s = (String)element;
//        	BibStringTriMap<ReferenceEntry> sortElement = sortIndex.get(sortBy);
//        	if (sortElement != null) {        		
//        		if (s.endsWith("...")) {
//        			sortElement = sortElement.getTri(s.substring(0, s.length()- 3));
//        			if (sortElement!= null) {
//        				return getElements(sortElement, s.substring(0, s.length()- 3));
//        			} else return new Object[0]; //Error, should never happen; 
//        		} else { //Get the actual ReferenceEntries belonging to the key
//        			ArrayList<ReferenceEntry> entries = sortElement.getAll((String)element);        		
//        			return entries.toArray();
//        		}
//        	}
//            return rootElements.toArray();
//        }
        return new Object[0];
    }
    
    /**
     * Change the Bibtex field by which the outline should be indexed
     * @param sBy sBy specifies the field by which the outline should be ordered
     */
    public BibOutlineContainer changeSort(String sBy) {
        BibOutlineContainer newContainer = content;
        if (contentIndex.containsKey(sBy)) {
            newContainer = (BibOutlineContainer) contentIndex.get(sBy);
        } else {
            if (BibOutlineContainer.SORTYEAR.equals(sBy)) {
                newContainer = ((BibOutlineContainer)
                        contentIndex.get(BibOutlineContainer.SORTNATURAL)).buildYearSort();
            } else if (BibOutlineContainer.SORTAUTHOR.equals(sBy)) {
                newContainer = ((BibOutlineContainer)
                        contentIndex.get(BibOutlineContainer.SORTNATURAL)).buildAuthorSort();
            } else if (BibOutlineContainer.SORTINDEX.equals(sBy)) {
                newContainer = ((BibOutlineContainer)
                        contentIndex.get(BibOutlineContainer.SORTNATURAL)).buildIndexSort();
            } else if (BibOutlineContainer.SORTJOURNAL.equals(sBy)) {
                newContainer = ((BibOutlineContainer)
                        contentIndex.get(BibOutlineContainer.SORTNATURAL)).buildJournalSort();
            }
            contentIndex.put(sBy, newContainer);
        }
    	this.sortBy = sBy;
        return newContainer;
    }
    
}
