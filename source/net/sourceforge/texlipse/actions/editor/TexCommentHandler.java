/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions.editor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexSelections;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Listens comment actions.
 *
 * @author Laura Takkinen
 */
public class TexCommentHandler extends AbstractHandler {

    /**
     * Comments selection.
     */
    private void comment(TexSelections selection) {
    	StringBuffer strbuf = new StringBuffer();
    	selection.selectCompleteLines();

    	try {
    		// For each line, comment them out
    		for (int i = selection.getStartLineIndex(); i < selection.getEndLineIndex(); i++) {
    			strbuf.append("% " + selection.getLine(i) + selection.getEndLineDelim());
    		}
    		// Last line shouldn't add the delimiter
    		strbuf.append("% " + selection.getLine(selection.getEndLineIndex()));

    		// Replace the text with the modified information
    		selection.getDocument().replace(selection.getStartLine().getOffset(), selection.getSelLength(), strbuf.toString());
    	}
    	catch (Exception e) {
    		TexlipsePlugin.log("TexComment.comment(): ", e);
    	}
    }

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        ITextEditor editor = TexlipseHandlerUtil.getTextEditor(event);
        TexSelections selection = new TexSelections(editor);
        comment(selection);
        return null;
    }

}
