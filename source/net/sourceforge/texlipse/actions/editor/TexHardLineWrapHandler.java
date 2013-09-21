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

import java.util.HashSet;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexSelections;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.editor.TexEditorTools;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * This class handles the action based text wrapping.
 *
 * @author Antti Pirinen
 * @author Oskar Ojala
 */
public class TexHardLineWrapHandler extends AbstractHandler {

    private class TextWrapper {

        private StringBuffer tempBuf = new StringBuffer();
        private TexEditorTools tools;
        private String delimiter;

        TextWrapper(TexEditorTools tet, String delim) {
            this.tools = tet;
            this.delimiter = delim;
        }

        private void storeUnwrapped(String s) {
            tempBuf.append(s);
            tempBuf.append(" ");
        }

        private String loadWrapped(String indentation) {
            String wrapped = tools.wrapWordString(tempBuf.toString(),
                    indentation, lineLength, delimiter);
            tempBuf = new StringBuffer();
            return wrapped;
        }
    }

    private int tabWidth = 2;
    private int lineLength = 80;

    private static Set<String> environmentsToProcess = new HashSet<String>();

    static {
        environmentsToProcess.add("document");
    }

    private void doWrapB(TexSelections selection) throws BadLocationException {
        TexEditorTools tools = TexEditorTools.getInstance();
        selection.selectParagraph();
        String delimiter = tools.getLineDelimiter(selection.getDocument());
        IDocument document = selection.getDocument();
        // FIXME complete selection just returns the current line
        //String[] lines = selection.getCompleteSelection().split(delimiter);
        String[] lines = document.get(document.getLineOffset(selection.getStartLineIndex()), selection.getSelLength()).split(delimiter);
        if (lines.length == 0) {
            return;
        }
        // FIXME doc.get
        String endNewlines = tools.getNewlinesAtEnd(document.get(document.getLineOffset(selection.getStartLineIndex()), selection.getSelLength()),
                delimiter);

        StringBuffer newText = new StringBuffer();
        TextWrapper wrapper = new TextWrapper(tools, delimiter);

        boolean inEnvironment = false;
        String environment = "";

        String indentation = "";
        String newIndentation;

        for (int index = 0; index < lines.length; index++) {
            String trimmedLine = lines[index].trim();

            if (tools.isLineCommandLine(trimmedLine) || inEnvironment) {
                // command lines or environments -> don't wrap them

                newText.append(wrapper.loadWrapped(indentation));
                newText.append(lines[index]);
                newText.append(delimiter);

                // TODO this will not find a match in case begins and ends
                // are scattered on one line
                String[] command = tools.getEnvCommandArg(trimmedLine);
                if (!environmentsToProcess.contains(command[1])) {
                    if ("begin".equals(command[0]) && !inEnvironment) {
                        inEnvironment = true;
                        environment = command[1];
                    }
                    else if ("end".equals(command[0])
                            && inEnvironment
                            && environment.equals(command[0])) {
                        inEnvironment = false;
                        environment = "";
                    }
                }
            }
            else if (trimmedLine.length() == 0){
                // empty lines -> don't wrap them

                newText.append(wrapper.loadWrapped(indentation));
                newText.append(lines[index]);
                newText.append(delimiter);
            }
            else {
                // normal paragraphs -> buffer and wrap

                if (tools.isLineCommentLine(trimmedLine)) {
                    newIndentation = tools.getIndentationWithComment(lines[index]);
                    trimmedLine = trimmedLine.substring(1).trim(); // FIXME remove all % signs
                }
                else {
                    newIndentation = tools.getIndentation(lines[index], tabWidth);
                }
                if (!indentation.equals(newIndentation)) {
                    newText.append(wrapper.loadWrapped(indentation));
                }
                indentation = newIndentation;
                wrapper.storeUnwrapped(trimmedLine);

                if (trimmedLine.endsWith("\\\\")
                        || trimmedLine.endsWith(".")
                        || trimmedLine.endsWith(":")) {
                    // On forced breaks, end of sentence or enumerations keep existing breaks
                    newText.append(wrapper.loadWrapped(indentation));
                }
            }
        }
        // empty the buffer
        newText.append(wrapper.loadWrapped(indentation));

        // put old delims here
        newText.delete(newText.length() - delimiter.length(), newText.length());
        newText.append(endNewlines);

//        selection.getDocument().replace(selection.getTextSelection().getOffset(),
//                selection.getSelLength(),
//                newText.toString());

        document.replace(document.getLineOffset(selection.getStartLineIndex()),
                selection.getSelLength(),
                newText.toString());
    }

    /**
     * {@inheritDoc}
     */
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        this.lineLength = TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.WORDWRAP_LENGTH);
        this.tabWidth = TexlipsePlugin.getDefault().getPreferenceStore()
                .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
        ITextEditor editor = TexlipseHandlerUtil.getTextEditor(event);
        TexSelections selection = new TexSelections(editor);
        try {
            doWrapB(selection);
        }
        catch (BadLocationException e) {
            TexlipsePlugin.log("TexCorrectIndentationAction.run", e);
        }
        return null;
    }

}
