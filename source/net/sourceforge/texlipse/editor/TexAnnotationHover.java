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

import java.util.Iterator;
import java.util.Vector;

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
 * @author Boris von Loesch
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
     * Creates a message out of the marker Vector
     * @param markers
     * @return
     */
    private String getMessage(Vector markers) {
        if (markers.size() == 1) {
            return (String) markers.get(0);
        } else {
            StringBuffer out = new StringBuffer(
                    "There are several problems at this line:");
            for (Iterator iter = markers.iterator(); iter.hasNext();) {
                String element = (String) iter.next();
                out.append("\n");
                out.append(" -");
                out.append(element);
            }
            return out.toString();
        }
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
                // Vector of Strings
                Vector lineMarkers = null;
                IMarker[] list = file.findMarkers(IMarker.PROBLEM, true, IFile.DEPTH_ONE);
                for (int i = 0; i < list.length; i++) {
                    if (MarkerUtilities.getLineNumber(list[i]) == lineNumber + 1) {
                        if (lineMarkers == null)
                            lineMarkers = new Vector(1);
                        lineMarkers.add(MarkerUtilities.getMessage(list[i]));
                    }
                }
                if (lineMarkers != null)
                    return getMessage(lineMarkers);
            } catch (CoreException e) {
            }
        }
        return null;
    }
}
