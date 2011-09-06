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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * @author Laura Takkinen, Oskar Ojala
 * 
 * This class implements user made text selections. Selection has different
 * features line indexes, selection string, startline etc. This class is used
 * for example commenting blocks feature.
 */
public class TexSelections {
    
    //Text editor
    private ITextEditor editor;
    
    //Document from where the selection is made
    private IDocument document;
    
    //The start line number of the selection 
    private int startLineIndex;
    
    //The end line number of the selection
    private int endLineIndex;
    
    //Length of selected text
    private int selLength;
    
    //The selected text
    private String selection = "";
    
    //End line delimiter
    private String endLineDelim = "";
    
    //Start line region
    private IRegion startLine;
    
    //End line region
    private IRegion endLine;
    
    //Selection
    private ITextSelection textSelection;

    /**
     * Takes a text editor as a parameter and sets variables 
     * to correspond selection features.
     * 
     * @param textEditor The currenct text editor
     */
    public TexSelections(ITextEditor textEditor) {
         
        // Get the document
        this.document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        this.editor = textEditor;
        
        // Get the selection
        this.textSelection = (ITextSelection) this.editor.getSelectionProvider().getSelection();
      
        this.startLineIndex = this.textSelection.getStartLine();
        this.endLineIndex = this.textSelection.getEndLine();
        this.selLength = this.textSelection.getLength();

        // Store selection information
        select();
    }
    
    /**
     * Make the full selection from the information in the class data.
     */
    private void select() {
        
        //special case
        if (this.endLineIndex < this.startLineIndex) {
            this.endLineIndex = this.startLineIndex;
        }
        try {
            // If anything is actually selected, we'll be modifying the selection only
            if (this.selLength > 0) {
                this.startLine = this.document.getLineInformation(this.startLineIndex);
                this.endLine = this.document.getLineInformation(this.endLineIndex);
                
                //Get line delimiter
                this.endLineDelim = this.document.getLineDelimiter(this.startLineIndex);
                
                // Get the selected text
                this.selection = this.document.get(this.textSelection.getOffset(), this.textSelection.getLength());
                
            } else { // Grab the current line only
                int initialPos = 0;
                this.startLine = this.document.getLineInformation(this.startLineIndex);
                this.endLine = this.document.getLineInformation(this.endLineIndex);
                this.selLength = this.startLine.getLength();
                
                // Get offsets
                initialPos = startLine.getOffset();
                this.endLineDelim = this.document.getLineDelimiter(this.startLineIndex);
                
                // Grab the selected text into our string
                this.selection = this.document.get(initialPos, this.selLength);				
            }
        } catch(Exception e) {
            TexlipsePlugin.log("TexSelections.select(): ", e);
        }
    }
    
    /**
     * In event of partial selection, used to select the full lines involved.
     */
    public void selectCompleteLines() {
        this.selLength = this.endLine.getOffset() + this.endLine.getLength() - this.startLine.getOffset();		
    }

    /**
     * Selects a paragraph if nothing was selected or if something was then
     * selects complete lines (for HardWrapAction)
     */
    public void selectParagraph() {
    	if (textSelection.getLength() == 0) {
    		try {
    			int offset = textSelection.getOffset();
    			String doc = document.get();

    			Pattern p = Pattern.compile("(?m)"
    					+ Pattern.quote(endLineDelim) + "\\p{Blank}*"
    					+ Pattern.quote(endLineDelim));
    			Matcher m = p.matcher(doc);

    			m.region(0, offset);
    			int paraBegin = 0;
    			// find last match before the paragraph
    			while (m.find()) paraBegin = m.end();

    			startLineIndex = document.getLineOfOffset(paraBegin);

    			// Find first match after the paragraph
    			m.region(offset, doc.length());
    			int paraEnd;
    			if (m.find()) paraEnd = m.start();
    			else paraEnd = doc.length();

    			endLineIndex = document.getLineOfOffset(paraEnd);

    			selLength = paraEnd - paraBegin;
    		} catch (BadLocationException ble) {
    			throw new RuntimeException(ble);
    		}
    	} else {
    		selectCompleteLines();
    	}
    }

    /**
     * Gets line from the document.
     * 
     * @param i Line number
     * @return String line in String form
     */
    public String getLine(int i) {
        try {
            return this.document.get(this.document.getLineInformation(i).getOffset(),
                    this.document.getLineInformation(i).getLength());
        } catch (Exception e) {
            TexlipsePlugin.log("TexSelections.getLine: ", e);
            return "";
        }
    }       

    /**
     * Gets all complete lines from the selection.
     * 
     * @return String consisting of all lines in the selection
     */
    public String getCompleteLines() {
    	try {
          return document.get(startLine.getOffset(), endLine.getOffset() + this.endLine.getLength() -this.startLine.getOffset());
    	} catch (Exception e){
            TexlipsePlugin.log("TexSelections.getCompleteLines: ", e);
    		return "";    		
    	}
    	
    }       
    
    // Getters and setters
    
    /**
     * Returns current IDocument.
     * @return Returns the document.
     */
    public IDocument getDocument() {
        return document;
    }
    /**
     * Sets current IDocument.
     * @param document The document to set.
     */
    public void setDocument(IDocument document) {
        this.document = document;
    }
    /**
     * Returns current ITextEditor.
     * @return Returns the editor.
     */
    public ITextEditor getEditor() {
        return editor;
    }
    /**
     * Sets current ITextEditor.
     * @param editor The editor to set.
     */
    public void setEditor(ITextEditor editor) {
        this.editor = editor;
    }
    /**
     * Returns current endline region.
     * @return Returns the endLine.
     */
    public IRegion getEndLine() {
        return endLine;
    }
    /**
     * Sets current endline region.
     * @param endLine The endLine to set.
     */
    public void setEndLine(IRegion endLine) {
        this.endLine = endLine;
    }
    
    /**
     * Returns current startline region.
     * @return Returns the startLine.
     */
    public IRegion getStartLine() {
        return startLine;
    }
    /**
     * Sets current startline region.
     * @param startLine The startLine to set.
     */
    public void setStartLine(IRegion startLine) {
        this.startLine = startLine;
    }
    
    /**
     * Returns current line delimiter.
     * @return Returns the endLineDelim.
     */
    public String getEndLineDelim() {
        return endLineDelim;
    }
    /**
     * Sets current line delimiter.
     * @param endLineDelim The endLineDelim to set.
     */
    public void setEndLineDelim(String endLineDelim) {
        this.endLineDelim = endLineDelim;
    }
    /**
     * Returns current endline index.
     * @return Returns the endLineIndex.
     */
    public int getEndLineIndex() {
        return endLineIndex;
    }
    /**
     * Set current endline index.
     * @param endLineIndex The endLineIndex to set.
     */
    public void setEndLineIndex(int endLineIndex) {
        this.endLineIndex = endLineIndex;
    }
    /**
     * Returns complete selection as a text String,
     * ie. if selectCompleteLines have been used
     * @return Returns the selection.
     */
    public String getCompleteSelection() {
        return selection;
    }
    /**
     * Gets selection string.
     * @return Returns the selection as a string.
     */
    public String getSelection() {
        return selection;
    }
    /**
     * Sets selection String.
     * @param selection The selection to set.
     */
    public void setSelection(String selection) {
        this.selection = selection;
    }
    /**
     * Returns the length of the current selection, which
     * is extended to the current line if nothing is selected.
     * @return Returns the selLength.
     */
    public int getSelLength() {
        return selLength;
    }
    
    /**
     * Returns the length of the initial selection.
     * @return The length of the initial selection
     */
    public int getRawSelLength() {
        return this.textSelection.getLength();
    }
  
    /**
     * Returns the start line index.
     * @return Returns the startLineIndex.
     */
    public int getStartLineIndex() {
        return startLineIndex;
    }
    /**
     * Sets the start line index.
     * @param startLineIndex The startLineIndex to set.
     */
    public void setStartLineIndex(int startLineIndex) {
        this.startLineIndex = startLineIndex;
    }
    /**
     * Returns current textSelection.
     * @return Returns the ITextSelection.
     */
    public ITextSelection getTextSelection() {
        return textSelection;
    }
    /**
     * Sets textSelection.
     * @param textSelection The textSelection to set.
     */
    public void setTextSelection(ITextSelection textSelection) {
        this.textSelection = textSelection;
    }
}
