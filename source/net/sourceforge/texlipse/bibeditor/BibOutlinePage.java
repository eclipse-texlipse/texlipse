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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.bibparser.BibOutlineContainer;
import net.sourceforge.texlipse.extension.BibOutlineActionProvider;
import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;


/**
 * An outline page for the BibTeX-editor.
 * 
 * @author Oskar Ojala
 */
public class BibOutlinePage extends ContentOutlinePage {
    
    public final static String SEGMENTS = "__bib_segments";
    private static final String ACTION_BYAUTHOR = "byauthor";
    private static final String ACTION_BYJOURNAL = "byjournal";
    private static final String ACTION_BYINDEX = "byindex";
    private static final String ACTION_BYYEAR = "byyear";
    private static final String ACTION_OPENBIBREF = "openbibref";
    
    protected BibOutlineContainer content;
    //protected List content;
    //protected Hashtable<String, BibStringTriMap<ReferenceEntry>> sortIndex;
    protected String sortBy;
    protected ITextEditor editor;
    
    private HashMap outlineActions;
    
    //a list that holds additional action-buttons for the outline page
    private List<Action> extensionActions;
    
    /**
     * Constructs a new outline page
     * 
     * @param textEditor The editor that this outline is associated with
     */
    public BibOutlinePage(ITextEditor textEditor) {
        super();
        this.outlineActions = new HashMap();
        this.editor = textEditor;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) {
        
        super.createControl(parent);
        
//      create the context actions
        createActions();
        
        TreeViewer viewer = getTreeViewer();        
        viewer.setContentProvider(new BibContentProvider(editor.getDocumentProvider().getDocument(editor.getEditorInput())));
        viewer.setLabelProvider(new BibLabelProvider());
        viewer.addSelectionChangedListener(this);
        
        createToolbar();
        createContextMenu();
        
        if (this.content != null) {
            viewer.setInput(this.content);
        }
//        if (this.sortIndex != null) {
//            viewer.setInput(this.sortIndex);
//        }
    }
    
    /**
     * Updates the outline with new content.
     * 
     * @param content The new content of the outline
     */    
    //public void update(Hashtable<String, BibStringTriMap<ReferenceEntry>> sortIndex) {
    public void update(BibOutlineContainer content) {
        this.content = content;

        TreeViewer viewer = getTreeViewer();
        //this.sortIndex = sortIndex;
        
        if (viewer != null) {
            Control control = viewer.getControl();
            if (control != null && !control.isDisposed()) {
                control.setRedraw(false);
                viewer.setInput(this.content);
                //viewer.setInput(sortIndex);
                control.setRedraw(true);
            }
        }
    }
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
     */
    public void selectionChanged(SelectionChangedEvent event) {
        super.selectionChanged(event);
        
        ISelection selection = event.getSelection();
        if (selection.isEmpty()) {
            editor.resetHighlightRange();
        } else {
            try {
                ReferenceEntry be = (ReferenceEntry) ((IStructuredSelection) selection).getFirstElement();
                Position position = be.position;
                editor.setHighlightRange(position.getOffset(), position.getLength(), true);
//              editor.setHighlightRange(start, length, true);
            } catch (IllegalArgumentException x) {
                editor.resetHighlightRange();
            } catch (ClassCastException y) {
                editor.resetHighlightRange();
            } catch (NullPointerException z) {
                editor.resetHighlightRange();
            }
        }        
    }
    
    /**
     * Creates the actions assosiated with the outline. 
     */
    private void createActions() {
    	
    	//creates the additional actions from "BibEditorOutlineExtension"-Extension point
    	IConfigurationElement[] config = Platform
		.getExtensionRegistry()
		.getConfigurationElementsFor(
			"net.sourceforge.texlipse.BibEditorOutlineExtension");
        if (config.length > 0){
        	this.extensionActions = new ArrayList<Action>();
        	for (IConfigurationElement elem : config) {
        		try {
        			BibOutlineActionProvider a = (BibOutlineActionProvider)elem.createExecutableExtension("OutlineActionProvider");
        			//get a resource representation of the opened .bib file
        			IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        			//get the action button and add it to the extension buttons
        			this.extensionActions.add(a.getAction(getTreeViewer(), resource));
        		} catch (CoreException e) {
        			//e.printStackTrace();
        			//do some logging
        		}
        	}
        }
        // toolbar actions        
        Action byAuthor = new Action("Sort by Author", IAction.AS_RADIO_BUTTON) {
            public void run() {
                if (isChecked()) {
                    BibOutlineContainer newContent = 
                        ((BibContentProvider)getTreeViewer().getContentProvider()).changeSort(BibOutlineContainer.SORTAUTHOR);
                    update(newContent);
                }
            }
        };
        byAuthor.setToolTipText("Sort by Author");
        byAuthor.setImageDescriptor(TexlipsePlugin.getImageDescriptor("sortauthor"));
        this.outlineActions.put(ACTION_BYAUTHOR, byAuthor);
        
        Action byJournal = new Action("Sort by Journal", IAction.AS_RADIO_BUTTON) {
            public void run() {
                if (isChecked()) {
                    BibOutlineContainer newContent = 
                        ((BibContentProvider)getTreeViewer().getContentProvider()).changeSort(BibOutlineContainer.SORTJOURNAL);
                    update(newContent);
                }
            }
        };        
        byJournal.setToolTipText("Sort by Journal");
        byJournal.setImageDescriptor(TexlipsePlugin.getImageDescriptor("sortjournal"));
        this.outlineActions.put(ACTION_BYJOURNAL, byJournal);
        
        Action byIndex = new Action("Sort by Index", IAction.AS_RADIO_BUTTON) {
            public void run() {
                if (isChecked()) {
                    BibOutlineContainer newContent = 
                        ((BibContentProvider)getTreeViewer().getContentProvider()).changeSort(BibOutlineContainer.SORTINDEX);
                    update(newContent);
                }
            }
        };        
        byIndex.setToolTipText("Sort by Index");
        byIndex.setImageDescriptor(TexlipsePlugin.getImageDescriptor("sortindex"));
        this.outlineActions.put(ACTION_BYINDEX, byIndex);
        
        Action byYear = new Action("Sort by Year", IAction.AS_RADIO_BUTTON) {
            public void run() {
                if (isChecked()) {
                    BibOutlineContainer newContent = 
                        ((BibContentProvider)getTreeViewer().getContentProvider()).changeSort(BibOutlineContainer.SORTYEAR);
                    update(newContent);
                }
            }
        };        
        byYear.setToolTipText("Sort by Year");
        byYear.setImageDescriptor(TexlipsePlugin.getImageDescriptor("sortyear"));
        this.outlineActions.put(ACTION_BYYEAR, byYear);
        
        Action openBibRef = new Action("View reference", IAction.AS_PUSH_BUTTON) {
            public void run() {            	
                TreeItem[] ti = getTreeViewer().getTree().getSelection();
                if (ti != null) {
                    Object selectedElement = ti[0].getData();
                    if (selectedElement instanceof ReferenceEntry) {
                        ReferenceEntry re = (ReferenceEntry)selectedElement;
                        /*if (re.refFile != null)
                         ViewerManager.viewFile(re.refFile);*/
                    }
                }            	
            }
        };        
        openBibRef.setToolTipText("Open the current reference in an external viewer");        
        this.outlineActions.put(ACTION_OPENBIBREF, openBibRef);
    }
    
    private void createToolbar() {
        // add actions to the toolbar
        IToolBarManager toolbarManager = getSite().getActionBars().getToolBarManager();
        toolbarManager.add((IAction)outlineActions.get(ACTION_BYINDEX));
        toolbarManager.add((IAction)outlineActions.get(ACTION_BYAUTHOR));    	
        toolbarManager.add((IAction)outlineActions.get(ACTION_BYJOURNAL));
        toolbarManager.add((IAction)outlineActions.get(ACTION_BYYEAR));      
        //adds the additional actions from the "BibEditorOutlineExtension"-Extension point
        if (this.extensionActions != null) {
        	for (Action a : this.extensionActions) {
        		toolbarManager.add(a);
        	}
        }
    }
    
    /**
     * Fill the context menu.
     * 
     * @param the IMenuManager of the context menu
     */
    private void fillContextMenu(IMenuManager mgr) {
        TreeItem[] ti = getTreeViewer().getTree().getSelection();
        if (ti != null && ti.length > 0) {
            Object selectedElement = ti[0].getData();
            if (selectedElement instanceof ReferenceEntry) {
                if (((ReferenceEntry)selectedElement).refFile != null) {
                    mgr.add((IAction)outlineActions.get(ACTION_OPENBIBREF));
                }    			    	            			
            }
        }
    }
    
    private void createContextMenu() {
        // create menu manager
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager mgr) {
                fillContextMenu(mgr);
            }
        });
        // create the menu
        Menu menu = menuMgr.createContextMenu(getTreeViewer().getControl());
        getTreeViewer().getControl().setMenu(menu);        
    }
}
