/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibeditor;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.bibparser.BibParser;
import net.sourceforge.texlipse.editor.TexDocumentParseException;
import net.sourceforge.texlipse.model.MarkerHandler;
import net.sourceforge.texlipse.model.ReferenceContainer;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.part.FileEditorInput;


/**
 * Document model for the BibTeX -editor.
 * 
 * @author Oskar Ojala
 */
public class BibDocumentModel {

    private BibEditor editor;
    private ArrayList currentOutline;
    private ArrayList abbrevs;
    private AbbrevManager abbrManager;
    
    private ReferenceContainer bibContainer;
    
    /**
     * Constructs a new document model.
     * 
     * @param editor The editor that this model is associated with.
     */
    public BibDocumentModel(BibEditor editor) {
        this.editor = editor;
        abbrManager = new AbbrevManager();
    }
    
    /**
     * @return Returns the abbrManager.
     */
    public AbbrevManager getAbbrManager() {
        return abbrManager;
    }

    /**
     * Parses the BibTeX -document and retrieves parse errors and other useful
     * data.
     * 
     * @throws TexDocumentParseException
     *             If there is an error in the document preventing further
     *             parsing
     */
    private void doParse() throws TexDocumentParseException {
        try {
            BibParser parser = new BibParser(new StringReader(this.editor.getDocumentProvider().getDocument(this.editor.getEditorInput()).get()));
            
            this.currentOutline = parser.getEntries();
            
            ArrayList parseErrors = parser.getErrors();
            MarkerHandler marker = MarkerHandler.getInstance();
            marker.clearMarkers(editor);
            if (parseErrors.size() > 0) {
                marker.createErrorMarkers(editor, parseErrors);
                throw new TexDocumentParseException("Fatal errors in file");
            }
            
            this.abbrevs = parser.getAbbrevs();
        } catch (IOException e) {
            TexlipsePlugin.log("Can't read file.", e);
            throw new TexDocumentParseException(e);
        }
    }
    
    /**
     * Updates the abbreviation data in the abbreviation manager.
     */
    private void updateAbbrManager() {
        ReferenceEntry[] esar = new ReferenceEntry[abbrevs.size()];
        abbrevs.toArray(esar);
        Arrays.sort(esar);
        this.abbrManager.setAbbrevs(esar);
    }

    /**
     * Updates the BibTeX -data in the BibTeX-container.
     */
    private void updateBibContainer() {
        IResource resource = ((FileEditorInput)editor.getEditorInput()).getFile();
        IProject project = resource.getProject();
        if (bibContainer == null) {
            ReferenceContainer refCon = (ReferenceContainer) TexlipseProperties.getSessionProperty(project,
                    TexlipseProperties.BIBCONTAINER_PROPERTY);
            if (refCon != null) {
                bibContainer = refCon;
            } else {
                return;
            }
        }
        boolean changed = bibContainer.updateRefSource(
                resource.getFullPath().removeFirstSegments(1).toString(),
                currentOutline);
        if (changed) {
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.BIBFILES_CHANGED,
                    new Boolean(true));
        }
    }

    /**
     * Updates the outline view when outline.doSave is called.
     */
    private void updateOutline() {
        this.editor.getOutlinePage().update(this.currentOutline);
    }


    /**
     * Updates the document positions of the outline. These are used both
     * for outline navigation and code folding.
     */
    private void updateDocumentPositions() {
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        
        try {
            document.removePositionCategory(BibOutlinePage.SEGMENTS);
        } catch (BadPositionCategoryException bpce) {
            // do nothing
        }
        document.addPositionCategory(BibOutlinePage.SEGMENTS);
        
        try {
            int beginOffset = -1, endOffset = -1;
            ReferenceEntry prev = null, next = null;
            Iterator it = currentOutline.iterator();
            if (it.hasNext()) {
                prev = (ReferenceEntry) it.next();
                beginOffset = document.getLineOffset(prev.startLine - 1);
            }
            while (it.hasNext()) {
                next = (ReferenceEntry) it.next();
                
                endOffset = document.getLineOffset(next.startLine - 1);
                int length =  endOffset - beginOffset;

                prev.setPosition(beginOffset, length);
                document.addPosition(BibOutlinePage.SEGMENTS, prev.position);

                prev = next;
                beginOffset = endOffset;
            }
            if (beginOffset != -1) {
                prev.setPosition(beginOffset, document.getLength() - beginOffset);
                document.addPosition(BibOutlinePage.SEGMENTS, prev.position);                    
            }

        } catch (BadPositionCategoryException bpce) {
            TexlipsePlugin.log("BibDocumentModel.updateDocumentPositions: bad position category ", bpce);
        } catch (BadLocationException ble) {
            TexlipsePlugin.log("BibDocumentModel.updateDocumentPositions: bad position ", ble);
        }
    }
    
    /**
     * Updates the document model. This includes parsing the document
     * and retrieving updated outline and abbreviation informaiton as
     * well as updating these into the editor.
     */
    public void update() {
        try {
            doParse();
            this.updateDocumentPositions();
            updateBibContainer();
            if (this.editor.getOutlinePage() != null) {
                this.updateOutline();
            }
            updateAbbrManager();
            editor.updateCodeFolder(currentOutline);            
        } catch (TexDocumentParseException e) {
            // We do nothing, since the error is already added
//            TexlipsePlugin.log("There were parse errors in the document", e);
        }
    }
}
