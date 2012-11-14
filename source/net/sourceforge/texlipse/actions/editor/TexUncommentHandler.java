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
 * Listens uncomment actions.
 *
 * @author Laura Takkinen
 */
public class TexUncommentHandler extends AbstractHandler {

    /**
     * Uncomments selected lines of text.
     *
     * @param selection tex selection object
     */
    private void uncomment(TexSelections selection) {
        StringBuffer strbuf = new StringBuffer();
        selection.selectCompleteLines();

        try {
            // For each line, comment them out
            for (int i = selection.getStartLineIndex(); i <= selection.getEndLineIndex(); i++) {
                String line = selection.getLine(i);

                //we may want to remove comments that have leading whitespace
                line = line.trim();
                if (line.startsWith("% ")) {
                    strbuf.append(line.replaceFirst("% ", "") + (i < selection.getEndLineIndex() ? selection.getEndLineDelim() : ""));
                }
                else if (line.startsWith("%")) {
                        strbuf.append(line.replaceFirst("%", "") + (i < selection.getEndLineIndex() ? selection.getEndLineDelim() : ""));
                }
                else {
                    strbuf.append(line + (i < selection.getEndLineIndex() ? selection.getEndLineDelim() : ""));
                }
            }
            // Replace the text with the modified information
            selection.getDocument().replace(selection.getStartLine().getOffset(), selection.getSelLength(), strbuf.toString());

        }
        catch (Exception e) {
            TexlipsePlugin.log("TexUncomment.uncomment(): ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        ITextEditor editor = TexlipseHandlerUtil.getTextEditor(event);
        TexSelections selection = new TexSelections(editor);
        uncomment(selection);
        return null;
    }

}
