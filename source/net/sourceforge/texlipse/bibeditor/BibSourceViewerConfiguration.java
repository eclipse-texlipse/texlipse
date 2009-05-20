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

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexAnnotationHover;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;


/**
 * Configuration for the source viewer of the BibTeX
 * editor.
 * 
 * @author Oskar Ojala
 */
public class BibSourceViewerConfiguration extends SourceViewerConfiguration {

    private BibEditor editor;
    private TexAnnotationHover annotationHover;
    private ContentAssistant assistant;
    
    /**
     * Creates a new source viewer configuration.
     * 
     * @param te The editor that this configuration is associated to
     */
    public BibSourceViewerConfiguration(BibEditor te) {
        super();
        this.editor = te;
        this.annotationHover = new TexAnnotationHover();
        
        // Adds a listener for changing content assistan properties if
        // these are changed in the preferences
        TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new  
                IPropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent event) {
                
                if (assistant == null)
                    return;
                
                String property = event.getProperty();
                System.out.println(property);
                if (TexlipseProperties.BIB_COMPLETION.equals(property)) {
                    assistant.enableAutoActivation(
                            TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(
                                    TexlipseProperties.BIB_COMPLETION));
                } else if (TexlipseProperties.BIB_COMPLETION_DELAY.equals(property)) {
                    assistant.setAutoActivationDelay(
                            TexlipsePlugin.getDefault().getPreferenceStore().getInt(
                                    TexlipseProperties.BIB_COMPLETION_DELAY));
                }
            };
        });

    }

    /**
     * @return The annotation hover text provider for this editor
     */
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return annotationHover;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
     */
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
        return BibEditor.BIB_PARTITIONING;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredContentTypes(org.eclipse.jface.text.source.ISourceViewer)
     */
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] {IDocument.DEFAULT_CONTENT_TYPE, BibPartitionScanner.BIB_ENTRY};
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getPresentationReconciler(org.eclipse.jface.text.source.ISourceViewer)
     */
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

        BibColorProvider provider = TexlipsePlugin.getDefault().getBibColorProvider();
        PresentationReconciler reconciler = new PresentationReconciler();
        reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
        
        DefaultDamagerRepairer dr = new DefaultDamagerRepairer(TexlipsePlugin.getDefault().getBibCodeScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

        dr = new DefaultDamagerRepairer(TexlipsePlugin.getDefault().getBibEntryScanner());
        reconciler.setDamager(dr, BibPartitionScanner.BIB_ENTRY);
        reconciler.setRepairer(dr, BibPartitionScanner.BIB_ENTRY);
        
        return reconciler;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(org.eclipse.jface.text.source.ISourceViewer)
     */
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        assistant = new ContentAssistant();
        assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
        
        assistant.setContentAssistProcessor(new BibCompletionProcessor(this.editor.getDocumentModel()),
                BibPartitionScanner.BIB_ENTRY);
        assistant.setContentAssistProcessor(new BibCompletionProcessor(this.editor.getDocumentModel()),
                IDocument.DEFAULT_CONTENT_TYPE);

        assistant.enableAutoActivation(TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.BIB_COMPLETION));
        assistant.enableAutoInsert(true);
        assistant.setAutoActivationDelay(TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.BIB_COMPLETION_DELAY));
        assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
        assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        return assistant;
    }
}
