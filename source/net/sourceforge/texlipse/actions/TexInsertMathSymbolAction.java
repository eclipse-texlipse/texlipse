package net.sourceforge.texlipse.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Simple action for inserting a Tex command into the current editor
 * @author boris
 *
 */
public class TexInsertMathSymbolAction extends Action {
	String symbol;
	ITextEditor editor;
	
	public TexInsertMathSymbolAction(String symbol) {
		super();
		this.symbol = symbol;
	}
	
	public void run() {
		ITextSelection selection = (ITextSelection)editor.getSelectionProvider().getSelection();
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		try {
			doc.replace(selection.getOffset(), 0, "\\"+symbol);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		int newOffset = selection.getOffset() + symbol.length() + 1;
		editor.getSelectionProvider().setSelection(new TextSelection(newOffset, 0));
	}

	public void setActiveEditor(IEditorPart part){
		if (part instanceof ITextEditor)
			editor = (ITextEditor) part;
	}

}
