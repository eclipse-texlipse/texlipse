/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.ICharacterPairMatcher;

import org.eclipse.jface.text.BadLocationException;


/**
 * @author Esa Seuranen
 *
 * A pair finder class for implementing the pair matching.
 */
public class TexPairMatcher implements ICharacterPairMatcher {
    
    // Indicate the anchor value "right"
    int RIGHT = 0;
    // Indicate the anchor value "left"
    int LEFT = 1;
    
    //current anchor position
    private int fAnchor = LEFT;
    //string of pairs, so that two consecutive characters form a pair and the first
    //character is the "left" and the latter is the "right"
    private String pairs;
    
    /**
     * Constructs a TexPairMatcher. The matching pairs are given in as a string
     * parameter so that two consecutive characters form a pair (in which
     * the first character is "left" and the latter is "right.
     * 
     * Example: String "(){}[]" has three pairs: 1. ( and ) 2. { and } 3. [ and ]  
     * 
     * @param pairs of matching characters
     */
    public TexPairMatcher(String pairs) {
        this.pairs = pairs;
        if((pairs.length() % 2) == 1) {
            TexlipsePlugin.stat("Bad parameter for TexPairMatcher constructor: " + pairs);
            this.pairs = "";
        }
    }
    
    /**
     * Disposes the TexPairMatcher
     * 
     * @see org.eclipse.jface.text.source.ICharacterPairMatcher#dispose()
     */
    public void dispose() {
    }
    
    /**
     * Clears all internal state information in preparation for <code>match</code>
     * 
     * @see org.eclipse.jface.text.source.ICharacterPairMatcher#clear()
     */
    public void clear() {		
    }
    
    /**
     * Matches smallest region between a pair. If there is another pair (of same type)
     * inside the region, it is taken into consideration. Example (numbers indicate
     * matching parens):
     * <pre>
     * (there is a region (inside) another region)
     * 1                   2      2               1    
     * </pre>
     * 
     * @param document
     * @param offset 
     * @return region (the pair included) between the matching pair, or <code>null</code>
     *   if there is no counterpair for the character at the offset (either it is
     *   not a matching pair character or it's pair does not exist)
     *  
     * @see org.eclipse.jface.text.source.ICharacterPairMatcher#match(org.eclipse.jface.text.IDocument, int)
     */
    public IRegion match(IDocument document, int offset) {
    	offset--; //we want to match pairs after we have entered the pair character
    	if(offset<0) return null;
        try {
            int index = pairs.indexOf(document.getChar(offset));
            if (index == -1) { 
                return null;
            }

            int peerIndex;
            if ((index % 2) == 1) {
                fAnchor=RIGHT;
                peerIndex = findPeerChar(document, offset, fAnchor, pairs.charAt(index), pairs.charAt(index-1));
                if (peerIndex != -1)
                    return new Region(peerIndex, offset - peerIndex + 1);
            } else {
                fAnchor=LEFT;
                peerIndex = findPeerChar(document, offset, fAnchor, pairs.charAt(index), pairs.charAt(index+1)); 
                if (peerIndex != -1)
                    return new Region(offset, peerIndex - offset + 1);
            }
            
        } catch (BadLocationException ble) {
            TexlipsePlugin.log("Bad location in TexPairMatcher.match()", ble);
        }
        return null;
    }
    
    /**
     * Finds the peercharacter for opening character (can be either "left" or "right"
     * character. The direction of the search is determined by the achor
     * (i.e. anchor==LEFT -> forward search and opening character is "left",
     * or anchor==RIGHT -> backward search and opening character is "right")
     * 
     * @param document
     * @param offset
     * @param anchor
     * @param opening 
     * @param closing matching character for opening
     * @return index of the matching closing character, or -1 if the search failed
     */
    private int findPeerChar(IDocument document, int offset, int anchor, char opening, char closing) {
        try {
            int stack = 1, index;
            index = offset;
            while (stack > 0) {
                if (anchor == LEFT) {
                	index++;
                } else {
                    index--;
                }
                if ((index < 0) || (index >= document.getLength())) {
                	index=-1;
                	break;
                }
                
                if (document.getChar(index) == opening)
                    stack++;			
                if (document.getChar(index) == closing)
                    stack--;			
            }
            return index;
        } catch (BadLocationException ble){
            TexlipsePlugin.log("Bad location in TexPariMatcher.findPeerChar", ble);
            return -1;
        }
    }
    
    /**
     * Returns anchor, i.e. whether the current character (in the document's
     * current position) is RIGHT (0) if the character is "right" character in a pair, 
     * or LEFT (1) if the current character is anything else.
     * 
     * @see org.eclipse.jface.text.source.ICharacterPairMatcher#getAnchor()
     */
    public int getAnchor() {
        return fAnchor;
    }
}
