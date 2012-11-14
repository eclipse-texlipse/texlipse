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

import java.util.Arrays;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexSelections;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.editor.TexAutoIndentStrategy;
import net.sourceforge.texlipse.editor.TexEditorTools;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;


/**
 * Handles indentation quick fix when user selects text area
 * and performs "Correct Indentation" action. (Correct Indentation button or Ctrl+I)
 *
 * @author Laura Takkinen
 * @author Boris von Loesch
 */
public class TexCorrectIndentationHandler extends AbstractHandler {

    private TexSelections selection;
    private String indentationString = "";
    private String[] indentationItems;
    private int tabWidth;

    /**
     * Initializes indentation information from preferences.
     */
    private void setIndetationPreferenceInfo() {
        indentationItems = TexlipsePlugin.getPreferenceArray(TexlipseProperties.INDENTATION_ENVS);
        Arrays.sort(indentationItems);

        //this.tabWidth = editorPreferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);

        indentationString = TexAutoIndentStrategy.getIndentationString();
    }

    /**
     * Checks if selection needs indentation and corrects it when necessary.
     * @throws BadLocationException
     */
    private void indent() throws BadLocationException {
    	//FIXME: Refactor
    	int index = 0;
    	boolean fix = false;
    	IDocument document = selection.getDocument();
    	selection.selectCompleteLines();
    	String delimiter = document.getLineDelimiter(selection.getStartLineIndex());
    	String[] lines;

    	try {
    		lines = document.get(document.getLineOffset(selection.getStartLineIndex()), selection.getSelLength()).split(delimiter);
    	}
    	catch (BadLocationException e) {
    	    //for example new empty file
    		return;
    	}
    	//fix lines on at the time
    	String selectedLine = "";
    	String originalIndentation = "";
    	String correctIndentation = "";
    	for (int line = selection.getStartLineIndex(); index < lines.length; line++) {
    	    TexEditorTools tools = TexEditorTools.getInstance();
    		selectedLine = document.get(document.getLineOffset(line), document.getLineLength(line));
    		originalIndentation = tools.getIndentation(selectedLine, this.tabWidth);
    		correctIndentation = "";

    		//first line of end of document
    		if (line == 0 || line == document.getLength() -1) {

    		}
    		else if (line >= document.getLength()) {
    			return;
    		}
    		else if (selectedLine.indexOf("\\end") != -1) { //selected line is \end{...} -line
    			String endText = tools.getEndLine(selectedLine.trim(), "\\end");

    			if (Arrays.binarySearch(this.indentationItems, tools.getEnvironment(endText)) >= 0) {
    				int matchingBegin = tools.findMatchingBeginEquation(document, line, tools.getEnvironment(endText));

    				if (matchingBegin < selection.getStartLineIndex()) {
    					correctIndentation = tools.getIndentation(document, matchingBegin, "\\begin", this.tabWidth);
    				}
    				else {
    					correctIndentation = tools.getIndentation(lines[matchingBegin-selection.getStartLineIndex()], this.tabWidth);
    				}
    			}
    		}
    		else {
    		    //otherwise indentation is determined by the previous line
    			String lineText = "";

    			//previous line is outside the selection
    			if (index == 0) {
    				lineText = document.get(document.getLineOffset(line - 1), document.getLineLength(line - 1));
    			}
    			else { //previous line is inside the selection
    				lineText = lines[index - 1];
    			}

    			//previous line is \begin{...} -line
    			if (lineText.indexOf("\\begin") != -1) {
    				String endText = tools.getEndLine(lineText.trim(), "\\begin");
    				if (Arrays.binarySearch(this.indentationItems, tools.getEnvironment(endText)) >= 0) {
    					correctIndentation = tools.getIndentation(lineText, this.tabWidth) + this.indentationString;
    				}
    			}
    			else {
    			    //previous line is something else
    				correctIndentation = tools.getIndentation(lineText, this.tabWidth);
    			}
    		}
    		if (!correctIndentation.equals(originalIndentation)) {
    			fix = true;
    		}
    		lines[index] = correctIndentation.concat(lines[index].trim());
    		index++;
    	}
    	String newText = lines[0];
    	for (int i = 1; i < lines.length; i++) {
    		newText += delimiter;
    		newText += lines[i];
    	}
    	if (fix) {
    		document.replace(document.getLineOffset(selection.getStartLineIndex()), selection.getSelLength(), newText);
    	}
    }

    public final Object execute(ExecutionEvent event) throws ExecutionException {
        setIndetationPreferenceInfo();

        //no environments to indent
        if (this.indentationItems.length == 0) {
            return null;
        }
        else {
            //check if environments have change
            selection = new TexSelections(TexlipseHandlerUtil.getTextEditor(event));
            if (selection != null) {
                try {
                    indent();
                }
                catch (BadLocationException e) {
                    TexlipsePlugin.log("TexCorrectIndentationAction.run", e);
                }
            }
        }
        return null;
    }

}
