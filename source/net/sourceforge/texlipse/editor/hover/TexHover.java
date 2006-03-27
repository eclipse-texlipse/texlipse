package net.sourceforge.texlipse.editor.hover;

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
 */
public class TexHover implements ITextHover, ITextHoverExtension {

    TexEditor editor;
    TexHoverControlCreator creator;

    public TexHover(TexEditor editor) {
        this.editor = editor;
    }

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
            int finish = start;
            //first closing brace
            int cbr = line.indexOf('}', start);
            //last opening brace
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
            }
            // else extract current word
            int dLength = line.length();
            while ((start > 0) && !isIgnoreChar(line.charAt(start)))
                start--;
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

    public boolean isIgnoreChar(char c) {
        if (Character.isLetterOrDigit(c))
            return false;
        return true;
    }

}
