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
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.scanner.TexCommentScanner;
import net.sourceforge.texlipse.editor.scanner.TexMathScanner;
import net.sourceforge.texlipse.editor.scanner.TexScanner;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;




/**
 * Configuration for the source viewer of the LaTeX
 * editor.
 * 
 * @author Oskar Ojala
 * @author Antti Pirinen
 */
public class TexSourceViewerConfiguration extends SourceViewerConfiguration {

    private TexEditor editor;
    private TexMathScanner mathScanner;
    private TexScanner scanner;
    private TexCommentScanner commentScanner;
    private ColorManager colorManager;
    private TexAnnotationHover annotationHover;
    private ContentAssistant assistant;

    /**
     * Creates a new source viewer configuration.
     * 
     * @param te The editor that this configuration is associated to
     */
    public TexSourceViewerConfiguration(TexEditor te) {
        super();
        this.editor = te;
        this.colorManager = new ColorManager();
        this.annotationHover = new TexAnnotationHover(editor);
        
        // Adds a listener for changing content assistan properties if
        // these are changed in the preferences
        TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new  
                IPropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent event) {
                
                if (assistant == null)
                    return;
                
                String property = event.getProperty();
                if (TexlipseProperties.TEX_COMPLETION.equals(property)) {
                    assistant.enableAutoActivation(
                            TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(
                                    TexlipseProperties.TEX_COMPLETION));
                } else if (TexlipseProperties.TEX_COMPLETION_DELAY.equals(property)) {
                    assistant.setAutoActivationDelay(
                            TexlipsePlugin.getDefault().getPreferenceStore().getInt(
                                    TexlipseProperties.TEX_COMPLETION_DELAY));
                }
            };
        });
        
    }

    /**
     * @return the annotation hover text provider for this editor
     */
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return annotationHover;
    }
    
    // the deprecated interface must be used as a return value, since the extended class hasn't been
    // updated to reflect the change
    public IAutoIndentStrategy getAutoIndentStrategy(ISourceViewer sourceViewer, String contentType) {
        return new TexAutoIndentStrategy(editor.getPreferences());
    }
    /**
     * Returns the configured partitioning for the given source viewer. 
     * The partitioning is used when the querying content types from the 
     * source viewer's input document. 
	 * @param sourceViewer 	the source viewer to be configured by this 
	 * 						configuration
	 * @return 				the configured partitioning
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredContentTypes(ISourceViewer)
     */
    public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
        return TexEditor.TEX_PARTITIONING;
    }
    
    /**
     * A method to get allowed content types.
     * @param sourceViewer	the source viewer to be configured by this 
	 * 						configuration
     * @return 				a new String[] array of content types.
     */
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
        return new String[] {
                IDocument.DEFAULT_CONTENT_TYPE,
                TexPartitionScanner.TEX_MATH,
                TexPartitionScanner.TEX_CURLY_BRACKETS,
                TexPartitionScanner.TEX_SQUARE_BRACKETS,
                TexPartitionScanner.TEX_COMMENT
        };
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(org.eclipse.jface.text.source.ISourceViewer)
     */
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        
//        ContentAssistant assistant = new ContentAssistant();
        assistant = new ContentAssistant();
        assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
        
        // note that partitioning affects completions
        assistant.setContentAssistProcessor(new TexCompletionProcessor(this.editor.getDocumentModel()),
                IDocument.DEFAULT_CONTENT_TYPE);

        assistant.enableAutoActivation(TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.TEX_COMPLETION));
        assistant.enableAutoInsert(true);
        assistant.setAutoActivationDelay(TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.TEX_COMPLETION_DELAY));
        
        assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_OVERLAY);
        assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

        return assistant;
    }
    
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        PresentationReconciler reconciler = new PresentationReconciler();
        reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
        
        DefaultDamagerRepairer dr = null;
        
        dr = new DefaultDamagerRepairer(getTeXMathScanner());
        reconciler.setDamager(dr, TexPartitionScanner.TEX_MATH);
        reconciler.setRepairer(dr, TexPartitionScanner.TEX_MATH);
            
        dr = new DefaultDamagerRepairer(getTexCommentScanner());
        reconciler.setDamager(dr, TexPartitionScanner.TEX_COMMENT);
        reconciler.setRepairer(dr, TexPartitionScanner.TEX_COMMENT);
        
        dr = new DefaultDamagerRepairer(getTexScanner());
        reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
        reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
    
        return reconciler;
    }
    
    
    /**
     * Defines a default partition skanner and sets the default
     * color for it
     * @return 	a scanner to find default partitions.
     */
    protected TexScanner getTexScanner() {
        if (scanner == null) {
            scanner = new TexScanner(colorManager, editor);
            scanner.setDefaultReturnToken(
                    new Token(
                            new TextAttribute(
                                    colorManager.getColor(ColorManager.DEFAULT))));			
        }
        return scanner;
    }
    
    /**
     * Defines a math partition skanner and sets the default
     * color for it.
     * @return a scanner to detect math partitions
     */
    protected TexMathScanner getTeXMathScanner() {
        if (mathScanner == null) {
            mathScanner = new TexMathScanner(colorManager, editor);
            mathScanner.setDefaultReturnToken(
                    new Token(
                            new TextAttribute(
                                    colorManager.getColor(ColorManager.EQUATION))));
        }
        return mathScanner;
    }
    
    /**
     * Defines a comment skanner and sets the default color for it
     * @return a scanner to detect comment partitions
     */
    protected TexCommentScanner getTexCommentScanner() {
        if (commentScanner == null) {
            commentScanner = new TexCommentScanner(colorManager,editor);
            commentScanner.setDefaultReturnToken(
                    new Token(
                            new TextAttribute(
                                    colorManager.getColor(ColorManager.COMMENT))));
        }
        return commentScanner;
    }
}
