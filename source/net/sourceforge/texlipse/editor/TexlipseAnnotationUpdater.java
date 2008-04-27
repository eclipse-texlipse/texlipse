/*
 * $Id$
 *
 * Copyright (c) 2007-2008 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.LatexParserUtils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.texteditor.AbstractTextEditor;

/**
 * This class implements a PostSelectionChangeListener which creates annotations
 * to highlight
 * <ul>
 * <li>associated begin or end</li>
 * <li>all references of a label</li>
 * </ul>
 * in the current document.
 * 
 * @author Boris von Loesch
 *
 */
public class TexlipseAnnotationUpdater implements ISelectionChangedListener {

    private final List<Annotation> fOldAnnotations= new LinkedList<Annotation>();
    private AbstractTextEditor fEditor;
    private Job fUpdateJob;
    private final static String ANNOTATION_TYPE = "net.sourceforge.texlipse.defAnnotation";
    private boolean fEnabled;

    /**
     * Creates a new TexlipseAnnotationUpdater and adds itself to the TexEditor via
     * <code>addPostSelectionChangedListener</code>
     * @param editor The TexEditor
     */
    public TexlipseAnnotationUpdater (AbstractTextEditor editor) {
        //Add this listener to the current editors IPostSelectionListener (lazy update)
        ((IPostSelectionProvider) editor.getSelectionProvider()).addPostSelectionChangedListener(this);
        fEditor = editor;
        fEnabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(
                TexlipseProperties.TEX_EDITOR_ANNOTATATIONS);
        
        //Add a PropertyChangeListener
        TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new  
                IPropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent event) {    
                String property = event.getProperty();
                if (TexlipseProperties.TEX_EDITOR_ANNOTATATIONS.equals(property)) {
                    boolean enabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(
                                    TexlipseProperties.TEX_EDITOR_ANNOTATATIONS);
                    fEnabled = enabled;
                }
            }
        });
    }


    public void selectionChanged(SelectionChangedEvent event) {
        update((ISourceViewer) event.getSource());
    }


    /**
     * Updates the annotations. It first checks if the current selection is
     * already annotated, if not it clears all annotations and tries to detect
     * if the current selection is part of a \[a-zA-Z]*ref, \label, \begin{...}
     * or \end{...} string. If the last is true, it searches with regular expressions
     * to find the associated part(s) and highlights them (The last uses a non UI-Job
     * which do not influence the responsiveness of the editor). 
     * 
     * @param viewer
     */
    private void update(ISourceViewer viewer) {
        final IDocument document = viewer.getDocument();
        final IAnnotationModel model = viewer.getAnnotationModel();
        ISelection selection = fEditor.getSelectionProvider().getSelection();
        
        if (testSelection(selection, model)) return;
        
        if (fUpdateJob != null) {
            fUpdateJob.cancel();
        }
        removeOldAnnotations(model);
        
        if (!fEnabled) { 
            //Feature is turned off, but we have to delete the old annotations 
            return;
        }
        
        if (selection instanceof ITextSelection) {
            try {
                //TODO Split this and create new classes for the different annotations
                final ITextSelection textSelection = (ITextSelection) selection;
                final int offset = textSelection.getOffset();
                final int lineNr = document.getLineOfOffset(offset);
                final int lineOff = document.getLineOffset(lineNr);
                final String line = document.get(lineOff, document.getLineLength(lineNr));
                IRegion r = LatexParserUtils.getCommand(line, offset - lineOff);
                if (r == null) return;
                
                final String command = line.substring(r.getOffset(), r.getOffset() + r.getLength()).trim();
                if ("\\begin".equals(command) || "\\end".equals(command)) {
                    //TODO Its maybe better/faster to use the AST here
                    IRegion r2 = LatexParserUtils.getCommandArgument(line, r.getOffset());
                    if (r2 == null) return;

                    final IRegion startRegion = new Region(lineOff + r.getOffset(), r2.getOffset() + r2.getLength() - r.getOffset() + 1);

                    final String refName = line.substring(r2.getOffset(), r2.getOffset() + r2.getLength());
                    //Create a job to update the annotations in the background
                    fUpdateJob = createMatchEnvironmentJob(document, model, offset, command, startRegion, refName);
                    fUpdateJob.setPriority(Job.DECORATE);
                    fUpdateJob.setSystem(true);
                    fUpdateJob.schedule();
                }
                else if (command.endsWith("ref") || "\\label".equals(command)) {
                    //TODO Its maybe better/faster to use the AST here
                    IRegion r2 = LatexParserUtils.getCommandArgument(line, r.getOffset());
                    if (r2 == null) return;

                    final String refName = line.substring(r2.getOffset(), r2.getOffset() + r2.getLength());
                    //Create a job to update the annotations in the background
                    fUpdateJob = createMatchReferenceJob(document, model, refName);
                    fUpdateJob.setPriority(Job.DECORATE);
                    fUpdateJob.setSystem(true);
                    fUpdateJob.schedule();
                }
            } catch (BadLocationException ex) {
                //Do not inform the user cause this is only a decorator
            }
        }
    }

    /**
     * Creates and returns a background job which searches and highlights all \label and \*ref. 
     * @param document
     * @param model
     * @param refName   The name of the reference
     * @return The job
     */
    private Job createMatchReferenceJob(final IDocument document, final IAnnotationModel model, final String refName) {
        return
            new Job("Update Annotations") {
                public IStatus run(IProgressMonitor monitor) {
                    String text = document.get();
                    String refNameRegExp = refName.replaceAll("\\*", "\\\\*");
                    final String simpleRefRegExp = "\\\\([a-zA-Z]*ref|label)\\s*\\{" + refNameRegExp + "\\}";
                    Matcher m = (Pattern.compile(simpleRefRegExp)).matcher(text);
                    while (m.find()) {
                        if (monitor.isCanceled()) return Status.CANCEL_STATUS;
                        IRegion match = LatexParserUtils.getCommand(text, m.start());
                        //Test if it is a real LaTeX command
                        if (match != null) {
                            IRegion fi = new Region(m.start(), m.end()-m.start());
                            createNewAnnotation(fi, "References", model);
                        }
                    }
                    return Status.OK_STATUS;
                }
        };
    }

    /**
     * Creates and returns a new background job which searches and highlights the matching \end or \begin environment.
     * @param document
     * @param model
     * @param offset        The offset of the selection (cursor)
     * @param command       \begin or \end
     * @param startRegion   A region which contains the command and the argument (e.g \begin{environment})
     * @param envName       The name of the environment
     * @return  The Job
     */
    private Job createMatchEnvironmentJob(final IDocument document, final IAnnotationModel model, final int offset, 
            final String command, final IRegion startRegion, final String envName) {
        return new Job("Update Annotations") {
                public IStatus run(IProgressMonitor monitor) {
                    String text = document.get();
                    boolean forward = false;
                    if ("\\begin".equals(command)) forward = true;
                    if (forward) {
                        IRegion endRegion = LatexParserUtils.findMatchingEndEnvironment(text, envName, startRegion.getOffset());
                        if (endRegion != null) {
                            createNewAnnotation(endRegion, "Environment", model);
                            createNewAnnotation(startRegion, "Environment", model);
                        }
                    } else {
                        IRegion endRegion = LatexParserUtils.findMatchingBeginEnvironment(text, envName, startRegion.getOffset());
                        if (endRegion != null) {
                            createNewAnnotation(endRegion, "Environment", model);
                            createNewAnnotation(startRegion, "Environment", model);
                        }
                    }
                    return Status.OK_STATUS;
                }
        };
    }


    /**
     * Tests if the selection is already annotated
     * @param selection current selection
     * @param model The AnnotationModel
     * @return true, if selection is already annotated
     */
    private boolean testSelection (ISelection selection, IAnnotationModel model) {
        if (selection instanceof ITextSelection) {
            final ITextSelection textSelection = (ITextSelection) selection;
            //Iterate over all existing annotations
            for (Iterator<Annotation> iter = fOldAnnotations.iterator(); iter.hasNext();) {
                Annotation anno = iter.next();
                Position p = model.getPosition(anno);
                if (p != null && p.offset <= textSelection.getOffset() && p.offset+p.length >= textSelection.getOffset()) { 
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Removes all existing annotations
     * @param model AnnotationModel
     */
    private void removeOldAnnotations(IAnnotationModel model) {

        for (Iterator<Annotation> it= fOldAnnotations.iterator(); it.hasNext();) {
            Annotation annotation= (Annotation) it.next();
            model.removeAnnotation(annotation);
        }

        fOldAnnotations.clear();
    }

    /**
     * Creates a new annotation
     * @param r The IRegion which should be highlighted
     * @param annString The name of the annotation (not important)
     * @param model The AnnotationModel
     */
    private void createNewAnnotation(IRegion r, String annString, IAnnotationModel model) {
            Annotation annotation= new Annotation(ANNOTATION_TYPE, false, annString);
            Position position= new Position(r.getOffset(), r.getLength());
            model.addAnnotation(annotation, position);
            fOldAnnotations.add(annotation);
    }
    
}