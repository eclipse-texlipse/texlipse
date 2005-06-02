/*
 * Created on Jun 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.texlipse.editor;

import java.util.HashMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author Oskar Ojala
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TexQuoteListener implements IDocumentListener {

    private ITextEditor editor;
    private IDocument document;
    
    private static HashMap quotes;
    
    public TexQuoteListener(ITextEditor editor) {
        this.editor = editor;
        this.document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (quotes == null) {
            quotes = new HashMap();
            quotes.put("eno", "``");
            quotes.put("enc", "''");
            quotes.put("fio", "''");
            quotes.put("fic", "''");
            quotes.put("fro", "<<");
            quotes.put("frc", ">>");
        }
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentChanged(DocumentEvent event) {
        // if this is enabled
        if ("\"".equals(event.getText())) {
            ITextSelection textSelection = (ITextSelection) this.editor.getSelectionProvider().getSelection();            
            try {
                char prev = document.getChar(textSelection.getOffset() - 1);
                String replacement = "\"";
                //IProject project = resource.getProject(); //IResource
                //String lang = TexlipseProperties.getProjectProperty(project, TexlipseProperties.LANGUAGE_PROPERTY);
                if (Character.isWhitespace(prev)) {
                    replacement = (String) quotes.get("eno");
                } else if (Character.isLetterOrDigit(prev)) {
                    replacement = (String) quotes.get("enc");
                } else {
                    return;
                }
                document.replace(textSelection.getOffset(), 1, replacement);
                //editor.resetHighlightRange();
                //editor.setHighlightRange(textSelection.getOffset() + 1, 1, true);
                //editor.getSelectionProvider().setSelection(new TextSelection(textSelection.getOffset() + 3, 5));
            } catch (BadLocationException e) {}
        }
    }
}
