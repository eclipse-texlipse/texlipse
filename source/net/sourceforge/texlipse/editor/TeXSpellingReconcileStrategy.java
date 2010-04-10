/*
 * $Id$
 *
 * Copyright (c) 2004-2010 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.spelling.TexSpellingEngine.TexSpellingProblem;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;


/**
 * Reconcile strategy used for spell checking TeX files. Most parts copied
 * from {@link org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy}.
 * We could not extend it, because of a bug in reconcile.
 */
public class TeXSpellingReconcileStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

    /**
     * Spelling problem collector. Copied from SpellingReconcileStrategy and changed
     * it a little bit to ignore errors in TeX Commands.
     */
    public static class TeXSpellingProblemCollector implements ISpellingProblemCollector {

        /** Annotation model. */
        private IAnnotationModel fAnnotationModel;

        /** Annotations to add. */
        private Map<Annotation, Position> fAddAnnotations;
        
        /** Lock object for modifying the annotations. */
        private Object fLockObject;
        
        private IRegion[] regions;

        /**
         * Initializes this collector with the given annotation model.
         *
         * @param annotationModel the annotation model
         */
        public TeXSpellingProblemCollector(IAnnotationModel annotationModel) {
            Assert.isLegal(annotationModel != null);
            fAnnotationModel= annotationModel;
            if (fAnnotationModel instanceof ISynchronizable)
                fLockObject= ((ISynchronizable)fAnnotationModel).getLockObject();
            else
                fLockObject= fAnnotationModel;
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
         */
        public void accept(SpellingProblem problem) {
            fAddAnnotations.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
        }

        
        public void setRegions (IRegion[] _region) {
            regions = _region.clone();
        }
        
        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting()
         */
        public void beginCollecting() {
            fAddAnnotations= new HashMap<Annotation, Position>();
        }

        /**
         * @param p
         * @return true, if the Position is inside a region which was checked by
         * the spell checker
         */
        private boolean wasChecked (Position p) {
            for (IRegion r : regions) {
                if (p.getOffset() >= r.getOffset() &&
                        p.getOffset() <= r.getOffset() + r.getLength()) {
                    return true;
                }
            }
            return false;
        }
        
        
        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endCollecting()
         */
        public void endCollecting() {
            List<Annotation> toRemove= new ArrayList<Annotation>();
            
            synchronized (fLockObject) {
                Iterator<Annotation> iter= fAnnotationModel.getAnnotationIterator();
                while (iter.hasNext()) {
                    Annotation annotation= (Annotation)iter.next();
                    if (SpellingAnnotation.TYPE.equals(annotation.getType())) { 
                        final Position p = fAnnotationModel.getPosition(annotation);
                        if (wasChecked(p)) toRemove.add(annotation);
                        else {
                            //Update position (Bug 2983142)
                            SpellingAnnotation spAnn = (SpellingAnnotation) annotation;
                            TexSpellingProblem problem = (TexSpellingProblem) spAnn.getSpellingProblem();
                            problem.setOffset(p.getOffset());
                        }
                    }
                }
                Annotation[] annotationsToRemove= (Annotation[])toRemove.toArray(new Annotation[toRemove.size()]);

                if (fAnnotationModel instanceof IAnnotationModelExtension)
                    ((IAnnotationModelExtension)fAnnotationModel).replaceAnnotations(annotationsToRemove, fAddAnnotations);
                else {
                    for (int i= 0; i < annotationsToRemove.length; i++)
                        fAnnotationModel.removeAnnotation(annotationsToRemove[i]);
                    for (iter= fAddAnnotations.keySet().iterator(); iter.hasNext();) {
                        Annotation annotation= (Annotation)iter.next();
                        fAnnotationModel.addAnnotation(annotation, (Position)fAddAnnotations.get(annotation));
                    }
                }
            }

            fAddAnnotations= null;
        }
    }

    

    /** Text content type */
    private static final IContentType TEXT_CONTENT_TYPE= Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);

    /** The text editor to operate on. */
    private ISourceViewer fViewer;

    /** The document to operate on. */
    private IDocument fDocument;

    /** The progress monitor. */
    private IProgressMonitor fProgressMonitor;

    private SpellingService fSpellingService;

    private TeXSpellingProblemCollector fSpellingProblemCollector;

    /** The spelling context containing the Java source content type. */
    private SpellingContext fSpellingContext;

    /**
     * Region array, used to prevent us from creating a new array on each reconcile pass.
     * @since 3.4
     */
    private IRegion[] fRegions= new IRegion[1];


    /**
     * Creates a new comment reconcile strategy.
     *
     * @param viewer the source viewer
     * @param spellingService the spelling service to use
     */
    public TeXSpellingReconcileStrategy(ISourceViewer viewer, SpellingService spellingService) {
        Assert.isNotNull(viewer);
        Assert.isNotNull(spellingService);
        fViewer= viewer;
        fSpellingService= spellingService;
        fSpellingContext= new SpellingContext();
        fSpellingContext.setContentType(getContentType());

    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
     */
    public void initialReconcile() {
        reconcile(new Region(0, fDocument.getLength()));
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion,org.eclipse.jface.text.IRegion)
     */
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        try {
            IRegion startLineInfo= fDocument.getLineInformationOfOffset(subRegion.getOffset());
            IRegion endLineInfo= fDocument.getLineInformationOfOffset(subRegion.getOffset() + Math.max(0, subRegion.getLength() - 1));
            if (startLineInfo.getOffset() == endLineInfo.getOffset())
                subRegion= startLineInfo;
            else
                subRegion= new Region(startLineInfo.getOffset(), endLineInfo.getOffset() + endLineInfo.getLength() - startLineInfo.getOffset());
            //Check everything from startLine to the end of the document, otherwise
            //The positions of the errors are not in sync
            //subRegion= new Region(startLineInfo.getOffset(), fDocument.getLength() - startLineInfo.getOffset());

        } catch (BadLocationException e) {
            subRegion= new Region(0, fDocument.getLength());
        }
        reconcile(subRegion);
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
     */
    public void reconcile(IRegion region) {
        if (getAnnotationModel() == null || fSpellingProblemCollector == null)
            return;

        fRegions[0]= region;
        fSpellingProblemCollector.setRegions(fRegions);
        fSpellingService.check(fDocument, fRegions, fSpellingContext, fSpellingProblemCollector, fProgressMonitor);
    }

    /**
     * Returns the content type of the underlying editor input.
     *
     * @return the content type of the underlying editor input or
     *         <code>null</code> if none could be determined
     */
    protected IContentType getContentType() {
        return TEXT_CONTENT_TYPE;
    }

    /**
     * Returns the document which is spell checked.
     *
     * @return the document
     */
    protected final IDocument getDocument() {
        return fDocument;
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
     */
    public void setDocument(IDocument document) {
        fDocument= document;
        fSpellingProblemCollector= createSpellingProblemCollector();
    }

    /**
     * Creates a new spelling problem collector.
     *
     * @return the collector or <code>null</code> if none is available
     */
    protected TeXSpellingProblemCollector createSpellingProblemCollector() {
        IAnnotationModel model= getAnnotationModel();
        if (model == null)
            return null;
        return new TeXSpellingProblemCollector(model);
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
     */
    public final void setProgressMonitor(IProgressMonitor monitor) {
        fProgressMonitor= monitor;
    }

    /**
     * Returns the annotation model to be used by this reconcile strategy.
     *
     * @return the annotation model of the underlying editor input or
     *         <code>null</code> if none could be determined
     */
    protected IAnnotationModel getAnnotationModel() {
        return fViewer.getAnnotationModel();
    }
}
