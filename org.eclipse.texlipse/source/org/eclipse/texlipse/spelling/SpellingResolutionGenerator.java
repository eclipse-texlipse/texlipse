/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.spelling;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Generate a list of resolutions for a spelling error marker.
 * 
 * @author Kimmo Karlsson
 */
public class SpellingResolutionGenerator implements IMarkerResolutionGenerator2 {

    /**
     * Empty constructor.
     */
    public SpellingResolutionGenerator() {
    }

    /**
     * Generate resolutions for the given error marker.
     * Marker type must be SpellChecker.SPELLING_ERROR_MARKER_TYPE.
     * 
     * @param marker marker for the error
     * @return an array of resolutions for the given marker
     *         or null if an error occurs or the marker is of wrong type
     */
    public IMarkerResolution[] getResolutions(IMarker marker) {
        
        try {
            if (!SpellChecker.SPELLING_ERROR_MARKER_TYPE.equals(marker.getType())) {
                return null;
            }
        } catch (CoreException e) {
            return null;
        }
        
        String[] proposals = SpellChecker.getProposals(marker);
        if (proposals == null || proposals.length == 0) {
            return null;
        }
        
        IDocument doc = getProviderDocument();
        
        IMarkerResolution[] res = new IMarkerResolution[proposals.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new SpellingMarkerResolution(proposals[i], doc);
        }
        return res;
    }

    /**
     * @param marker spelling error marker
     * @return true, if the marker has resolutions
     */
    public boolean hasResolutions(IMarker marker) {
        String[] proposals = SpellChecker.getProposals(marker);
        return (proposals != null && proposals.length > 0);
    }

    /**
     * @return
     */
    protected IDocument getProviderDocument() {
        IEditorPart editor = TexlipsePlugin.getCurrentWorkbenchPage().getActiveEditor();
        if (editor instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) editor;
            return textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        }
        return null;
    }
}
