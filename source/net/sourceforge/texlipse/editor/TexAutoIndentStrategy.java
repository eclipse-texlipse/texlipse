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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DefaultAutoIndentStrategy;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

/**
 * Defines indentation strategy.
 * 
 * @author Laura Takkinen
 * @author Antti Pirinen
 */
public class TexAutoIndentStrategy extends DefaultAutoIndentStrategy {

    private String indentationString = "";
    private String[] indentationItems;
    private int lineLength;
    public static boolean hardWrap = false;
    private boolean indent;
    private int tabWidth;
    private TexEditorTools tools;
    private HardLineWrap hlw;
    private boolean autoItem = true;
    private boolean itemSetted = false;
    private int itemAtLine = 0;

    /**
     * Creates new TexAutoIndentStrategy.
     * 
     * @param store
     */
    public TexAutoIndentStrategy(IPreferenceStore store) {
        TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new
        	IPropertyChangeListener() {
           	public void propertyChange(PropertyChangeEvent event) {
                        int ll, tl;
                        boolean ai;
                        String ev = event.getProperty();
                        if (TexlipseProperties.WORDWRAP_LENGTH.equals(ev)) {
                            ll = TexlipsePlugin.getDefault()
                                    .getPreferenceStore().getInt(TexlipseProperties.WORDWRAP_LENGTH);
                            setLineLength(ll);
                        }
                        if (TexlipseProperties.TEX_ITEM_COMLETION.equals(ev)) {
                            autoItem = TexlipsePlugin.getDefault().getPreferenceStore()
                                    .getBoolean(TexlipseProperties.TEX_ITEM_COMLETION);
                        }
                        if (TexlipseProperties.INDENTATION_LEVEL.equals(ev)
                                || TexlipseProperties.INDENTATION.equals(ev)
                                || TexlipseProperties.INDENTATION_ENVS.equals(ev)) {
                            setIndetationPreferenceInfo(TexlipsePlugin
                                    .getDefault().getPreferenceStore());
                        }
                    };
                });
        this.tools = new TexEditorTools();
        this.hlw = new HardLineWrap();
        setIndetationPreferenceInfo(store);
    }

    /**
     * Initializes indentation information from preferencepage
     */
    private void setIndetationPreferenceInfo(
            IPreferenceStore editorPreferenceStore) {
        Boolean indentation = new Boolean(TexlipsePlugin
                .getPreference(TexlipseProperties.INDENTATION));
        this.indentationItems = TexlipsePlugin
                .getPreferenceArray(TexlipseProperties.INDENTATION_ENVS);
        int indentationLevel = TexlipsePlugin.getDefault().getPreferenceStore()
                .getInt(TexlipseProperties.INDENTATION_LEVEL);
        this.tabWidth = editorPreferenceStore
                .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);

        this.indentationString = "";
        for (int i = 0; i < indentationLevel; i++) {
            this.indentationString += " ";
        }

        Arrays.sort(this.indentationItems);
        this.indent = indentation.booleanValue();
        this.lineLength = TexlipsePlugin.getDefault().getPreferenceStore()
                .getInt(TexlipseProperties.WORDWRAP_LENGTH);
        this.autoItem = TexlipsePlugin.getDefault().getPreferenceStore()
                .getBoolean(TexlipseProperties.TEX_ITEM_COMLETION);
    }

    /*
     * (non-Javadoc) Method declared on IAutoIndentStrategy
     */
    public void customizeDocumentCommand(IDocument document,
            DocumentCommand command) {
        if (this.indent) {
            if (command.length == 0 && command.text != null
                    && this.tools.endsWithNewline(document, command.text)
                    && itemSetted && autoItem) {
                dropItem(document, command);

            } else if (command.length == 0 && command.text != null
                    && this.tools.endsWithNewline(document, command.text)) {
                smartIndentAfterNewLine(document, command);
            } else if (command.text.equals("}")) {
                smartIndentAfterBracket(document, command);
            } else {
                itemSetted = false;
            }
        }

        if (hardWrap) {
            if (command.length == 0 && command.text != null) {
                hlw.doWrap(document, command, lineLength);
            }
        }
    }

    /**
     * Sets new line length.
     * 
     * @param length
     *            New line length value.
     */
    public void setLineLength(int length) {
        this.lineLength = length;
    }

    /**
     * Sets new tabwidth.
     * 
     * @param length
     *            Number of spaces in tab.
     */
    public void setTabWidth(int length) {
        this.tabWidth = length;
    }

    /**
     * Performs indentation after new line is detected.
     * 
     * @param document
     *            Document where new line is detected.
     * @param command
     *            Command that represent the change of the document (new line).
     */
    private void smartIndentAfterNewLine(IDocument document,
            DocumentCommand command) {
        try {
            itemSetted = false;
            int commandOffset = command.offset;
            int line = document.getLineOfOffset(commandOffset);
            int lineOffset = document.getLineOffset(line);
            int lineLength = document.getLineLength(line);
            String startLine = document.get(lineOffset, commandOffset
                    - lineOffset);
            String endLine = document.get(commandOffset, lineLength
                    + lineOffset - commandOffset);

            // test if line contains \begin and search the environment (itemize,
            // table...)
            int beginIndex;
            int endIndex;
            if ((beginIndex = startLine.indexOf("\\begin")) != -1) {
                String endText = this.tools.getEndLine(startLine
                        .substring(beginIndex), "\\begin");
                StringBuffer buf = new StringBuffer(command.text);
                String prevIndentation = "";
                //get identation of \begin
                prevIndentation = this.tools.getIndentation(document, line,
                        "\\begin", this.tabWidth); // NEW

                if (Arrays.binarySearch(this.indentationItems, this.tools
                        .getEnvironment(endText)) >= 0) {
                    buf.append(prevIndentation);
                    buf.append(this.indentationString);

                }
                if ((this.tools.getEnvironment(endText).equals("itemize") || this.tools
                        .getEnvironment(endText).equals("enumerate"))
                        && autoItem) {
                    buf.append("\\item ");
                    itemSetted = true;
                    itemAtLine = document.getLineOfOffset(command.offset);
                } else if (this.tools.getEnvironment(endText).equals(
                        "description")
                        && autoItem) {
                    buf.append("\\item[]");
                    itemSetted = true;
                    itemAtLine = document.getLineOfOffset(command.offset);
                }
                // NEW from here to the else-if
                // neither itemize- nor description-environment
                else {
                    buf.append(prevIndentation);
                }
                /*
                 * looks for the \begin-statement and inserts
                 * an equivalent \end-statement (respects \begin-indentation)
                 */
                buf.append("\n" + prevIndentation + "\\end{"
                        + this.tools.getEnvironment(endText) + "}");
                command.shiftsCaret = false;
                if (this.tools.getEnvironment(endText).equals("description")
                        && autoItem) {
                    command.caretOffset = command.offset + buf.length()
                            - prevIndentation.length() - "\\end{}".length()
                            - this.tools.getEnvironment(endText).length() - 2;
                } else {
                    command.caretOffset = command.offset + buf.length()
                            - prevIndentation.length() - 7
                            - this.tools.getEnvironment(endText).length();
                }
                command.text = buf.toString();

                // test if line contains \end and search the environment
                // (itemize, table...)
            } else if ((endIndex = startLine.indexOf("\\end")) != -1) {
                String endText = this.tools.getEndLine(startLine
                        .substring(endIndex), "\\end");

                if (Arrays.binarySearch(this.indentationItems, this.tools
                        .getEnvironment(endText)) >= 0) {
                    int matchingBegin = this.tools.findMatchingBeginEquation(
                            document, line, this.tools.getEnvironment(endText));
                    String prevIndentation = this.tools.getIndentation(
                            document, matchingBegin, "\\begin", this.tabWidth);
                    StringBuffer buf = new StringBuffer(prevIndentation);
                    buf.append(this.tools.trimBegin(startLine));
                    buf.append(command.text);
                    buf.append(prevIndentation);
                    buf.append(endLine);
                    command.shiftsCaret = false;
                    command.caretOffset = lineOffset
                            + this.tools.getIndexAtLine(document, command, true)
                            - (this.tools.getIndentation(document, command)
                                    .length() - prevIndentation.length())
                            + this.tools.getLineDelimiter(document, command)
                                    .length() + prevIndentation.length();
                    command.offset = lineOffset;
                    command.length = lineLength;
                    command.text = buf.toString();
                }
            } else {
                if (autoItem) {
                    if (!itemInserted(document, command)) {
                        super.customizeDocumentCommand(document, command);
                    }
                } else {
                    super.customizeDocumentCommand(document, command);
                }
            }
        } catch (Exception e) {
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
    private void smartIndentAfterBracket(IDocument document,
            DocumentCommand command) {
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
     * the beginin of the previous line
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
}
