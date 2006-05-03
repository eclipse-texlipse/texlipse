/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor.hover;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.texlipse.editor.TexEditor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

/**
 * Objects of this class determines the Hoverregion and returns
 * the text of the hover.
 * 
 * @author Boris von Loesch
 * @author Oskar Ojala
 */
public class TexHover implements ITextHover, ITextHoverExtension {

    /**
     * Pattern for recognizing a LaTeX command
     */
    // TODO only one optional argument?
    private static final Pattern recognizeCommand = Pattern.compile("\\\\(\\w+)\\s*(?:\\[.*?\\]\\s*)?\\{.+?\\}");
    
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

            int cStart = line.lastIndexOf('\\', start);
            int oBrace = line.lastIndexOf('{', start);
            int cEnd = line.indexOf('}', start);
            if (cStart >= 0 && cEnd >= 0 && cStart < oBrace) {
                String fullCommand = line.substring(cStart, cEnd + 1);
                Matcher m = recognizeCommand.matcher(fullCommand);
                if (m.matches()) {
                    String command = m.group(1);
                    if (command.indexOf("cite") >= 0) {
                        int regionStart = line.lastIndexOf(',', start) < line.lastIndexOf('{', start) ?
                                line.lastIndexOf('{', start) + 1 : line.lastIndexOf(',', start) + 1;
                        int lastComma = line.indexOf(',', start);
                        if (lastComma >= 0 && lastComma < cEnd) {
                            return new Region(lOffset + regionStart, lastComma - regionStart);
                        } else {
                            return new Region(lOffset + regionStart, cEnd - regionStart);
                        }
                    } else {
                        return new Region(lOffset + cStart, (cEnd - cStart) + 1);
                    }
                }
            } else if (cStart >= 0) {
                // The command might not have an argument
                for (int i = start; i < line.length(); i++) {
                    if (!Character.isLetter(line.charAt(i))) {
                        cEnd = i;
                        break;
                    }
                }
                return new Region(lOffset + cStart, (cEnd - cStart));
            }
            
            
            /*
            // first closing brace
            int cbr = line.indexOf('}', start);
            // last opening brace
            int obr = line.lastIndexOf('{', start);
            int insBack = line.lastIndexOf('\\', start);
            if (obr != -1 && cbr != -1 && obr > line.lastIndexOf('}', start)
                    && (line.indexOf('{', start) == -1 || cbr < line.indexOf('{', start))
                    && obr > insBack) {
                // seems we are inside {...}
                // now search for the command
                obr = line.lastIndexOf('\\', obr);
                if (obr != -1)
                    return new Region(lOffset + obr, (cbr - obr) + 1);
            }*/
            
            
            // else extract current word
            int dLength = line.length();
            while ((start > 0) && !isIgnoreChar(line.charAt(start)))
                start--;
            int finish = start;
            while ((finish < dLength) && !isIgnoreChar(line.charAt(finish)))
                finish++;
            // special case \command we want the backslash if not \\command
            if (line.charAt(start) == '\\' && (start == 0 || (start > 0 && line.charAt(start - 1) != '\\')))
                start--;
            Region r = new Region(lOffset + start + 1, (finish - start) - 1);
            return r;
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

    /**
     * Returns whether this character should be ignored
     * 
     * @param c
     * @return
     */
    private boolean isIgnoreChar(char c) {
        if (Character.isLetterOrDigit(c))
            return false;
        return true;
    }

}
