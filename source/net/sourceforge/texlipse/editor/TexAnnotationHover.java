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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * Creates a hovering text for error annotations by reading
 * problem marker's message-attribute.
 * 
 * @author Kimmo Karlsson
 */
public class TexAnnotationHover implements IAnnotationHover {
    
    private TextEditor editor;
    
    /**
     * Create a new hover text provider.
     * @param edi the editor of the file
     */
    public TexAnnotationHover(TextEditor edi) {
        editor = edi;
    }
    
    /**
     * Find a problem marker from the given line and return its error message.
     * 
     * @param sourceViewer the source viewer
     * @param lineNumber line number in the file, starting from zero
     * @return the message of the marker of null, if no marker at the specified line
     */
    public String getHoverInfo(ISourceViewer sourceViewer, int lineNumber) {
        
        IEditorInput input = editor.getEditorInput();
        if (input instanceof IFileEditorInput) {
            
            IFile file = ((IFileEditorInput)input).getFile();
            try {
                
                IMarker[] list = file.findMarkers(IMarker.PROBLEM, true, IFile.DEPTH_ONE);
                for (int i = 0; i < list.length; i++) {
                    if (MarkerUtilities.getLineNumber(list[i]) == lineNumber+1) {
                        return MarkerUtilities.getMessage(list[i]);
                    }
                }
                
            } catch (CoreException e) {
            }
        }
        return null;
    }
}
