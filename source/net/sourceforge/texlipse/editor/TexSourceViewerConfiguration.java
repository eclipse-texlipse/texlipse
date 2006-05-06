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
import net.sourceforge.texlipse.editor.hover.TexHover;
import net.sourceforge.texlipse.editor.scanner.TexCommentScanner;
import net.sourceforge.texlipse.editor.scanner.TexMathScanner;
import net.sourceforge.texlipse.editor.scanner.TexScanner;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoIndentStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


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
    private RuleBasedScanner verbatimScanner;
    private ColorManager colorManager;
    private TexAnnotationHover annotationHover;
    private ContentAssistant assistant;
    private TexHover textHover;

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
        //TexCompletionProcessor tcp = new TexCompletionProcessor(this.editor.getDocumentModel());
        TexCompletionProcessor tcp = new TexCompletionProcessor(this.editor.getDocumentModel(), sourceViewer);
        TexMathCompletionProcessor tmcp = new TexMathCompletionProcessor(this.editor.getDocumentModel(), sourceViewer);
//        assistant.setContentAssistProcessor(new TexCompletionProcessor(this.editor.getDocumentModel()),
//                IDocument.DEFAULT_CONTENT_TYPE);

        assistant.setContentAssistProcessor(tcp, IDocument.DEFAULT_CONTENT_TYPE);
        //assistant.setContentAssistProcessor(tcp, TexPartitionScanner.TEX_MATH);
        assistant.setContentAssistProcessor(tmcp, TexPartitionScanner.TEX_MATH);
        assistant.setContentAssistProcessor(tcp, TexPartitionScanner.TEX_CURLY_BRACKETS);
        assistant.setContentAssistProcessor(tcp, TexPartitionScanner.TEX_SQUARE_BRACKETS);

        
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
        
        dr = new DefaultDamagerRepairer(getTexVerbatimScanner());
        reconciler.setDamager(dr, TexPartitionScanner.TEX_VERBATIM);
        reconciler.setRepairer(dr, TexPartitionScanner.TEX_VERBATIM);

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
                                    colorManager.getColor(ColorManager.DEFAULT),
                                    null,
                                    colorManager.getStyle(ColorManager.DEFAULT_STYLE))));
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
                                    colorManager.getColor(ColorManager.EQUATION),
                                    null,
                                    colorManager.getStyle(ColorManager.EQUATION_STYLE))));
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
                                    colorManager.getColor(ColorManager.COMMENT),
                                    null,
                                    colorManager.getStyle(ColorManager.COMMENT_STYLE))));
        }
        return commentScanner;
    }
    
    /**
     * Defines a verbatim scanner and sets the default color for it
     * @return a scanner to detect varbatim style partitions
     */
    protected RuleBasedScanner getTexVerbatimScanner() {
        if (verbatimScanner == null) {
            //We need no rules, because the user can write everything inside a verbatim env
            verbatimScanner = new RuleBasedScanner ();
            verbatimScanner.setDefaultReturnToken(
                    new Token(
                            new TextAttribute(
                                    colorManager.getColor(ColorManager.VERBATIM),
                                    null,
                                    colorManager.getStyle(ColorManager.VERBATIM_STYLE))));
        }
        return verbatimScanner;
    }

    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        if (textHover == null) {
            textHover = new TexHover(editor);
        }
        return textHover;
    }
    
    /**
     * Displays all commands in bold and all groups in italic
     */
    private static final DefaultInformationControl.IInformationPresenter
    presenter = new DefaultInformationControl.IInformationPresenter() {
        public String updatePresentation(Display display, String infoText,
                TextPresentation presentation, int maxWidth, int maxHeight) {
            int cstart = -1;
            int gstart = -1;
            
            // Loop over all characters of information text
            for (int i = 0; i < infoText.length(); i++) {
                switch (infoText.charAt(i)) {
                case '{':
                    // if we get \foo\{ or \{, then a group doesn't start
                    if (cstart >= 0 && infoText.charAt(i-1) != '\\') {
                        boldRange(cstart, i - cstart, presentation, false);
                        cstart = -1;
                        gstart = i;
                    } else if (cstart < 0) {
                        gstart = i;
                    }
                    break;
                case '}':
                    // if we get \} then it doesn't mean that a group ends
                    if (cstart >= 0 && infoText.charAt(i-1) != '\\') {
                        boldRange(cstart, i - cstart, presentation, true);
                        cstart = -1;
                        if (gstart >= 0) {
                            italicizeRange(gstart, cstart - gstart + 1, presentation);
                            gstart = -1;
                        }
                    } else if (gstart >= 0) {
                        italicizeRange(gstart, i - gstart + 1, presentation);
                        gstart = -1;
                    }
                    break;
                case '\\':
                    if (cstart < 0 && gstart < 0) {
                        cstart = i;
                    }
                    break;
                case '\n':
                case '\r':
                case '\t':
                case ' ':
                    if (cstart >= 0) {
                        if (gstart >= 0) {
                            italicizeRange(gstart, cstart - gstart, presentation);
                            boldRange(cstart, i - cstart, presentation, true);
                            gstart = i;
                        } else {
                            boldRange(cstart, i - cstart, presentation, false);
                        }
                        cstart = -1;
                    }
                    break;
                }
            }
            // check if we want to bold to the end of string
            if (gstart >= 0) {
                italicizeRange(gstart, infoText.length() - gstart, presentation);
            }
            if (cstart >= 0) {
                boldRange(cstart, infoText.length() - cstart, presentation, false);
            }
            // Return the information text
            return infoText;
        }
        private void boldRange(int start, int length, TextPresentation presentation, boolean doItalic) {
            // We have found a tag and create a new style range
            int fontStyle = doItalic ? (SWT.BOLD | SWT.ITALIC) : SWT.BOLD;
            StyleRange range = new StyleRange(start, length, null, null, fontStyle);
            
            // Add this style range to the presentation
            presentation.addStyleRange(range);
        }
        private void italicizeRange(int start, int length, TextPresentation presentation) {
            StyleRange range = new StyleRange(start, length, null, null, SWT.ITALIC);
            presentation.addStyleRange(range);
        }
        
    };
    
    public IInformationControlCreator getInformationControlCreator
    (ISourceViewer sourceViewer) {
        return new IInformationControlCreator() {
            public IInformationControl createInformationControl(Shell parent) {
                return new DefaultInformationControl(parent, presenter);
            }
        };
    }
    
}
