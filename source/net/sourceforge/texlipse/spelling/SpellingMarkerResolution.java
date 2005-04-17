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
    private int offset;
    private int length;

    /**
     * Create a spelling resolution.
     * 
     * @param begin beginning offset in the document
     * @param len length of the replacable area
     * @param str replacement string (might be of different length than the replacable area)
     * @param doc the document where the replacing occurs
     */
    public SpellingMarkerResolution(int begin, int len, String str, IDocument doc) {
        document = doc;
        solution = str;
        offset = begin;
        length = len;
    }
    
    public int getLength() {
        return length;
    }
    public int getOffset() {
        return offset;
    }
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
        try {
            document.replace(offset, length, solution);
        } catch (BadLocationException e) {
        }
        try {
            marker.delete();
        } catch (CoreException e) {
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
