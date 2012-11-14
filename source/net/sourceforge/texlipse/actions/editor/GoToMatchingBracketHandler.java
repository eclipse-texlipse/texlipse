package net.sourceforge.texlipse.actions.editor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.editor.TexPairMatcher;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.SubStatusLineManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;


/**
 * Handler for jumping to the associated brace.
 *
 * @author Boris von Loesch
 */
public class GoToMatchingBracketHandler extends AbstractHandler {

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event)
            throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
        TexEditor targetEditor;
        if (editor instanceof TexEditor) {
            targetEditor = (TexEditor) editor;
        }
        else {
            return null;
        }

        ISourceViewer sourceViewer = targetEditor.getViewer();
        IDocument document = sourceViewer.getDocument();
        if (document == null) {
            return null;
        }
        ITextSelection selection = (ITextSelection) targetEditor.getSelectionProvider().getSelection();
        SubStatusLineManager slm =
            (SubStatusLineManager) targetEditor.getEditorSite()
            .getActionBars().getStatusLineManager();

        int selectionLength = Math.abs(selection.getLength());
        if (selectionLength > 1) {
            slm.setErrorMessage(TexlipsePlugin.getResourceString("gotoMatchingBracketNotSelected"));
            slm.setVisible(true);
            sourceViewer.getTextWidget().getDisplay().beep();
            return null;
        }

        int sourceCaretOffset = selection.getOffset() + selection.getLength();

        TexPairMatcher fBracketMatcher = new TexPairMatcher("{}[]()");

        IRegion region = fBracketMatcher.match(document, sourceCaretOffset);
        if (region == null) {
            slm.setErrorMessage(TexlipsePlugin.getResourceString("gotoMatchingBracketNotFound"));
            slm.setVisible(true);
            sourceViewer.getTextWidget().getDisplay().beep();
            return null;
        }

        int offset = region.getOffset();
        int length = region.getLength();

        if (length < 1) return null;

        int anchor = fBracketMatcher.getAnchor();
        int targetOffset = (ICharacterPairMatcher.RIGHT == anchor)
                ? offset + 1 : offset + length;

        if (selection.getLength() < 0) {
            targetOffset -= selection.getLength();
        }

        sourceViewer.setSelectedRange(targetOffset, selection.getLength());
        sourceViewer.revealRange(targetOffset, selection.getLength());
        return null;
    }

}
