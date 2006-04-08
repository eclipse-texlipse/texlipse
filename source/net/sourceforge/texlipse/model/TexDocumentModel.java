/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.bibparser.BibParser;
import net.sourceforge.texlipse.editor.TexDocumentParseException;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.outline.TexOutlinePage;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.FullTexParser;
import net.sourceforge.texlipse.texparser.LatexRefExtractingParser;
import net.sourceforge.texlipse.texparser.TexParser;
import net.sourceforge.texlipse.treeview.views.TexOutlineTreeView;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.SubStatusLineManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.WorkbenchJob;


/**
 * LaTeX document model. Handles parsing of the document and updating
 * the outline, folding and content assist datastructures.
 * 
 * @author Taavi Hupponen, Oskar Ojala
 */
public class TexDocumentModel implements IDocumentListener {
   
    /**
     * Job for performing the parsing in a background thread.
     * When parsing is done schedules the PostParseJob, which
     * updates the ui stuff. waits for the PostParseJob to finish.
     * 
     * Monitor is polled often to detect cancellation.
     * 
     * @author Taavi Hupponen
     *
     */
    private class ParseJob extends Job {
        
        /**
         * @param name name of the job
         */
        public ParseJob(String name) {
            super(name);
        }
        
        /** 
         * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        protected IStatus run(IProgressMonitor monitor) {
            try {
                
                // before parsing stuff, only takes time when run the first time
                if (bibContainer == null) {
                    createReferenceContainers();
                }
                pollCancel(monitor);
                
                // parsing
                ArrayList rootNodes;
                try {
                    rootNodes = doParse(monitor);
                } catch (TexDocumentParseException e1) {
                    return Status.CANCEL_STATUS;
                }
                pollCancel(monitor);
                
                // handling of parse results
                postParseJob.setRootNodes(rootNodes);
                postParseJob.schedule();
                
                try {
                    postParseJob.join();
                } catch (InterruptedException e2) {
                    return Status.CANCEL_STATUS;
                }
                
                // return parse status etc.
                IStatus result = postParseJob.getResult();
                // parsing ok
                if (result != null && result.equals(Status.OK_STATUS)) {
                    
                    // cancel check must be here before setDirty(false)!
                    try {
                        lock.acquire();
                        pollCancel(monitor);
                        setDirty(false);
                    } finally {
                        lock.release();
                    }
                    return result;
                }
                
                // parsing not ok
                return Status.CANCEL_STATUS;
            } catch (Exception e) {
                return Status.CANCEL_STATUS;
            }
        }
    }
    
    /**
     * Job for updating the ui after parsing. Runs in the ui thread.
     * 
     * Monitor is polled often to detect cancellation.
     * 
     * @author Taavi Hupponen
     */
    private class PostParseJob extends WorkbenchJob {
        
        private ArrayList rootNodes;

        /**
         * 
         * @param name name of the job
         */
        public PostParseJob(String name) {
            super(name);
        }
        
        /**
         * @param rootNodes 
         */
        public void setRootNodes(ArrayList rootNodes) {
            this.rootNodes = rootNodes;
        }

        /**
         * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
         */
        public IStatus runInUIThread(IProgressMonitor monitor) {
            try {
                updateDocumentPositions(rootNodes, monitor);
                
                pollCancel(monitor);
                
                editor.updateCodeFolder(rootNodes, monitor);
                pollCancel(monitor);
                
                if (editor.getOutlinePage() != null) {
                    editor.getOutlinePage().update(outlineInput);
                }
                
                return Status.OK_STATUS;
            } catch (Exception e) {
                // npe when exiting eclipse and saving
                return Status.CANCEL_STATUS;
            }
        }
    }

    // B----------------------------------- mmaus
    
    /**
     * Job for performing the parsing in a background thread. When parsing is
     * done schedules the PostParseJob, which updates the input for the full
     * outline. waits for the PostParseJob to finish.
     * 
     * Monitor is polled often to detect cancellation.
     * 
     */
    private class ParseJob2 extends Job {

        private IFile fileChanged;
        private String changedInput;

        /**
         * @param name name of the job
         */
        public ParseJob2(String name) {
            super(name);
        }

        /**
         * 
         * @param fileChanged the reference to the file which changed.
         */
        public void setChangedFile(IFile fileChanged) {
            this.fileChanged = fileChanged;
        }

        /**
         * 
         * @param changedInput the input which has changed
         */
        public void setChangedInput(String changedInput) {
            this.changedInput = changedInput;
        }

        /**
         * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
         */
        protected IStatus run(IProgressMonitor monitor) {
            try {
                // parsing
                ArrayList rootNodes;
                try {
                    rootNodes = doFullParse(this.fileChanged, this.changedInput, monitor);
                } catch (TexDocumentParseException e1) {
                    return Status.CANCEL_STATUS;
                }
                if (rootNodes == null) {
                    return Status.CANCEL_STATUS;
                }
                pollCancel(monitor);

                // handling of parse results
                postParseJob2.setRootNodes(rootNodes);
                postParseJob2.schedule();

                try {
                    postParseJob2.join();
                } catch (InterruptedException e2) {
                    return Status.CANCEL_STATUS;
                }

                // return parse status etc.
                IStatus result = postParseJob2.getResult();
                // parsing ok
                if (result != null && result.equals(Status.OK_STATUS)) {
                    pollCancel(monitor);
                    return result;
                }

                // parsing not ok
                return Status.CANCEL_STATUS;
            } catch (Exception e) {
                return Status.CANCEL_STATUS;
            }
        }
    }

    /**
     * Job for updating the full outline input. Runs in the ui thread.
     * 
     * Monitor is polled often to detect cancellation.
     * 
     * @author Taavi Hupponen
     */
    private class PostParseJob2 extends WorkbenchJob {

        private ArrayList rootNodes;

        /**
         * @param name name of the job
         */
        public PostParseJob2(String name) {
            super(name);
        }

        /**
         * @param rootNodes
         */
        public void setRootNodes(ArrayList rootNodes) {
            this.rootNodes = rootNodes;
        }

        /**
         * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
         */
        public IStatus runInUIThread(IProgressMonitor monitor) {
            try {
                createOutlineInput(rootNodes, monitor);

                pollCancel(monitor);
                if (editor.getFullOutline() != null) {
                    editor.getFullOutline().update(fullOutlineInput);
                }

                return Status.OK_STATUS;
            } catch (Exception e) {
                // npe when exiting eclipse and saving
                return Status.CANCEL_STATUS;
            }
        }
    }

    private FullTexParser fullParser;
    private TexOutlineInput fullOutlineInput;
    
    // parsing jobs for the full outline.
    private ParseJob2 parseJob2;
    private PostParseJob2 postParseJob2;

    // E----------------------------------- mmaus
    
    private TexEditor editor;
    private TexParser parser;
    
    private TexOutlineInput outlineInput;
    
    private ReferenceContainer bibContainer;
    private ReferenceContainer labelContainer;
    private TexCommandContainer commandContainer;
    
    private ReferenceManager refMana;
    
    private boolean firstRun = true;

    // used to synchronize ParseJob rescheduling
    private static ILock lock = Platform.getJobManager().newLock();
    private boolean isDirty;
    
    private ParseJob parseJob;
    private PostParseJob postParseJob;
    
    // preferences
    private int parseDelay;
    private boolean autoParseEnabled;
    
    /**
     * Constructs a new document model.
     * 
     * @param editor The editor this model is associated to.
     */
    public TexDocumentModel(TexEditor editor) {
        this.editor = editor;
        this.isDirty = true;
        
        // initialize jobs
        parseJob = new ParseJob("Parsing");
        postParseJob = new PostParseJob("Updating");
        parseJob.setPriority(Job.DECORATE); 
        postParseJob.setPriority(Job.DECORATE); 
    
//      B----------------------------------- mmaus
        
        parseJob2 = new ParseJob2("Parsing");
        postParseJob2 = new PostParseJob2("Updating");
        parseJob2.setPriority(Job.DECORATE); 
        postParseJob2.setPriority(Job.DECORATE);
        
//      E----------------------------------- mmaus
        
        // get preferences
        this.autoParseEnabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.AUTO_PARSING);
        this.parseDelay = TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.AUTO_PARSING_DELAY);
    
        // add preference change listener
		TexlipsePlugin.getDefault().getPreferenceStore()
                .addPropertyChangeListener(new IPropertyChangeListener() {
			
		    public void propertyChange(PropertyChangeEvent event) {
		        String property = event.getProperty();
		        if (TexlipseProperties.AUTO_PARSING.equals(property)) {
		            autoParseEnabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.AUTO_PARSING);
		        } else if (TexlipseProperties.AUTO_PARSING_DELAY.equals(property)) {
		            parseDelay = TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.AUTO_PARSING_DELAY);
		        }
		    }	
		});	
    }



    /**
     * Initializes the model. Should be called immediately after constructing
     * an instance, otherwise parsing may fail.
     */
    public void initializeModel() {
        MarkerHandler.getInstance().clearErrorMarkers(editor);
        MarkerHandler.getInstance().clearTaskMarkers(editor);
        createReferenceContainers();
    }

    
    
    /**
     * Cancels possibly running parseJob and schedules it to run again 
     * immediately.
     * 
     * Called when new outline is created, so it is quite likely that there
     * is no parseJob running.
     * 
     * If parseJob were running we could maybe use isDirty to figure out
     * if it would be smarter to wait for the running parseJob.
     */
    public void updateNow() {
       parseJob.cancel();
       parseJob.schedule();
    }
    
  
    
    /**
     * Called from TexEditor.getAdapter(). If uptodate outline input is 
     * found the outline is updated with it.
     * 
     * If current outline is not uptodate, parsing job is started and 
     * eventually outline is updated with fresh input.
     */
    public void updateOutline() {
        if (!isDirty()) {
            this.editor.getOutlinePage().update(this.outlineInput);
        } else {
            this.updateNow();        
        }
    }

//  B----------------------------------- mmaus
    
    /**
     * Cancels possibly running parseJob and schedules it to run again 
     * immediately.
     * 
     * Called when new outline is created, so it is quite likely that there
     * is no parseJob running.
     * 
     * If parseJob were running we could maybe use isDirty to figure out
     * if it would be smarter to wait for the running parseJob.
     */
    public void updateFullOutline() {
        parseJob2.cancel();
        parseJob2.schedule();
    }

// E----------------------------------- mmaus

    /**
     * Returns the reference (label and BibTeX) for this model
     * (ie. project).
     * 
     * @return Returns the refMana.
     */
    public ReferenceManager getRefMana() {
        if (refMana == null) {
            refMana = new ReferenceManager(bibContainer,
                    labelContainer,
                    commandContainer);
        }
        return refMana;
    }


    /**
     * Returns whether current OutlineInput is dirty, i.e. if the
     * document has been changed after latest parsing.
     * 
     * @return true if document is dirty
     */
    public synchronized boolean isDirty() {
        return this.isDirty;
    }



    
    
    /** 
     * Does nothing atm.
     * 
     * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
     */
    public void documentAboutToBeChanged(DocumentEvent event) {
    
    }



    /**
     * Called when document changes. Marks the document dirty and
     * schedules the parsing job.
     * 
     * When we return from here this.isDirty must remain true
     * until new parseJob has finished.
     * 
     * If the previous parseJob is not cancelled immediately 
     * (parseJob.cancel() returns false) a smarter way to 
     * calculate the next scheduling delay could be used. 
     * However we should not spend much time in documentChanged()  
     */
    public void documentChanged(DocumentEvent event) {
    
        // set isDirty true and prevent possibly running parseJob from
        // changing it back to false
        // order of acquire, cancel and setDirty matters!
        try {
            lock.acquire();
            parseJob.cancel();
            this.setDirty(true);
        } finally {
            lock.release();
        }
        // if parseJob was running, now it is either cancelled or it will 
        // be before it tries setDirty(false)
    
        // inform outline that model is dirty
        TexOutlinePage outline = editor.getOutlinePage();
        if (outline != null) {
        	editor.getOutlinePage().modelGotDirty();
        }
    
//      B----------------------------------- mmaus
        TexOutlineTreeView fullOutline = editor.getFullOutline();
        if(fullOutline != null) {
            fullOutline.modelGotDirty();
        }
//      E----------------------------------- mmaus
        
        // reschedule parsing with delay
        if (autoParseEnabled) {
            parseJob.schedule(parseDelay);
            
//          B----------------------------------- mmaus
            parseJob2.setChangedFile(getCurrentProject().getFile(this.editor.getEditorInput().getName()));
            parseJob2.setChangedInput(event.getDocument().get());
            parseJob2.schedule(parseDelay);
//          E----------------------------------- mmaus
        }
    }

    /**
     * Parses the LaTeX-document and adds error markers if there were any
     * errors. Throws <code>TexDocumentParseException</code> if there were
     * fatal parse errors that prohibit building an outline.
     * @param monitor
     * 
     * @throws TexDocumentParseException
     */
    private ArrayList doParse(IProgressMonitor monitor) throws TexDocumentParseException {
        
        if (this.parser == null) {
            this.parser = new TexParser(this.editor.getDocumentProvider().getDocument(this.editor.getEditorInput()));
        }
        
        try {
            this.parser.parseDocument(labelContainer, bibContainer);
        } catch (IOException e) {
            TexlipsePlugin.log("Can't read file.", e);
            throw new TexDocumentParseException(e);
        }
        pollCancel(monitor);
        
        ArrayList errors = parser.getErrors();
        List tasks = parser.getTasks();
        MarkerHandler marker = MarkerHandler.getInstance();
        // somewhat inelegantly ensures that errors marked in createProjectDatastructs()
        // aren't removed immediately
        if (!firstRun) {
            marker.clearErrorMarkers(editor);
            marker.clearTaskMarkers(editor);
        } else {
            firstRun = false;
        }
        if (errors.size() > 0) {
            marker.createErrorMarkers(editor, errors);
        }
        if (tasks.size() > 0) {
            marker.createTaskMarkers(editor, tasks);
        }
        if (parser.isFatalErrors()) {
            throw new TexDocumentParseException("Fatal errors in file, parsing aborted.");
        }
        
        updateReferences(monitor);
        
        ArrayList bibErrors = parser.getCites();
        ArrayList refErrors = parser.getRefs();
        if (bibErrors.size() > 0) {
            marker.createReferencingErrorMarkers(editor, bibErrors);
        }
        if (refErrors.size() > 0) {
            labelContainer.removeFalseEntries(refErrors);
            marker.createReferencingErrorMarkers(editor, refErrors);
        }
        
        return this.parser.getOutlineTree();
    }

//  B----------------------------------- mmaus
    
    /**
     * Parses the LaTeX-document and adds error markers if there were any
     * errors. Throws <code>TexDocumentParseException</code> if there were
     * fatal parse errors that prohibit building an outline.
     * @param monitor
     * @throws TexDocumentParseException 
     */
    private ArrayList doFullParse(IFile changedFile, String inputChanged,
            IProgressMonitor monitor) throws TexDocumentParseException {
        // create the full parser if not available yet. initialize it with the
        // project main file and a handle to the current project.

        if (this.fullParser == null) {
            try {
                IFile mainFile = TexlipseProperties
                        .getProjectSourceFile(getCurrentProject());
                this.fullParser = new FullTexParser(getCurrentProject(),
                        mainFile);
            } catch (IllegalArgumentException e) {
                TexlipsePlugin.log("Project main file not set.", e);
            }
        }

        // if the actual document has changed
        if (changedFile != null) {
            this.fullParser.setChangedFile(changedFile);
        }
        if (inputChanged != null) {
            this.fullParser.setChangedInput(inputChanged);
        }

        // parse
        try {
            this.fullParser.parseDocument(labelContainer, bibContainer);
        } catch (IOException e) {
            TexlipsePlugin.log("Can't read file.", e);
            throw new TexDocumentParseException(e);
        }
        pollCancel(monitor);

        // error hadling
        ArrayList errors = fullParser.getErrors();
        MarkerHandler marker = MarkerHandler.getInstance();
        if (errors != null && errors.size() > 0) {
            marker.createErrorMarkers(editor, errors);
        }

        if (fullParser.isFatalErrors()) {
            throw new TexDocumentParseException(
                    "Fatal errors in file, parsing aborted.");
        }

        return this.fullParser.getOutlineTree();
    }
    
//  E----------------------------------- mmaus
    
    /**
     * Traverses the OutlineNode tree and adds a Position for each
     * node to Document.
     * 
     * Also adds the nodes to type lists of the OutlineInput and 
     * calculates the tree depth.
     * 
     * Old Positions are removed before adding new ones.
     * 
     * @param rootNodes
     * @param monitor monitor for the job calling this method
     */
    private void updateDocumentPositions(ArrayList rootNodes, IProgressMonitor monitor) {
        TexOutlineInput newOutlineInput = new TexOutlineInput(rootNodes);
        
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        
        // remove previous positions
        try {
        	document.removePositionCategory("__outline");
        } catch (BadPositionCategoryException bpce) {
            // do nothing, the category will be added again next, it does not exists the first time
        }
        
        document.addPositionCategory("__outline");
        pollCancel(monitor);
        
        // add new positions for nodes and their children
        int maxDepth = 0;
        for (Iterator iter = rootNodes.iterator(); iter.hasNext(); ) {
            OutlineNode node = (OutlineNode) iter.next();
            int localDepth = addNodePosition(node, document, 0, newOutlineInput); 
            
            if (localDepth > maxDepth) {
                maxDepth = localDepth;
            }
            pollCancel(monitor);
        }
        pollCancel(monitor);

        // set the new outline input
        newOutlineInput.setTreeDepth(maxDepth);
        this.outlineInput = newOutlineInput;
    }
    
    /** 
     * Handles a single node when traversing the outline tree. Used
     * recursively.
     * 
     * @param node
     * @param document
     * @param parentDepth
     * @param newOutlineInput
     * @return
     */
    private int addNodePosition(OutlineNode node, IDocument document,
            int parentDepth, TexOutlineInput newOutlineInput) {        
        
        // add the Document position
        int beginOffset = 0;
        int length = 0;
        Position position = null;
        
        try {
            beginOffset = document.getLineOffset(node.getBeginLine() - 1);
            if (node.getEndLine() -1 == document.getNumberOfLines())
            	length = document.getLength() - beginOffset;
            else
            	length =  document.getLineOffset(node.getEndLine() - 1) - beginOffset;
            position = new Position(beginOffset, length);
            document.addPosition("__outline", position);
        } catch (BadLocationException bpe) {
            throw new OperationCanceledException();
        } catch (BadPositionCategoryException bpce) {
            throw new OperationCanceledException();
        }
        node.setPosition(position);
        
        // add node to outline input
        newOutlineInput.addNode(node);
        
        // iterate through the children
        List children = node.getChildren();
        int maxDepth = parentDepth + 1;
        if (children != null) {
            for (Iterator iter = children.iterator(); iter.hasNext();) {
                int localDepth = addNodePosition((OutlineNode) iter.next(), document, parentDepth + 1, newOutlineInput);
                if (localDepth > maxDepth) {
                    maxDepth = localDepth;
                }
            }
        }
        return maxDepth;
    }
    
//  B----------------------------------- mmaus
    
    /**
     * Traverses the OutlineNode tree.
     * 
     * Adds the nodes to type lists of the OutlineInput and 
     * calculates the tree depth.
     * 
     * @param rootNodes
     * @param monitor monitor for the job calling this method
     */
    private void createOutlineInput(ArrayList rootNodes, IProgressMonitor monitor) {
        TexOutlineInput newOutlineInput = new TexOutlineInput(rootNodes);

        // set the depth and add nodes to the tree
        int maxDepth = 0;
        for (Iterator iter = rootNodes.iterator(); iter.hasNext();) {
            OutlineNode node = (OutlineNode) iter.next();
            int localDepth = handleNode(node, 0, newOutlineInput);

            if (localDepth > maxDepth) {
                maxDepth = localDepth;
            }
            pollCancel(monitor);
        }
        pollCancel(monitor);

        // set the new outline input
        newOutlineInput.setTreeDepth(maxDepth);
        this.fullOutlineInput = newOutlineInput;
    }
    
    /**
     * Adds a node to the outline input. Calculates the depth of the tree. Used
     * recursively.
     * 
     * @param node the current node.
     * @param parentDepth the depth to the parent.
     * @param newOutlineInput the input for the full outline.
     * @return
     */
    private int handleNode(OutlineNode node, int parentDepth, TexOutlineInput newOutlineInput) {        
        
        // add node to outline input
        newOutlineInput.addNode(node);
        
        // iterate through the children
        List children = node.getChildren();
        int maxDepth = parentDepth + 1;
        if (children != null) {
            for (Iterator iter = children.iterator(); iter.hasNext();) {
                int localDepth = handleNode((OutlineNode) iter.next(),
                        parentDepth + 1, newOutlineInput);
                if (localDepth > maxDepth) {
                    maxDepth = localDepth;
                }
            }
        }
        return maxDepth;
    }
    
//  E----------------------------------- mmaus
    
    /**
     * Updates the references and project data based on the data in the
     * parsed document.
     * 
     * @param monitor Progress monitor
     */
    private void updateReferences(IProgressMonitor monitor) {
        this.updateLabels(parser.getLabels());
        this.updateCommands(parser.getCommands());
        
        pollCancel(monitor);
        
        String[] bibs = parser.getBibs();
        if (bibs != null) {
            this.updateBibs(bibs, ((FileEditorInput)editor.getEditorInput()).getFile());
        }
        // After here we just store those fun properties...
        
        pollCancel(monitor);
        
        IProject project = getCurrentProject();
        IFile cFile = ((FileEditorInput) editor.getEditorInput()).getFile();
        //Only update Preamble, Bibstyle if main Document
        if (cFile.equals(TexlipseProperties.getProjectSourceFile(project))) {
            String preamble = parser.getPreamble();
            if (preamble != null) {
                TexlipseProperties.setSessionProperty(project, 
                        TexlipseProperties.PREAMBLE_PROPERTY, 
                        preamble);
            }
            String bibstyle = parser.getBibstyle();
            if (bibstyle != null) {
                String oldStyle = (String) TexlipseProperties.getSessionProperty(project,
                        TexlipseProperties.BIBSTYLE_PROPERTY);

                if (oldStyle == null || !bibstyle.equals(oldStyle)) {
                    TexlipseProperties.setSessionProperty(project, 
                            TexlipseProperties.BIBSTYLE_PROPERTY, 
                            bibstyle);

                    // schedule running bibtex on the next build
                    TexlipseProperties.setSessionProperty(project, 
                            TexlipseProperties.BIBFILES_CHANGED, 
                            new Boolean(true));
                }
            }
        }
    }
    
    /**
     * Updates completions for the BibTeX -data
     * 
     * @param bibNames Names of the BibTeX -files that the document uses
     * @param resource The resource of the document
     */
    private void updateBibs(String[] bibNames, IResource resource) {        
        IProject project = getCurrentProject();
        
        for (int i=0; i < bibNames.length; i++)
            bibNames[i] += ".bib";
        
        if (bibContainer.checkFreshness(bibNames)) {
            return;
        }
        
        TexlipseProperties.setSessionProperty(project,
                TexlipseProperties.BIBFILE_PROPERTY,
                bibNames);
        
        LinkedList newBibs = bibContainer.updateBibHash(bibNames);

        IPath path = resource.getFullPath().removeFirstSegments(1).removeLastSegments(1);        
        if (!path.isEmpty())
            path = path.addTrailingSeparator();
        
        for (Iterator iter = newBibs.iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            IResource res = project.findMember(path + name);
            if (res != null) {
                BibParser parser = new BibParser(res.getLocation().toOSString());
                try {
                    ArrayList bibEntriesList = parser.getEntries();
                    if (bibEntriesList != null && bibEntriesList.size() > 0) {
                        bibContainer.addRefSource(path + name, bibEntriesList);
                    } else {
                        MarkerHandler marker = MarkerHandler.getInstance();
                        marker.addFatalError(editor, "The BibTeX file " + res.getFullPath() + " contains fatal errors, parsing aborted.");
                        continue;
                    }
                } catch (IOException ioe) {
                    TexlipsePlugin.log("Can't read BibTeX file " + res.getFullPath(), ioe);
                }
            }
        }
        bibContainer.organize();
    }
    
    /**
     * Updates the labels.
     * @param labels
     */
    private void updateLabels(ArrayList labels) {
        IResource resource = ((FileEditorInput)editor.getEditorInput()).getFile();
        labelContainer.addRefSource(resource.getProjectRelativePath().toString(), labels);
        labelContainer.organize();
    }
    
    /**
     * Updates the commands.
     * @param commands
     */
    private void updateCommands(ArrayList commands) {
        IResource resource = ((FileEditorInput)editor.getEditorInput()).getFile();
        commandContainer.addRefSource(resource.getProjectRelativePath().toString(), commands);
        commandContainer.organize();
    }
    
    /**
     * Creates the reference containers.
     *
     */
    private void createReferenceContainers() {
        boolean parseAll = false;
        IProject project = getCurrentProject();
        ReferenceContainer bibCon = (ReferenceContainer) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.BIBCONTAINER_PROPERTY);
        if (bibCon == null) {
            bibContainer = new ReferenceContainer();
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.BIBCONTAINER_PROPERTY,
                    bibContainer);
            parseAll = true;
        } else {
            bibContainer = bibCon;
        }
        ReferenceContainer labCon = (ReferenceContainer) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.LABELCONTAINER_PROPERTY);
        if (labCon == null) {
            labelContainer = new ReferenceContainer();
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.LABELCONTAINER_PROPERTY,
                    labelContainer);
            parseAll = true;
        } else {
            labelContainer = labCon;
        }
        TexCommandContainer comCon = (TexCommandContainer) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.COMCONTAINER_PROPERTY);
        if (comCon == null) {
            commandContainer = new TexCommandContainer();
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.COMCONTAINER_PROPERTY,
                    commandContainer);
            parseAll = true;
        } else {
            commandContainer = comCon;
        }
        
        if (parseAll) {
            createProjectDatastructs(project);
        }
    }
    
    /**
     * Creates all the project data structures. These include the reference
     * completions (BibTeX and label), command completions, the preamble,
     * the BibTeX style.
     * 
     * @param project The current project
     */
    private void createProjectDatastructs(IProject project) {
        IResource resource = ((FileEditorInput)editor.getEditorInput()).getFile();
        
        IResource[] files = TexlipseProperties.getAllProjectFiles(project);        
        
        if (files != null) {
            IFile mainFile = TexlipseProperties.getProjectSourceFile(project);

            for (int i = 0; i < files.length; i++) {
                IPath path = files[i].getFullPath();
                String ext = files[i].getFileExtension();
				// here are the file types we want to parse
                if ("tex".equals(ext) || "ltx".equals(ext) || "sty".equals(ext)) {
                    try {
                        String input = TexlipseProperties.getFileContents(files[i]);
                        LatexRefExtractingParser lrep = new LatexRefExtractingParser();
                        lrep.parse(input);
                        if (lrep.isFatalErrors()) {
                            MarkerHandler marker = MarkerHandler.getInstance();
                            marker.addFatalError(editor, "The file " + files[i].getFullPath() + " contains fatal errors, parsing aborted.");
                            continue;
                        }
                        ArrayList labels = lrep.getLabels();
                        if (labels.size() > 0) {
                            labelContainer.addRefSource(files[i].getProjectRelativePath().toString(), labels);
                        }
                        ArrayList commands = lrep.getCommands();
                        if (commands.size() > 0) {
                            commandContainer.addRefSource(files[i].getProjectRelativePath().toString(), commands);
                        }
                        //Only update Preamble, Bibstyle if main Document
                        if (files[i].equals(mainFile)) {
                            String[] bibs = lrep.getBibs();
                            if (bibs != null)
                                this.updateBibs(bibs, files[i]);

                            String preamble = lrep.getPreamble();
                            if (preamble != null) {
                                TexlipseProperties.setSessionProperty(project, 
                                        TexlipseProperties.PREAMBLE_PROPERTY,
                                        preamble);
                            }

                            String bibstyle = lrep.getBibstyle();
                            if (bibstyle != null)
                                TexlipseProperties.setSessionProperty(project, 
                                        TexlipseProperties.BIBSTYLE_PROPERTY,
                                        bibstyle);
                        }
                    } catch (IOException ioe) {
                        TexlipsePlugin.log("Unable to open file " + files[i].getFullPath() + " for parsing", ioe);
                    }
                }
            }
            // save time by doing this last
            labelContainer.organize();
            commandContainer.organize();
        }
    }
    
    /**
     * @return the current project
     */
    private IProject getCurrentProject() {
        return ((FileEditorInput)editor.getEditorInput()).getFile().getProject();
    }

    /**
     * Marks the current OutlineInput dirty.
     * @param dirty true if OutlineInput is marked dirty
     */
    private synchronized void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    /**
     * Cancels a job by throwing OperationCanceledException.
     * 
     * Used by inner class jobs of this class.
     * 
     * @param monitor releated to the Job polling the cancel state
     */
    private void pollCancel(IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }
    
//  B----------------------------------- mmaus
    
    /**
     * Write a message on the status line.
     * @param msg the message.
     */
    public void setStatusLineErrorMessage(String msg){
        SubStatusLineManager slm = 
            (SubStatusLineManager) editor.getEditorSite().getActionBars().getStatusLineManager();
        slm.setVisible(true);
        slm.setErrorMessage(msg);
    }
    
    /**
     * clean the status line
     *
     */
    public void removeStatusLineErrorMessage(){
        SubStatusLineManager slm = 
            (SubStatusLineManager) editor.getEditorSite().getActionBars().getStatusLineManager();
        slm.setVisible(false);
    }
    
//  E----------------------------------- mmaus
    
}
