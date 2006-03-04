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
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

/**
 * Completion proposal for spelling errors.
 * 
 * @see SpellingMarkerResolution
 * @author Kimmo Karlsson
 */
public class SpellingCompletionProposal implements ICompletionProposal {

    // solution string
    private String solution;

    // error marker
    private IMarker marker;

    /**
     * Constructs a new completion proposal for spelling correction.
     * @param solution solution string
     * @param marker spelling error marker
     */
    public SpellingCompletionProposal(String solution, IMarker marker) {
        this.solution = solution;
        this.marker = marker;
    }

    /**
     * Inserts the proposed completion into the given document.
     *
     * @param document the document into which to insert the proposed completion
     */
    public void apply(IDocument document) {
        try {
            int charStart = marker.getAttribute(IMarker.CHAR_START, -1);
            int documentOffset = charStart;
            int wordLength = marker.getAttribute(IMarker.CHAR_END, -1) - charStart;
            
            // add word to user dictionary
            if (this.solution.equals(SpellChecker.SPELL_CHECKER_ADD)) {
                String word = document.get(documentOffset, wordLength);
                SpellChecker.addWordToAspell(word);
            } else {
                // replace word in document only if user chose a replacement word
                if (!this.solution.equals(SpellChecker.SPELL_CHECKER_IGNORE)) {
                    document.replace(documentOffset, wordLength, this.solution);
                }
            }
            
        } catch (BadLocationException e) {
            TexlipsePlugin.log("Replacing Spelling Marker", e);
        }
        
        // delete marker in any case
        try {
            marker.delete();
        } catch (CoreException e) {
            TexlipsePlugin.log("Removing Spelling Marker", e);
        }
    }

    /**
     * Returns the new selection after the proposal has been applied to 
     * the given document in absolute document coordinates. If it returns
     * <code>null</code>, no new selection is set.
     * 
     * A document change can trigger other document changes, which have
     * to be taken into account when calculating the new selection. Typically,
     * this would be done by installing a document listener or by using a
     * document position during {@link #apply(IDocument)}.
     *
     * @param document the document into which the proposed completion has been inserted
     * @return the new selection in absolute document coordinates
     */
    public Point getSelection(IDocument document) {
        int offset = marker.getAttribute(IMarker.CHAR_START, -1);
        if (offset == -1) {
            return null;
        }
        return new Point(offset, solution.length());
    }

    /**
     * Returns optional additional information about the proposal.
     * The additional information will be presented to assist the user
     * in deciding if the selected proposal is the desired choice.
     *
     * @return the additional information or <code>null</code>
     */
    public String getAdditionalProposalInfo() {
        return solution;
    }

    /**
     * Returns the string to be displayed in the list of completion proposals.
     *
     * @return the string to be displayed
     */
    public String getDisplayString() {
        return solution;
    }

    /**
     * Returns the image to be displayed in the list of completion proposals.
     * The image would typically be shown to the left of the display string.
     *
     * @return the image to be shown or <code>null</code> if no image is desired
     */
    public Image getImage() {
        return TexlipsePlugin.getImage("replacetext");
    }

    /**
     * Returns optional context information associated with this proposal.
     * The context information will automatically be shown if the proposal
     * has been applied.
     *
     * @return the context information for this proposal or <code>null</code>
     */
    public IContextInformation getContextInformation() {
        return null;
    }
}
