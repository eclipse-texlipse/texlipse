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

import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexPairMatcher;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;


/**
 * BibTeX editor.
 * 
 * @author Oskar Ojala
 */
public class BibEditor extends TextEditor {
    
    /**
     * The name of the partitioning variable used for partitioning
     * the BibTeX document.
     */
    public final static String BIB_PARTITIONING = "__bibtex_partitioning";
    
    public final static String ID = "net.sourceforge.texlipse.bibeditor.BibEditor";
    
    /** The editor's bracket matcher */
    private TexPairMatcher fBracketMatcher = new TexPairMatcher("{}[]()");

    private BibOutlinePage outlinePage;
    private BibDocumentModel documentModel;
    private BibCodeFolder folder;
    private ProjectionSupport fProjectionSupport;
    
    /**
     * Constructs a new BibTeX editor.
     */
    public BibEditor() {
        super();
        folder = new BibCodeFolder(this);
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    public void dispose() {
        if (outlinePage != null)
            outlinePage = null;
        super.dispose();
    }
        
    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#initializeEditor()
     */
    protected void initializeEditor() {
        super.initializeEditor();
        this.documentModel = new BibDocumentModel(this);
        setSourceViewerConfiguration(new BibSourceViewerConfiguration(this));
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#configureSourceViewerDecorationSupport(org.eclipse.ui.texteditor.SourceViewerDecorationSupport)
     */
    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
        // copy the necessary values from plugin preferences instead of overwriting editor preferences
    	getPreferenceStore().setValue(TexlipseProperties.MATCHING_BRACKETS,
    			TexlipsePlugin.getPreference(TexlipseProperties.MATCHING_BRACKETS));
    	
    	getPreferenceStore().setValue(TexlipseProperties.MATCHING_BRACKETS_COLOR,
    			TexlipsePlugin.getPreference(TexlipseProperties.MATCHING_BRACKETS_COLOR));
    	
    	support.setCharacterPairMatcher(fBracketMatcher);
    	support.setMatchingCharacterPainterPreferenceKeys(TexlipseProperties.MATCHING_BRACKETS,
    			TexlipseProperties.MATCHING_BRACKETS_COLOR);
    	
    	super.configureSourceViewerDecorationSupport(support);
    }
        
    /* (non-Javadoc)
     * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        ProjectionViewer projectionViewer = (ProjectionViewer) getSourceViewer();
        
        fProjectionSupport = new ProjectionSupport(projectionViewer,
                getAnnotationAccess(), getSharedColors());
        fProjectionSupport
        .addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.error");
        fProjectionSupport
        .addSummarizableAnnotationType("org.eclipse.ui.workbench.texteditor.warning");
        fProjectionSupport.install();
        
        if (TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.BIB_CODE_FOLDING)) {
            projectionViewer.doOperation(ProjectionViewer.TOGGLE);
        }
        
        this.documentModel.update();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createActions()
     */
    protected void createActions() {
        super.createActions();
        IAction a = new TextOperationAction(TexlipsePlugin.getDefault().getResourceBundle(),
        		"ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS);
        
        a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
        setAction("ContentAssistProposal", a);
        
        //This feature was removed because it causes errors
        //getDocumentProvider().getDocument(this.getEditorInput()).addDocumentListener(new BibStringCompleter(this));
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.ISaveablePart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void doSave(IProgressMonitor monitor) {
        super.doSave(monitor);
        this.documentModel.update();
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class required) {
        if (IContentOutlinePage.class.equals(required)) {
            if (this.outlinePage == null) {
                this.outlinePage = new BibOutlinePage(this);
                this.documentModel.update();
            }
            return outlinePage;
        } else if (fProjectionSupport != null) {
            Object adapter = fProjectionSupport.getAdapter(getSourceViewer(), required);
            if (adapter != null)
                return adapter;
        }
        return super.getAdapter(required);
    }
        
    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractTextEditor#createSourceViewer(org.eclipse.swt.widgets.Composite, org.eclipse.jface.text.source.IVerticalRuler, int)
     */
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        fOverviewRuler = createOverviewRuler(getSharedColors());
        
        ISourceViewer viewer = new ProjectionViewer(parent,
        		ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
        
        // ensure decoration support has been created and configured.
        getSourceViewerDecorationSupport(viewer);
        return viewer;
    }

    /**
     * Updates the code folds.
     * 
     * @param outlineTree The outline data structure containing the document positions
     */
    public void updateCodeFolder(List outlineTree) {
        this.folder.update(outlineTree);
    }
    
    /**
     * @return The outline page of this editor.
     */
    public BibOutlinePage getOutlinePage() {
        return this.outlinePage;
    }

    /**
     * @return Returns the documentModel.
     */
    public BibDocumentModel getDocumentModel() {
        return documentModel;
    }
    
    /**
     * @return The project that belongs to the current file
     * or null if it does not belong to any project
     */
    public IProject getProject() {
        IResource res = (IResource) getEditorInput().getAdapter(IResource.class);
        if (res == null) return null;
        else return res.getProject();
    }
}
