/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.spelling;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

/**
 * Resolution for a spelling error.
 *  
 * @author Kimmo Karlsson
 */
public class SpellingMarkerResolution implements IMarkerResolution2 {

    private String solution;
    private IDocument document;

    /**
     * Create a spelling resolution.
     * 
     * @param begin beginning offset in the document
     * @param len length of the replacable area
     * @param str replacement string (might be of different length than the replacable area)
     * @param doc the document where the replacing occurs
     */
    public SpellingMarkerResolution(String str, IDocument doc) {
        document = doc;
        solution = str;
    }
    
    /**
     * @return the solution string
     */
    public String getSolution() {
        return solution;
    }
    
    /**
     * @return label for the resolution dialog 
     */
    public String getLabel() {
        return solution;
    }

    /**
     * Do the actual marker resolution.
     * 
     * @param marker the marker to resolve
     */
    public void run(IMarker marker) {
        
        int charBegin = marker.getAttribute(IMarker.CHAR_START, -1);
        int charEnd = marker.getAttribute(IMarker.CHAR_END, -1);

        String str = document.get();
        
        if (charBegin > 0 && str.length() < charEnd
                && (!Character.isWhitespace(str.charAt(charBegin-1))
                    || !Character.isWhitespace(str.charAt(charEnd)))) {
            charBegin++;
            charEnd++;
            while (str.length() < charEnd && !Character.isWhitespace(str.charAt(charBegin-1))) {
                charBegin++;
                charEnd++;
            }
        }
        
        try {
            // add word to user dictionary
            if (this.solution.equals(SpellChecker.SPELL_CHECKER_ADD)) {
                String word = document.get(charBegin, charEnd-charBegin);
                SpellChecker.addWordToAspell(word);
            } else {
                // replace word in document only if user chose a replacement word
                if (!this.solution.equals(SpellChecker.SPELL_CHECKER_IGNORE)) {
                    document.replace(charBegin, charEnd-charBegin, solution);
                }
            }
        } catch (BadLocationException e) {
            TexlipsePlugin.log("Replacing Spelling Marker", e);
        }
        
        // delete marker
        try {
            marker.delete();
        } catch (CoreException e) {
            TexlipsePlugin.log("Removing Spelling Marker", e);
        }
    }

    /**
     * @return description for the resolution
     */
    public String getDescription() {
        return TexlipsePlugin.getResourceString("spellCheckerReplaceWithCorrect");
    }

    /**
     * @return icon for the resolution
     */
    public Image getImage() {
        return TexlipsePlugin.getImage("replacetext");
    }
}
