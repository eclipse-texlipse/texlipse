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

package org.eclipse.texlipse.editor.hover;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.texlipse.editor.TexEditor;
import org.eclipse.texlipse.texparser.LatexParserUtils;

/**
 * Objects of this class determines the hover region and returns
 * the text of the hover.
 * 
 * @author Boris von Loesch
 * @author Oskar Ojala
 */
public class TexHover implements ITextHover, ITextHoverExtension {
    
    TexEditor editor;
    TexHoverControlCreator creator;

    public TexHover(TexEditor editor) {
        this.editor = editor;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
     */
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        try {
            return textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());
        } catch (BadLocationException ex) {
            return "";
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer,
     *      int)
     */
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        try {
            // Extract current line
            int lineNr = textViewer.getDocument().getLineOfOffset(offset);
            int lOffset = textViewer.getDocument().getLineOffset(lineNr);
            String line = textViewer.getDocument().get(lOffset, textViewer.getDocument().getLineLength(lineNr));
            int start = offset - lOffset;

            IRegion r = LatexParserUtils.getCommand(line, start);
            if (r == null) return new Region(offset, 0);

            IRegion rArg = LatexParserUtils.getCommandArgument(line, r.getOffset());
            if (rArg == null) return new Region(lOffset + r.getOffset(), r.getLength());
            
            String command = line.substring(r.getOffset()+1, r.getOffset() + r.getLength());
            if (command.indexOf("cite") >= 0 && start > r.getOffset() + r.getLength()) {
                //Return only the citation entry, not the full command string
                int cEnd = rArg.getOffset() + rArg.getLength();
                int regionStart = line.lastIndexOf(',', start) < line.lastIndexOf('{', start) ?
                        line.lastIndexOf('{', start) + 1 : line.lastIndexOf(',', start) + 1;
                int lastComma = line.indexOf(',', start);
                if (lastComma >= 0 && lastComma < cEnd) {
                    return new Region(lOffset + regionStart, lastComma - regionStart);
                } else {
                    return new Region(lOffset + regionStart, cEnd - regionStart);
                }
            }

            int length = rArg.getOffset() - r.getOffset() + rArg.getLength() + 1;
            return new Region(lOffset + r.getOffset(), length);

        } catch (BadLocationException ex) {
            return new Region(offset, 0);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
     */
    public IInformationControlCreator getHoverControlCreator() {
        if (creator == null) {
            creator = new TexHoverControlCreator(editor);
        }
        return creator;
    }

}
