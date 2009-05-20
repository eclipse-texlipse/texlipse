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
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/**
 * Defines indentation strategy.
 * 
 * @author Laura Takkinen
 * @author Antti Pirinen
 * @author Oskar Ojala
 */
public class TexAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
    
    private String indentationString = "";
    private String[] indentationItems;
    private int lineLength;
    private static boolean hardWrap = false;
    private boolean indent;
    private int tabWidth;
    private TexEditorTools tools;
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
    public TexAutoIndentStrategy(final IPreferenceStore editorStore) {
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
                    setIndetationPreferenceInfo(editorStore);
                }
            };
            
        });
        this.tools = new TexEditorTools();
        this.hlw = new HardLineWrap();
        setIndetationPreferenceInfo(editorStore);
    }
    
    /**
     * Initializes indentation information from preferencepage
     */
    private void setIndetationPreferenceInfo(IPreferenceStore editorPreferenceStore) {
        indentationItems = TexlipsePlugin.getPreferenceArray(TexlipseProperties.INDENTATION_ENVS);
        int indentationLevel = fPreferenceStore.getInt(TexlipseProperties.INDENTATION_LEVEL);
        tabWidth = editorPreferenceStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
        
        indentationString = "";
        if (fPreferenceStore.getBoolean(TexlipseProperties.INDENTATION_TABS)) {
            indentationString = "\t";
        } else {
            for (int i = 0; i < indentationLevel; i++) {
                indentationString += " ";
            }
        }
        
        Arrays.sort(indentationItems);
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
    private boolean needsEnd(String environment, IDocument document, int coffset) {
        int counter = 1;
        int offset = coffset;
        String docString = document.get();
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
                String envName;
                IRegion r = LatexParserUtils.getCommandArgument(startLine, beginIndex);
                if (r == null) envName = "";
                else envName = startLine.substring(r.getOffset(), r.getOffset()+r.getLength());
                StringBuffer buf = new StringBuffer(command.text);

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
                if (needsEnd(envName, document, lineOffset)){
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
     * Performs indentation after new "}" character is detected.
     * 
     * @param document
     *            Document where new line is detected.
     * @param command
     *            Command that represent the change of the document (here command text is "}").
     */
    private void smartIndentAfterBrace(IDocument document,
            DocumentCommand command) {
        //FIXME: Tabs for indentation
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
                String endLine = this.tools.getEndLine(text, "\\end");
                
                if (Arrays.binarySearch(this.indentationItems, this.tools
                        .getEnvironment(endLine)) >= 0) {
                    int matchingBegin = this.tools.findMatchingBeginEquation(
                            document, line, this.tools.getEnvironment(endLine));
                    StringBuffer buff = new StringBuffer(this.tools
                            .getIndentation(document, matchingBegin, "\\begin",
                                    this.tabWidth));
                    buff.append(text);
                    command.offset = lineOffset;
                    command.length = commandOffset - lineOffset;
                    command.text = buff.toString();
                }
            } else {
                super.customizeDocumentCommand(document, command);
            }
        } catch (Exception e) {
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
            int one = 0;
            if (itemAtLine == (d.getLineOfOffset(c.offset) - 1)) {
                c.shiftsCaret = false;
                if (tools.getStringAt(d, c, true).trim().startsWith("\\item["))
                    one = 1;
                c.length = tools.getIndexAtLine(d, c, true) + one;
                c.offset = d.getLineOffset(d.getLineOfOffset(c.offset));
                c.caretOffset = c.offset + tools.getIndentation(d, c).length();
                c.text = tools.getIndentation(d, c);
                itemAtLine = -1;
            }
        } catch (Exception e) {
            TexlipsePlugin.log("TexAutoIndentStrategy:dropItem", e);
        }
        itemSetted = false;
    }
    
    /**
     * Insets an \item or an \item[] string Works ONLY it \item is found from
     * the beginning of the previous line
     * 
     * @param d
     * @param c
     * @return <code>true</code> if item was inserted, <code>false</code>
     *         otherwise
     */
    private boolean itemInserted(IDocument d, DocumentCommand c) {
        
        int lines = 0;
        int cnt = 0;
        itemSetted = false;
        String prevLine;
        StringBuffer buf = new StringBuffer(c.text);
        buf.append(tools.getIndentation(d, c));
        
        try {
            lines = d.getLineOfOffset(c.offset);
            while (lines > 1) {
                prevLine = tools.getStringAt(d, c, true, cnt);
                if (prevLine.trim().startsWith("\\item[")) {
                    buf.append("\\item[]");
                    c.shiftsCaret = false;
                    c.caretOffset = c.offset
                    + tools.getIndentation(d, c).length() + 6
                    + c.text.length();
                    c.text = buf.toString();
                    itemSetted = true;
                    itemAtLine = d.getLineOfOffset(c.offset);
                    return true;
                } else if (prevLine.trim().startsWith("\\item")) {
                    buf.append("\\item ");
                    c.text = buf.toString();
                    itemAtLine = d.getLineOfOffset(c.offset);
                    itemSetted = true;
                    return true;
                }
                if (tools.isLineCommandLine(prevLine))
                    return false;
                cnt--;
                lines--;
            }
        } catch (Exception e) {
            lines = -1;
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
