/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.Arrays;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.LatexParserUtils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Defines indentation strategy.
 * 
 * @author Laura Takkinen
 * @author Antti Pirinen
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public class TexAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
    
    private String indentationString = "";
    private String[] indentationItems;
    private int lineLength;
    private static boolean hardWrap = false;
    private boolean indent;

    private HardLineWrap hlw;
    private boolean autoItem = true;
    private boolean itemSetted = false;
    private int itemAtLine = 0;
    final private IPreferenceStore fPreferenceStore;
    
    /**
     * Creates new TexAutoIndentStrategy.
     * 
     * @param store
     */
    public TexAutoIndentStrategy() {
        fPreferenceStore = TexlipsePlugin.getDefault().getPreferenceStore();
        
        fPreferenceStore.addPropertyChangeListener(new IPropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent event) {
                
                String ev = event.getProperty();
                if (TexlipseProperties.WORDWRAP_LENGTH.equals(ev)) {
                    lineLength = fPreferenceStore.getInt(TexlipseProperties.WORDWRAP_LENGTH);
                }
                else if (TexlipseProperties.TEX_ITEM_COMPLETION.equals(ev)) {
                    autoItem = fPreferenceStore.getBoolean(TexlipseProperties.TEX_ITEM_COMPLETION);
                }
                else if (TexlipseProperties.INDENTATION_LEVEL.equals(ev)
                        || TexlipseProperties.INDENTATION_TABS.equals(ev)
                        || TexlipseProperties.INDENTATION.equals(ev)
                        || TexlipseProperties.INDENTATION_ENVS.equals(ev)) {
                    setIndetationPreferenceInfo();
                }
            };
            
        });
        this.hlw = new HardLineWrap();
        setIndetationPreferenceInfo();
    }
    
    /**
     * Returns a default indentation string, 
     * that is created according to the preferences
     * @return string consists of tabs or spaces
     */
    public static String getIndentationString() {
        String indentationString = "";
        if (TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.INDENTATION_TABS)) {
            indentationString = "\t";
        } else {
        	int indentationLevel = TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.INDENTATION_LEVEL);
        	for (int i = 0; i < indentationLevel; i++) {
                indentationString += " ";
            }
        }
        return indentationString;
    }
    
    
    /**
     * Initializes indentation information from preferences
     */
    private void setIndetationPreferenceInfo() {
        indentationItems = TexlipsePlugin.getPreferenceArray(TexlipseProperties.INDENTATION_ENVS);
        Arrays.sort(indentationItems);

        //tabWidth = editorPreferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
        
        indentationString = getIndentationString();
        
        indent = Boolean.parseBoolean(TexlipsePlugin.getPreference(TexlipseProperties.INDENTATION));
        lineLength = fPreferenceStore.getInt(TexlipseProperties.WORDWRAP_LENGTH);
        autoItem = fPreferenceStore.getBoolean(TexlipseProperties.TEX_ITEM_COMPLETION);
    }
    
    /*
     * (non-Javadoc) Method declared on IAutoIndentStrategy
     */
    public void customizeDocumentCommand(IDocument document, DocumentCommand command) {
        if (this.indent) {
            if (itemSetted && autoItem && command.length == 0 && command.text != null
                    && TextUtilities.endsWith(document.getLegalLineDelimiters(), command.text) != -1) {
                dropItem(document, command);
            } else if (command.length == 0 && command.text != null
                    &&  TextUtilities.endsWith(document.getLegalLineDelimiters(), command.text) != -1) {
                smartIndentAfterNewLine(document, command);
            } else if ("}".equals(command.text)) {
                smartIndentAfterBrace(document, command);
            } else {
                itemSetted = false;
            }
        }
        
        if (TexAutoIndentStrategy.hardWrap && command.length == 0 && command.text != null) {
            hlw.doWrapB(document, command, lineLength);
        }
    }
    
    
    /**
     * Decides if a "\begin{...}" needs a "\end{...}"
     * @param environment	Name of the environment (...)
     * @param document	The document as String
     * @param coffset	The starting offset (just at the beginning of
     * 					the "\begin{...}"
     * @return	true, if it needs an end, else false
     */
    public static boolean needsEnd(String environment, String docString, int coffset) {
        int counter = 1;
        int offset = coffset;
        while (counter > 0) {
            IRegion end = LatexParserUtils.findEndEnvironment(docString, environment, offset + 5);
            if (end == null) {
                return true;
            }
            IRegion start = LatexParserUtils.findBeginEnvironment(docString, environment, offset + 7);
            if (start == null) {
                counter--;
                offset = end.getOffset();
            } else if (end.getOffset() > start.getOffset()) {
                counter++;
                offset = start.getOffset();
            } else {
                counter--;
                offset = end.getOffset();
            }
        }
        return false;
    }
    
    /**
     * Returns a string with the whitespaces (spaces and tabs) that are at the beginning
     * of line
     * @param line
     * @return
     */
    public static String getIndentation(String line) {
        int offset = 0;
        while (offset < line.length() && (line.charAt(offset) == ' ' || line.charAt(offset) == '\t')) {
            offset++;
        }
        return line.substring(0, offset);
    }
    
    /**
     * Performs indentation after new line is detected.
     * 
     * @param document Document where new line is detected.
     * @param command Command that represent the change of the document (new
     *            line).
     */
    private void smartIndentAfterNewLine(IDocument document, DocumentCommand command) {
        try {
            itemSetted = false;
            int commandOffset = command.offset;
            int line = document.getLineOfOffset(commandOffset);
            int lineOffset = document.getLineOffset(line);
            String startLine = document.get(lineOffset, commandOffset - lineOffset);
            //this is save
            String lineDelimiter = document.getLegalLineDelimiters()
                [TextUtilities.endsWith(document.getLegalLineDelimiters(), command.text)];
            int beginIndex;
            if ((beginIndex = LatexParserUtils.findCommand(startLine, "\\begin", 0)) != -1) {
                // test if line contains \begin and search the environment (itemize,
                // table...)
                IRegion r = LatexParserUtils.getCommandArgument(startLine, beginIndex);
                if (r == null){
                	//No environment found
                    super.customizeDocumentCommand(document, command);
                    return;
                }
                String envName = startLine.substring(r.getOffset(), r.getOffset()+r.getLength());
                StringBuilder buf = new StringBuilder(command.text);

                // get indentation of \begin
/*                String prevIndentation = this.tools.getIndentation(document, line,
                        "\\begin", this.tabWidth); // NEW*/
                String prevIndentation = getIndentation(startLine);
                
                if (Arrays.binarySearch(this.indentationItems, envName) >= 0) {
                    buf.append(prevIndentation);
                    buf.append(this.indentationString);
                } else {
                    buf.append(prevIndentation);
                }
                
                if (autoItem && (envName.equals("itemize") || envName.equals("enumerate"))) {
                    buf.append("\\item ");
                    itemSetted = true;
                    itemAtLine = document.getLineOfOffset(command.offset);
                } else if (autoItem && envName.equals("description")) {
                    buf.append("\\item[]");
                    itemSetted = true;
                    itemAtLine = document.getLineOfOffset(command.offset);
                }

                command.caretOffset = command.offset + buf.length();
                command.shiftsCaret = false;
                if (autoItem && envName.equals("description")) {
                    command.caretOffset--;
                }
                
                /*
                 * looks for the \begin-statement and inserts
                 * an equivalent \end-statement (respects \begin-indentation)
                 */
                if (needsEnd(envName, document.get(), lineOffset)){
                    buf.append(lineDelimiter);
                    buf.append(prevIndentation);
                    buf.append("\\end{" + envName + "}");
                }
                command.text = buf.toString();
                
            } else {
                if (autoItem && !itemInserted(document, command)) {
                    super.customizeDocumentCommand(document, command);
                } else {
                    super.customizeDocumentCommand(document, command);
                }
            }
        } catch (BadLocationException e) {
            TexlipsePlugin.log("TexAutoIndentStrategy:SmartIndentAfterNewLine", e);
        }
    }
    
    /**
     * Removes indentation if \end{...} is detected. We assume that 
     * command.text is the closing brace '}'
     * 
     * @param document
     *            Document where new line is detected.
     * @param command
     *            Command that represent the change of the document (here command text is "}").
     */
    private void smartIndentAfterBrace(IDocument document, DocumentCommand command) {
        try {
            int commandOffset = command.offset;
            int line = document.getLineOfOffset(commandOffset);
            int lineOffset = document.getLineOffset(line);
            int lineLength = document.getLineLength(line);
            // the original line text
            String lineText = document.get(lineOffset, lineLength);
            // modified linetext
            String text = lineText.trim().concat(command.text);
            
            if (text.startsWith("\\end")) {
            	IRegion r = LatexParserUtils.getCommandArgument(text, 0);
            	//String envName = "";
            	if (r == null) {
            		super.customizeDocumentCommand(document, command);
            		return;
            	}
        		String envName = text.substring(r.getOffset(), r.getOffset() + r.getLength());            	
            	String docText = document.get();
            	IRegion rBegin = LatexParserUtils.findMatchingBeginEnvironment(docText, envName, lineOffset);
            	int beginLineNr = document.getLineOfOffset(rBegin.getOffset());
            	int beginLineLength = document.getLineLength(beginLineNr);
            	int beginLineStart = document.getLineOffset(beginLineNr);
            	String beginLine = document.get(beginLineStart, beginLineLength);
            	String beginInd = getIndentation(beginLine);
            	command.text = beginInd + text;
            	command.length = commandOffset - lineOffset;
            	command.offset = lineOffset;
            } else {
            	super.customizeDocumentCommand(document, command);
            }
            
        } catch (BadLocationException e) {
            TexlipsePlugin.log("TexAutoIndentStrategy:SmartIndentAfterBracket", e);
        }
    }
    
    /**
     * Erases the \item -string from a line
     * 
     * @param d
     * @param c
     */
    private void dropItem(IDocument d, DocumentCommand c) {
        try {
            if (itemSetted && itemAtLine == (d.getLineOfOffset(c.offset) - 1)) {
                IRegion r = d.getLineInformationOfOffset(c.offset);
                String line = d.get(r.getOffset(), r.getLength());
                if ("\\item".equals(line.trim()) || "\\item[]".equals(line.trim())) {
                	c.shiftsCaret = false;
                	c.length = line.length();
                	c.offset = r.getOffset();
                	c.text = getIndentation(line);
                	c.caretOffset = c.offset + c.text.length();
                }
            }
        } catch (BadLocationException e) {
            TexlipsePlugin.log("TexAutoIndentStrategy:dropItem", e);
        }
        itemSetted = false;
    }
    
    /**
     * Inserts an \item or an \item[] string. Works ONLY it \item is found at
     * the beginning of a preceeding line
     * 
     * @param d
     * @param c
     * @return <code>true</code> if item was inserted, <code>false</code>
     *         otherwise
     */
    private boolean itemInserted(IDocument d, DocumentCommand c) {
        itemSetted = false;
        try {
            int lineNr = d.getLineOfOffset(c.offset);
            int lineEnd = d.getLineOffset(lineNr) + d.getLineLength(lineNr);
            //Test if there is no text behind the cursor in the line
            if (c.offset < lineEnd - 1) return false;
            int currentLineNr = lineNr;
            
            String indentation = null;
            while (lineNr >= 0) {
            	IRegion r = d.getLineInformation(lineNr);
            	String prevLine = d.get(r.getOffset(), r.getLength());
            	if (indentation == null) indentation = getIndentation(prevLine);
            	
                if (prevLine.trim().startsWith("\\item")) {
                    StringBuilder buf = new StringBuilder(c.text);
                    buf.append(indentation);
                    if (prevLine.trim().startsWith("\\item[")) {
                    	c.shiftsCaret = false;
                    	c.caretOffset = c.offset
                    	+ buf.length() + 5
                    	+ c.text.length();
                    	buf.append("\\item[]");
                    } else {
                    	buf.append("\\item ");
                    }
                    itemSetted = true;
                    itemAtLine = currentLineNr;
                    c.text = buf.toString();
                    return true;
                }
                if (prevLine.trim().startsWith("\\begin") || prevLine.trim().startsWith("\\end"))
                    return false;
                lineNr--;
            }
        } catch (BadLocationException e) {
        	//Ignore
        }
        return false;
    }

    /**
     * @param hardWrap The hardWrap to set.
     */
    public static void setHardWrap(boolean hardWrap) {
        TexAutoIndentStrategy.hardWrap = hardWrap;
    }
}
