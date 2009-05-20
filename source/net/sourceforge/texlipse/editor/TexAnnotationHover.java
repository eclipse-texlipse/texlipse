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
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/**
 * Creates a hovering text for error annotations by reading
 * problem marker's message-attribute.
 * 
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 */
public class TexAnnotationHover implements IAnnotationHover {
    
    /**
     * Creates a message out of the marker list
     * @param markers
     * @return
     */
    private String getMessage(List<String> markers) {
        if (markers.size() == 1) {
            return (String) markers.get(0);
        } else {
            StringBuilder out = new StringBuilder(
                    "There are several problems at this line:");
            for (Iterator<String> iter = markers.iterator(); iter.hasNext();) {
                String element = iter.next();
                out.append(System.getProperty("line.separator"));
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
        IDocument document= sourceViewer.getDocument();
        IAnnotationModel model= sourceViewer.getAnnotationModel();
        
        if (model == null)
            return null;
        
        List<String> lineMarkers = null;

        Iterator<Annotation> e= model.getAnnotationIterator();
        while (e.hasNext()) {
            Annotation o= e.next();
            if (o instanceof MarkerAnnotation) {
                MarkerAnnotation a= (MarkerAnnotation) o;
                if (isRulerLine(model.getPosition(a), document, lineNumber)) {
                    if (lineMarkers == null)
                        lineMarkers = new LinkedList<String>();
                    lineMarkers.add(a.getMarker().getAttribute(IMarker.MESSAGE, null));
                }
            }
        }
        if (lineMarkers != null)
            return getMessage(lineMarkers);
        
        return null;
    }
    
    private boolean isRulerLine(Position position, IDocument document, int line) {
        if (position.getOffset() > -1 && position.getLength() > -1) {
            try {
                return line == document.getLineOfOffset(position.getOffset());
            } catch (BadLocationException x) {
            }
        }
        return false;
    }

}
