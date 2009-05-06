/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package net.sourceforge.texlipse.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;


/**
 * Reconcile strategy used for spell checking TeX files.
 */
public class TeXSpellingReconcileStrategy extends SpellingReconcileStrategy {

    
    /**
     * Spelling problem collector. Copied from SpellingReconcileStrategy and changed
     * it a little bit to ignore errors in TeX Commands.
     */
    private static class TeXSpellingProblemCollector implements ISpellingProblemCollector {

        /** Annotation model. */
        private IAnnotationModel fAnnotationModel;

        /** Annotations to add. */
        private Map<Annotation, Position> fAddAnnotations;
        
        /** Lock object for modifying the annotations. */
        private Object fLockObject;
        
        private IDocument fDocument;

        /**
         * Initializes this collector with the given annotation model.
         *
         * @param annotationModel the annotation model
         */
        public TeXSpellingProblemCollector(IAnnotationModel annotationModel, IDocument document) {
            Assert.isLegal(annotationModel != null);
            fAnnotationModel= annotationModel;
            fDocument = document;
            if (fAnnotationModel instanceof ISynchronizable)
                fLockObject= ((ISynchronizable)fAnnotationModel).getLockObject();
            else
                fLockObject= fAnnotationModel;
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
         */
        public void accept(SpellingProblem problem) {
            int start = problem.getOffset();
                try {
                    //TODO: More and better strategies
                    if (start > 0 && "\\".equals(fDocument.get(start-1, 1))) return;
                    if (start > 5 && "\\end{".equals(fDocument.get(start-5, 5))) return;
                    if (start > 7 && "\\begin{".equals(fDocument.get(start-7, 7))) return;
                } catch (BadLocationException e) {
                    //Should not happen
                }
            fAddAnnotations.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting()
         */
        public void beginCollecting() {
            fAddAnnotations= new HashMap<Annotation, Position>();
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
                    if (SpellingAnnotation.TYPE.equals(annotation.getType()))
                        toRemove.add(annotation);
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

    
    /**
     * Creates a new comment reconcile strategy.
     * 
     * @param viewer the source viewer
     * @param spellingService the spelling service to use
     */
    public TeXSpellingReconcileStrategy(ISourceViewer viewer, SpellingService spellingService) {
        super(viewer, spellingService);
    }

    /**
     * Creates a new spelling problem collector.
     * 
     * @return the collector or <code>null</code> if none is available
     */
    protected ISpellingProblemCollector createSpellingProblemCollector() {
        IAnnotationModel model= getAnnotationModel();
        if (model == null)
            return null;
        return new TeXSpellingProblemCollector(model, getDocument());
    }
}
