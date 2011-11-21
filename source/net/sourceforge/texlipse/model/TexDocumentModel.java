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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.bibparser.BibParser;
import net.sourceforge.texlipse.editor.TexDocumentParseException;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.outline.TexOutlinePage;
import net.sourceforge.texlipse.outline.TexProjectOutline;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.LatexRefExtractingParser;
import net.sourceforge.texlipse.texparser.TexParser;
import net.sourceforge.texlipse.treeview.views.TexOutlineTreeView;
import net.sourceforge.texlipse.builder.KpsewhichRunner;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.WorkbenchJob;


/**
 * LaTeX document model. Handles parsing of the document and updating
 * the outline, folding and content assist datastructures.
 * 
 * @author Taavi Hupponen
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public class TexDocumentModel implements IDocumentListener {

    public static final String PARSER_FAMILY = "TexDocument Parser";
   
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
                ArrayList<OutlineNode> rootNodes;
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

        @Override
        public boolean belongsTo(Object family) {
            return family.equals(PARSER_FAMILY);
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
        
        private ArrayList<OutlineNode> rootNodes;
        private List<OutlineNode> fullOutlineNodes;

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
        public void setRootNodes(ArrayList<OutlineNode> rootNodes) {
            this.rootNodes = rootNodes;
        }
        
        /**
         * @param rootNodes
         */
        public void setFONodes(List<OutlineNode> rootNodes) {
            this.fullOutlineNodes = rootNodes;
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
                
                //Update FullOutline
                if (fullOutlineNodes != null) {

                    pollCancel(monitor);
                    if (editor.getFullOutline() != null) {
                        //createOutlineInput(fullOutlineNodes, monitor);
                        editor.getFullOutline().update(new TexOutlineInput(new ArrayList<OutlineNode>(fullOutlineNodes)));
                    }
                }

                return Status.OK_STATUS;
            } catch (Exception e) {
                // npe when exiting eclipse and saving
                return Status.CANCEL_STATUS;
            }
        }
    }


    private TexEditor editor;
    private TexParser parser;
    private TexProjectOutline projectOutline;
    
    private TexOutlineInput outlineInput;
    
    private ReferenceContainer bibContainer;
    private ReferenceContainer labelContainer;
    private TexCommandContainer commandContainer;
    
    private ReferenceManager refMana;
    
    private boolean firstRun = true;

    // used to synchronize ParseJob rescheduling
    private static ILock lock = Job.getJobManager().newLock();
    private boolean isDirty;
    
    private ParseJob parseJob;
    private PostParseJob postParseJob;
    
    // preferences
    private int parseDelay;
    private boolean autoParseEnabled;
    private boolean sectionCheckEnabled;
    
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
        // get preferences
        this.parseDelay = TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.AUTO_PARSING_DELAY);
        this.autoParseEnabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.AUTO_PARSING);
        this.sectionCheckEnabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.SECTION_CHECK);
        
        // add preference change listener
		TexlipsePlugin.getDefault().getPreferenceStore()
                .addPropertyChangeListener(new IPropertyChangeListener() {
			
		    public void propertyChange(PropertyChangeEvent event) {
		        String property = event.getProperty();
		        if (TexlipseProperties.AUTO_PARSING.equals(property)) {
		            autoParseEnabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.AUTO_PARSING);
		        } else if (TexlipseProperties.AUTO_PARSING_DELAY.equals(property)) {
		            parseDelay = TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.AUTO_PARSING_DELAY);
		        } else if (TexlipseProperties.SECTION_CHECK.equals(property)) {
                    sectionCheckEnabled = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.SECTION_CHECK);
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

    /**
     * Returns the reference (label and BibTeX) for this model
     * (ie. project).
     * 
     * @return Returns the refMana.
     */
    public ReferenceManager getRefMana() {
        if (refMana == null) {
            if (bibContainer == null) createReferenceContainers();
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
    
        TexOutlineTreeView fullOutline = editor.getFullOutline();
        if (fullOutline != null) {
            fullOutline.modelGotDirty();
        }
        
        // reschedule parsing with delay
        if (autoParseEnabled) {
            parseJob.schedule(parseDelay);
        }
    }

    
    /**
     * Creates if not exist the ProjectOutline
     *
     */
    private void createProjectOutline() {
        IProject project = getCurrentProject();
        if (project == null) return;
        Object projectSessionOutLine = TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_PROJECT_FULLOUTLINE);
        if (projectSessionOutLine != null)
            projectOutline = (TexProjectOutline) projectSessionOutLine;
        else {
            projectOutline = new TexProjectOutline(getCurrentProject());
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.SESSION_PROJECT_FULLOUTLINE,
                    projectOutline);
        }
    }
    
    /**
     * Parses the LaTeX-document and adds error markers if there were any
     * errors. Throws <code>TexDocumentParseException</code> if there were
     * fatal parse errors that prohibit building an outline.
     * 
     * @param monitor
     * 
     * @throws TexDocumentParseException
     */
    private ArrayList<OutlineNode> doParse(IProgressMonitor monitor) throws TexDocumentParseException {
        
        if (this.parser == null) {
            this.parser = new TexParser(editor.getDocumentProvider().getDocument(editor.getEditorInput()));
        }
        if (projectOutline == null) {
            createProjectOutline();
        }
        
        try {
            parser.parseDocument(sectionCheckEnabled);
        } catch (IOException e) {
            TexlipsePlugin.log("Can't read file.", e);
            throw new TexDocumentParseException(e);
        }
        pollCancel(monitor);

        List<ParseErrorMessage> errors = parser.getErrors();
        List<ParseErrorMessage> tasks = parser.getTasks();
        MarkerHandler marker = MarkerHandler.getInstance();
        
        // somewhat inelegantly ensures that errors marked in createProjectDatastructs()
        // aren't removed immediately
        if (!firstRun) {
            marker.clearErrorMarkers(editor);
            marker.clearTaskMarkers(editor);
        } else {
            firstRun = false;
        }


        if (editor.getProject() != null && editor.getFullOutline() != null) {
            IResource res = (IResource) editor.getEditorInput().getAdapter(IResource.class);
            String fileName = res.getProjectRelativePath().toString();
            projectOutline.addOutline(parser.getOutlineTree(), fileName);
            
            List<OutlineNode> fo = projectOutline.getFullOutline();
            postParseJob.setFONodes(fo);
        } else {
            postParseJob.setFONodes(null);
        }
        pollCancel(monitor);
        
        processIncludes(parser.getInputs(), editor.getEditorInput());
        
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
        
        List<DocumentReference> cites = parser.getCites();
        List<DocumentReference> bibErrors = null;
        for (DocumentReference cite : cites) {
        	if (!bibContainer.binTest(cite.getKey())) {
        		if (bibErrors == null) bibErrors = new ArrayList<DocumentReference>();
        		bibErrors.add(cite);
        	}
		}
        if (bibErrors != null) {
        	marker.createReferencingErrorMarkers(editor, bibErrors);
        }

        List<DocumentReference> refs = parser.getRefs();
        List<DocumentReference> refErrors = null;
        for (DocumentReference ref : refs) {
			if (!labelContainer.binTest(ref.getKey())) {
				if (refErrors == null) refErrors = new ArrayList<DocumentReference>();
				refErrors.add(ref);
			}				
		}
        if (refErrors != null) {
        	marker.createReferencingErrorMarkers(editor, refErrors);
        }
        
        return this.parser.getOutlineTree();
    }

    
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
    private void updateDocumentPositions(List<OutlineNode> rootNodes, IProgressMonitor monitor) {
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
        for (Iterator<OutlineNode> iter = rootNodes.iterator(); iter.hasNext(); ) {
            OutlineNode node = iter.next();
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
        List<OutlineNode> children = node.getChildren();
        int maxDepth = parentDepth + 1;
        if (children != null) {
            for (Iterator<OutlineNode> iter = children.iterator(); iter.hasNext();) {
                int localDepth = addNodePosition(iter.next(), document, parentDepth + 1, newOutlineInput);
                if (localDepth > maxDepth) {
                    maxDepth = localDepth;
                }
            }
        }
        return maxDepth;
    }

    /**
     * Updates the settings for the BibLaTeX package. If this is not the initial run,
     * checks if settings have changed from previous parse job and, if applicable, sets a
     * notification flag for the builder that something has changed.
     *
     * @param project the current project
     * @param biblatexMode true, if biblatex package was found by parser
     * @param biblatexBackend database backend detected by parser, or null
     * @param init whether this is the initial run
     */
    private void updateBiblatex(IProject project, boolean biblatexMode,
            String biblatexBackend, boolean init) {
        if (!init) {
            Boolean oldBLMode = (Boolean) TexlipseProperties.getSessionProperty(project,
                    TexlipseProperties.SESSION_BIBLATEXMODE_PROPERTY);
            String oldBackend = (String) TexlipseProperties.getSessionProperty(project,
                    TexlipseProperties.SESSION_BIBLATEXBACKEND_PROPERTY);
            boolean bibChanged;
            if (biblatexMode) {
                if (oldBLMode != null) {
                    if (biblatexBackend != null) {
                        bibChanged = !biblatexBackend.equals(oldBackend);
                    }
                    else {
                        bibChanged = oldBLMode == null;
                    }
                }
                else {
                    bibChanged = true;
                }
            }
            else {
                bibChanged = oldBLMode != null;
            }
            if (bibChanged) {
                TexlipseProperties.setSessionProperty(project,
                        TexlipseProperties.SESSION_BIBTEX_RERUN,
                        new String("true"));
            }
        }
        if (biblatexMode) {
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.SESSION_BIBLATEXMODE_PROPERTY,
                    new Boolean(true));
        }
        else {
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.SESSION_BIBLATEXMODE_PROPERTY,
                    null);
        }
        TexlipseProperties.setSessionProperty(project,
                TexlipseProperties.SESSION_BIBLATEXBACKEND_PROPERTY,
                biblatexBackend);
    }

    /**
     * Updates the references and project data based on the data in the
     * parsed document.
     * 
     * @param monitor Progress monitor
     */
    private void updateReferences(IProgressMonitor monitor) {
        this.updateLabels(parser.getLabels());
        this.updateCommands(parser.getCommands());
        IProject project = getCurrentProject();
        if (project == null) return;
        IFile cFile = ((FileEditorInput) editor.getEditorInput()).getFile();
        boolean isMainFile = cFile.equals(TexlipseProperties.getProjectSourceFile(project));

        pollCancel(monitor);
        
        // After here we just store those fun properties...
        if (parser.isLocalBib()) {
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.SESSION_BIBLATEXLOCALBIB_PROPERTY,
                    new Boolean(true));
        }
        else {
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.SESSION_BIBLATEXLOCALBIB_PROPERTY,
                    null);
        }
        
        //Only update Preamble, Bibstyle if main Document
        if (isMainFile) {
            boolean biblatexMode = parser.isBiblatexMode();
            updateBiblatex(project, biblatexMode, parser.getBiblatexBackend(), false);

            String[] bibs = parser.getBibs();
            this.updateBibs(bibs, biblatexMode, cFile);

            pollCancel(monitor);

            String preamble = parser.getPreamble();
            if (preamble != null) {
                TexlipseProperties.setSessionProperty(project, 
                        TexlipseProperties.PREAMBLE_PROPERTY, 
                        preamble);
            }
            if (!biblatexMode) {
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
    }
    
    /**
     * Updates completions for the BibTeX -data
     * 
     * @param bibNames Names of the BibTeX -files that the document uses
     * @param resource The resource of the document
     */
    private void updateBibs(String[] bibNames, boolean biblatexMode, IResource resource) {
        IProject project = getCurrentProject();
        if (project == null) return;

        if (!biblatexMode) {
            for (int i=0; i < bibNames.length; i++) {
                if (!bibNames[i].endsWith(".bib")) {
                    bibNames[i] += ".bib";
                }
            }
        }
        
        if (bibContainer.checkFreshness(bibNames)) {
            return;
        }
        
        TexlipseProperties.setSessionProperty(project,
                TexlipseProperties.BIBFILE_PROPERTY,
                bibNames);
        
        List<String> newBibs = bibContainer.updateBibHash(bibNames);

        IPath path = resource.getFullPath().removeFirstSegments(1).removeLastSegments(1);        
        if (!path.isEmpty())
            path = path.addTrailingSeparator();
        
        KpsewhichRunner filesearch = new KpsewhichRunner();
                
        for (Iterator<String> iter = newBibs.iterator(); iter.hasNext();) {
        	String name = iter.next();
        	try {
        	    String filepath = "";
        	    //First try local search
        	    IResource res = project.findMember(path + name);
        	    //Try searching relative to main file
        	    if (res == null) {
        	        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
        	        res = sourceDir.findMember(name);
        	    }
                
        	    if (res != null) {
                    filepath = res.getLocation().toOSString();
                }
        	    if (res == null) {
        	        //Try Kpsewhich
        	        filepath = filesearch.getFile(resource, name, "bibtex");
        	        if (filepath.length() > 0 && !(new File(filepath).isAbsolute())) {
        	            //filepath is a local path
        	            res = project.findMember(path + filepath);
        	            if (res != null) {
        	                filepath = res.getLocation().toOSString();
        	            }
        	            else {
        	                filepath = "";
        	            }
        	        }
        	        else if (filepath.length() > 0) {
        	            //Create a link to resource
                        IPath p = new Path(filepath);
                        if (name.indexOf('/') >= 0) {
                            //Remove path from name
                            name = name.substring(name.lastIndexOf('/') + 1);
                        }
                        IFile f = project.getFile(path + name);
                        if (f != null && !f.exists()) {
                            f.createLink(p, IResource.NONE, null);
                        }
        	        }
        	    }
        	    
        		if (filepath.length() > 0) {
        			BibParser parser = new BibParser(filepath);
        			try {
        				List<ReferenceEntry> bibEntriesList = parser.getEntries();
        				if (bibEntriesList != null && bibEntriesList.size() > 0) {
        					bibContainer.addRefSource(path + name, bibEntriesList);
        				} else if (bibEntriesList == null) {
        					MarkerHandler marker = MarkerHandler.getInstance();
        					marker.addFatalError(editor, "The BibTeX file " + filepath + " contains fatal errors, parsing aborted.");
        					continue;
        				}
        			} catch (IOException ioe) {
        				TexlipsePlugin.log("Can't read BibTeX file " + filepath, ioe);
        			}
        		} else {
        			MarkerHandler marker = MarkerHandler.getInstance();
        			marker.addFatalError(editor, "The BibTeX file " +name+ " not found.");
        		}

        	} catch (CoreException ce) {
        		TexlipsePlugin.log("Can't run Kpathsea", ce);
        	}
        }
        bibContainer.organize();
    }
    
    /**
     * Updates the labels.
     * @param labels
     */
    private void updateLabels(List<ReferenceEntry> labels) {
        IResource resource = getFile();
        if (resource == null) return;
        labelContainer.addRefSource(resource.getProjectRelativePath().toString(), labels);
        labelContainer.organize();
    }
    
    /**
     * Updates the commands.
     * @param commands
     */
    private void updateCommands(ArrayList<TexCommandEntry> commands) {
        IResource resource = getFile();
        if (resource == null) return;
        if (commandContainer.addRefSource(resource.getProjectRelativePath().toString(), commands))
            commandContainer.organize();
    }
    
    /**
     * Checks whether all includes exists, if they are outside of the
     * project, add a link to the file to the project 
     * @param includes
     */
    private void processIncludes(List<OutlineNode> includes, IEditorInput input) {
        IProject project = getCurrentProject();
        if (project == null) return;
        IFile referFile = (IFile) input.getAdapter(IFile.class);
        if (referFile == null) return;
        for (OutlineNode node : includes) {
        	IFile f = null;
            IFile mainTexFile = TexlipseProperties.getProjectSourceFile(project);
            if (mainTexFile != null) {
            	//Includes are always relative to the main file
            	f = TexProjectParser.findIFile(node.getName(), mainTexFile, project);
            }
            if (f == null) {
            	//Try finding it relative to refering file
            	f = TexProjectParser.findIFile(node.getName(), referFile, project);
            }
            if (f == null) {
                MarkerHandler marker = MarkerHandler.getInstance();
                String errorMsg = MessageFormat.format(
                        TexlipsePlugin.getResourceString("parseErrorIncludeNotFound"),
                        new Object[] { node.getName() });
                marker.createErrorMarker(referFile, errorMsg, node.getBeginLine());
            }
        }
    }
    
    /**
     * Creates the reference containers.
     *
     */
    private void createReferenceContainers() {
        boolean parseAll = false;
        IProject project = getCurrentProject();
        if (project == null) {
            if (bibContainer == null) bibContainer = new ReferenceContainer();
            if (labelContainer == null) labelContainer = new ReferenceContainer();
            if (commandContainer == null) commandContainer = new TexCommandContainer();
            return;
        }
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
        //IResource resource = ((FileEditorInput)editor.getEditorInput()).getFile();
        
        IResource[] files = TexlipseProperties.getAllProjectFiles(project);        
        
        if (files != null) {
            IFile mainFile = TexlipseProperties.getProjectSourceFile(project);

            for (int i = 0; i < files.length; i++) {
                //IPath path = files[i].getFullPath();
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
                        List<ReferenceEntry> labels = lrep.getLabels();
                        if (labels.size() > 0) {
                            labelContainer.addRefSource(files[i].getProjectRelativePath().toString(), labels);
                        }
                        List<TexCommandEntry> commands = lrep.getCommands();
                        if (commands.size() > 0) {
                            commandContainer.addRefSource(files[i].getProjectRelativePath().toString(), commands);
                        }
                        //Only update Preamble, Bibstyle if main Document
                        if (files[i].equals(mainFile)) {
                            String[] bibs = lrep.getBibs();
                            boolean biblatexMode = lrep.isBiblatexMode();
                            String biblatexBackend = lrep.getBiblatexBackend();
                            this.updateBiblatex(project, biblatexMode, biblatexBackend, true);
                            this.updateBibs(bibs, biblatexMode, files[i]);

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
     * @return the current file or null if the input is no file
     * (e.g. repository entry)
     */
    
    public IFile getFile(){
        if (editor.getEditorInput() instanceof IFileEditorInput) {
            return ((IFileEditorInput) editor.getEditorInput()).getFile();
        }
        return null;
    }
    
    /**
     * @return the current project or null if this file belongs to
     * no project
     */
    private IProject getCurrentProject() {
        return editor.getProject();
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
    
    /**
     * Write a message on the status line.
     * @param msg the message.
     */
    public void setStatusLineErrorMessage(String msg){
        SubStatusLineManager slm = 
            (SubStatusLineManager) editor.getEditorSite().getActionBars().getStatusLineManager();
        slm.setErrorMessage(msg);
        slm.setVisible(true);
    }
    
    /**
     * clean the status line
     *
     */
    public void removeStatusLineErrorMessage(){
        SubStatusLineManager slm = 
            (SubStatusLineManager) editor.getEditorSite().getActionBars().getStatusLineManager();
        //slm.setVisible(false);
        slm.setErrorMessage(null);
    }
    
}
