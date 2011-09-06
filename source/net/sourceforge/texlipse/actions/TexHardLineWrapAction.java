/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions;

import java.util.HashSet;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.editor.TexEditorTools;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * This class handles the action based text wrapping.
 * 
 * @author Antti Pirinen
 * @author Oskar Ojala
 */
public class TexHardLineWrapAction implements IEditorActionDelegate {
    private IEditorPart targetEditor;
    private int tabWidth = 2;
    private int lineLength = 80;
    private TexEditorTools tools;
    //private static TexSelections selection;
    
    private static Set environmentsToProcess = new HashSet();
    
    static {
        environmentsToProcess.add("document");
    }
    
    
    public TexHardLineWrapAction() {
        this.tools = new TexEditorTools();
    }


    /**
     * From what editot the event will come.
     * @param action 		not used in this method, can also be </code>null</code>
     * @param targetEditor	the editor that calls this class
     * @see org.eclipse.ui.IEditorActionDelegate#setActiveEditor(org.eclipse.jface.action.IAction, org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
        this.targetEditor = targetEditor;
    }
    
    
    /** 
     * When the user presses <code>Esc, q</code> or selects from menu bar
     * <code>Wrap Lines</code> this method is invoked.
     * @param action	an action that invokes  
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
        this.lineLength = TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.WORDWRAP_LENGTH);
        this.tabWidth = TexlipsePlugin.getDefault().getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
        TexSelections selection = new TexSelections(getTexEditor());
        try {
            doWrapB(selection);
        } catch(BadLocationException e) {
            TexlipsePlugin.log("TexCorrectIndentationAction.run", e);
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof TextSelection) {
            action.setEnabled(true);
            return;
        }
        action.setEnabled( targetEditor instanceof ITextEditor);		
    }
    
    /**
     * Returns the TexEditor.
     */
    private TexEditor getTexEditor() {
        if (targetEditor instanceof TexEditor) {
            return (TexEditor) targetEditor;
        } else {
            throw new RuntimeException("Expecting text editor. Found:"+targetEditor.getClass().getName());
        }
    }
    
    /**
     * This method does actual wrapping...
     * @throws BadLocationException
     */
    private void doWrap(TexSelections selection) throws BadLocationException {
        boolean itemFound = false;
        IDocument document = selection.getDocument();
        //selection.selectCompleteLines();
        selection.selectParagraph();
        String delimiter = document.getLineDelimiter(selection.getStartLineIndex());
        //String[] lines = document.get(document.getLineOffset(selection.getStartLineIndex()), selection.getSelLength()).split(delimiter);
        String[] lines = selection.getCompleteSelection().split(delimiter);
        if (lines.length == 0) return;
        int index = 0;
        StringBuffer buff = new StringBuffer();
        boolean fix = true;
        
        String selectedLine = "";
        String correctIndentation = "";
        
        while (index < lines.length) {
            if (tools.isLineCommandLine(lines[index]) || 
                    tools.isLineCommentLine(lines[index]) ||
                    lines[index].trim().length() == 0) {	
                buff.append(lines[index]);					
                if (lines[index].trim().length() == 0 ||
                        isList(lines[index])) {
                    fix = true;
                }
                index++;
                if (index < lines.length)
                    buff.append(delimiter);
                continue;
            }
            
            // a current line is NOT a comment, a command or an empty line -> continue
            // OO: fix empty lines and lists, but only on the next iteration?
            if (fix) {
                correctIndentation = tools.getIndentation(lines[index], tabWidth);
                fix = false;
            }
            StringBuffer temp = new StringBuffer();
            
            boolean end = false;
            while (index < lines.length && !end) {
                if (!tools.isLineCommandLine(lines[index]) &&
                        !tools.isLineCommentLine(lines[index]) && 
                        lines[index].trim().length() > 0) {
                    if (lines[index].trim().startsWith("\\item") && !itemFound) {
                        end = true;
                        itemFound = true;
                    } else {
                        temp.append(lines[index].trim() + " ");
                        itemFound = false;
                        //Respect \\ with a subsequent line break
                        if (lines[index].trim().endsWith("\\\\")) { 
                            end = true;
                        }
                        index++;
                    }
                    
                } else {
                    /* a current line is a command, a comment or en empty -> 
                     do not handle the line at this iteration. */
                    end = true;  
                }
            }
            int wsLast = 0; 
            selectedLine = temp.toString().trim();
            while (selectedLine.length() > 0) {				
                /* find the last white space before MAX */  
                wsLast = tools.getLastWSPosition(selectedLine, 
                        (lineLength - correctIndentation.length())) + 1;
                if (wsLast == 0) {
                    /* there was no white space before MAX, try if there is 
                     one after */
                    wsLast = tools.getFirstWSPosition(selectedLine, 
                            (lineLength - correctIndentation.length())) + 1;
                }
                if (wsLast == 0 || wsLast > selectedLine.length() || 
                        selectedLine.length() < (lineLength - correctIndentation.length())){
                    //there was no white space character at the line 
                    wsLast = selectedLine.length();
                } 
                
                buff.append(correctIndentation);
                buff.append(selectedLine.substring(0,wsLast));
                selectedLine = selectedLine.substring(wsLast);
                selectedLine = tools.trimBegin(selectedLine);
                if (index < lines.length || selectedLine.length() > 0)
                    buff.append(delimiter);
            }
        }
//        document.replace(selection.getTextSelection().getOffset(),
//                selection.getSelLength(),
//                buff.toString());
        document.replace(document.getLineOffset(selection.getStartLineIndex()),
                selection.getSelLength(),
                buff.toString());
    }
    
    /**
     * Checks if the command word is \begin{itemize} or \begin{enumerate}
     * @param txt	string to test
     * @return		<code>true</code> if txt contains \begin{itemize} or 
     * 				\begin{enumerate}, <code>false</code> otherwise
     */
    private boolean isList(String txt){
        boolean rv = false;
        int bi = -1;
        if ((bi = txt.indexOf("\\begin")) != -1) {
            String end = tools.getEndLine(txt.substring(bi), "\\begin");
            String env = tools.getEnvironment(end);
            if (env.equals("itemize") || 
                    env.equals("enumerate")||
                    env.equals("description"))
                rv = true;
        }
        return rv;
    }
    
    // testing
    
    private void doWrapB(TexSelections selection) throws BadLocationException {
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
                    } else if ("end".equals(command[0])
                            && inEnvironment
                            && environment.equals(command[0])) {
                        inEnvironment = false;
                        environment = "";
                    }
                }
            } else if (trimmedLine.length() == 0){ 
                // empty lines -> don't wrap them
                
                newText.append(wrapper.loadWrapped(indentation));
                newText.append(lines[index]);
                newText.append(delimiter);
            } else {
                // normal paragraphs -> buffer and wrap
                
                if (tools.isLineCommentLine(trimmedLine)) {
                    newIndentation = tools.getIndentationWithComment(lines[index]);
                    trimmedLine = trimmedLine.substring(1).trim(); // FIXME remove all % signs
                } else {
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
    
}
