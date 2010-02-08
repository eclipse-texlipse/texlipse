/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.outline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.TexOutlineInput;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;


/**
 * The outline page for the TexEditor.
 *
 * @author Taavi Hupponen
 * @author Laura Takkinen
 */
public class TexOutlinePage extends ContentOutlinePage {
    
    private static final String ACTION_COPY = "copy";
    private static final String ACTION_CUT = "cut";
    private static final String ACTION_PASTE = "paste";
    private static final String ACTION_DELETE = "delete";
    private static final String ACTION_UPDATE = "update";
    private static final String ACTION_COLLAPSE = "collapse";
    private static final String ACTION_EXPAND = "expand";
    private static final String ACTION_HIDE_SEC = "hideSec";
    private static final String ACTION_HIDE_SUBSEC = "hideSubSec";
    private static final String ACTION_HIDE_SUBSUBSEC = "hideSubSubSec";
    private static final String ACTION_HIDE_PARAGRAPH = "hidePara";
    private static final String ACTION_HIDE_FLOAT = "hideFloat";
    private static final String ACTION_HIDE_LABEL = "hideLabel";
    
    private TexOutlineInput input;
    private TexEditor editor;
    private TexOutlineFilter filter;
    private Clipboard clipboard;
    private int expandLevel;
    private Map<String, IAction> outlineActions;
    //private Set outlineProperties;
    
    /**
     * The constructor.
     * 
     * @param texEditor the editor associated with this page
     */
    public TexOutlinePage(TexEditor texEditor) {
        super();
        this.editor = texEditor;
        expandLevel = 1;
        this.outlineActions = new HashMap<String, IAction>();
                
        
        TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new  
                IPropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent event) {
                
                String property = event.getProperty();
                if (TexlipseProperties.OUTLINE_PART.equals(property) || 
                	TexlipseProperties.OUTLINE_CHAPTER.equals(property) ||
					TexlipseProperties.OUTLINE_SECTION.equals(property) ||
					TexlipseProperties.OUTLINE_SUBSECTION.equals(property) ||
					TexlipseProperties.OUTLINE_SUBSUBSECTION.equals(property) ||
					TexlipseProperties.OUTLINE_PARAGRAPH.equals(property) ||
					TexlipseProperties.OUTLINE_ENVS.equals(property)) {
                	getOutlinePreferences();
                    resetToolbarButtons();
                    TreeViewer viewer = getTreeViewer();
                    if (viewer != null) {
                        Control control= viewer.getControl();
                        if (control != null && !control.isDisposed()) {
                        	viewer.refresh();
                        }
                    }	
                }
            }	
        });    
        
    }  
    
    /**
     * Creates the control ie. creates all the stuff that matters and
     * is visible in the outline. 
     * 
     * Actions must be created before menus and toolbars.
     * 
     * @param parent
     */
    public void createControl(Composite parent) {
        super.createControl(parent);
        
        // create the context actions
        createActions();
        
        // initialize the tree viewer
        TreeViewer viewer = getTreeViewer();		
        filter = new TexOutlineFilter();
        viewer.setContentProvider(new TexContentProvider(filter));
        viewer.setLabelProvider(new TexLabelProvider());
        viewer.setComparer(new TexOutlineNodeComparer());
        
        // get and apply the preferences
        this.getOutlinePreferences();
        viewer.addFilter(filter);
        
        // set the selection listener
        viewer.addSelectionChangedListener(this);
        
        // enable drag'n'drop support
        TexOutlineDNDAdapter dndAdapter = new TexOutlineDNDAdapter(viewer, this);
        int ops = DND.DROP_COPY | DND.DROP_MOVE;
        Transfer[] transfers = new Transfer[] {TextTransfer.getInstance()};
        viewer.addDragSupport(ops, transfers, dndAdapter);
        viewer.addDropSupport(ops, transfers, dndAdapter);
        
        // enable copy-paste
        initCopyPaste(viewer);
        
        // create the menu bar and the context menu
        createToolbar();
        resetToolbarButtons();
        createContextMenu();
        
        
        // finally set the input
        if (this.input != null) {
            viewer.setInput(this.input.getRootNodes());
            
            // set update button status and also the context actions
            outlineActions.get(ACTION_UPDATE).setEnabled(false);
            outlineActions.get(ACTION_COPY).setEnabled(true);
            outlineActions.get(ACTION_CUT).setEnabled(true);
            outlineActions.get(ACTION_PASTE).setEnabled(true);
            outlineActions.get(ACTION_DELETE).setEnabled(true);

        }
    }
    
    @Override
    public void setFocus() {
    	getTreeViewer().getTree().setFocus();
    }
    
    /**
     * Updates the outline with new content. Called by TexDocumentModel
     * through the editor. Saves the visible state of the outline,
     * sets the new content and restores the state.
     *
     * @param input the new outline input
     */
    public void update(TexOutlineInput input) {
        this.input = input;
        
        TreeViewer viewer= getTreeViewer();
        if (viewer != null) {
            
            Control control= viewer.getControl();
            if (control != null && !control.isDisposed()) {
                control.setRedraw(false);
                // save viewer state
                //ISelection selection = viewer.getSelection();
                viewer.getTree().deselectAll();
                
                Object[] expandedElements = viewer.getExpandedElements();
                
                /*
                 ArrayList oldNodes = new ArrayList(expandedElements.length);
                 for (int i = 0; i < expandedElements.length; i++) {
                 oldNodes.add(expandedElements[i]);
                 }
                 */
                
                // set new input
                viewer.setInput(input.getRootNodes());
                
                // restore viewer state
                viewer.setExpandedElements(expandedElements);
                
                /*
                 ArrayList newNodes = new ArrayList();
                 OutlineNode newNode;
                 for (Iterator iter = this.input.getRootNodes().iterator(); iter.hasNext();) {
                 newNode = (OutlineNode)iter.next();
                 restoreExpandState(newNode, oldNodes, newNodes);
                 }
                 */
                control.setRedraw(true);
                
                // disable the refresh button, enable context stuff
                outlineActions.get(ACTION_UPDATE).setEnabled(false);
                outlineActions.get(ACTION_COPY).setEnabled(true);
                outlineActions.get(ACTION_CUT).setEnabled(true);
                outlineActions.get(ACTION_PASTE).setEnabled(true);
                outlineActions.get(ACTION_DELETE).setEnabled(true);
            }
        }
    }
    
    /**
     * Focuses the editor to the text of the selected item.
     * 
     * @param event the selection event
     */
    public void selectionChanged(SelectionChangedEvent event) {
        super.selectionChanged(event);
        
        ISelection selection = event.getSelection();
        if (selection.isEmpty()) {
            editor.resetHighlightRange();
        }
        
        else {
            OutlineNode node = (OutlineNode) ((IStructuredSelection) selection).getFirstElement();
            Position position = node.getPosition();
            if (position != null) {
                try {
                    editor.setHighlightRange(position.getOffset(), position.getLength(), true);
                    editor.getViewer().revealRange(position.getOffset(), position.getLength());
                } catch (IllegalArgumentException x) {
                    editor.resetHighlightRange();
                }
            } else {
                editor.resetHighlightRange();
            }
        }        
    }
    
    /**
     * Gets the text of the currently selected item. Use by copy paste
     * and drag'n'drop operations. 
     * 
     * @return text of the currently selected item or null if no item
     * is selected
     * 
     * TODO handle multiple selections
     */
    public String getSelectedText() {
        IStructuredSelection selection = (IStructuredSelection)getTreeViewer().getSelection();
        if (selection == null) {
            return null;
        }
        
        OutlineNode node = (OutlineNode)selection.getFirstElement();
        Position pos = node.getPosition();
        
        String text;
        try {
            text = this.editor.getDocumentProvider().getDocument(this.editor.getEditorInput()).get(pos.getOffset(), pos.getLength());
        } catch (BadLocationException e) {
            return null;
        }
        return text;
    }
    
    /**
     * Removes the text of the currently selected item From the 
     * document. Used by copy paste and drag'n'drop operations.
     * 
     * Trigger parsing after remove is done. 
     * 
     * TODO handle multiple selections
     */
    public void removeSelectedText() {
        IStructuredSelection selection = (IStructuredSelection)getTreeViewer().getSelection();
        if (selection == null) {
            return;
        }
        
        OutlineNode node = (OutlineNode)selection.getFirstElement();
        Position pos = node.getPosition();
        
        try {
            this.editor.getDocumentProvider().getDocument(this.editor.getEditorInput()).replace(pos.getOffset(), pos.getLength(), "");
        } catch (BadLocationException e) {
            return;
        }
        
        this.editor.updateModelNow();
    }
    
    /**
     * Dispose the clipboard.
     */
    public void dispose() {
        super.dispose();
        this.clipboard.dispose();
        this.clipboard = null;
    }
    
    /**
     * Pastes given text after the selected item. Used by the paste
     * action.
     * 
     * Triggers model update afterwards.
     * 
     * @param text the text to be pasted
     * @return true if pasting was succesful, otherwise false
     */
    public boolean paste(String text) {
        // get selection
        IStructuredSelection selection = (IStructuredSelection)getTreeViewer().getSelection();
        if (selection == null) {
            return false;
        }
        OutlineNode node = (OutlineNode)selection.getFirstElement();
        Position pos = node.getPosition();
        
        // paste the text
        try {
            this.editor.getDocumentProvider().getDocument(this.editor.getEditorInput()).replace(pos.getOffset() + pos.getLength(), 0, text);
        } catch (BadLocationException e) {
            return false;
        }
        
        // trigger parsing
        this.editor.updateModelNow();
        return true;
    }
    
    /**
     * Called by the TexDocumentModel when it gets dirty. Enables
     * the update button.
     */
    public void modelGotDirty() {
        outlineActions.get(ACTION_UPDATE).setEnabled(true);
        outlineActions.get(ACTION_COPY).setEnabled(false);
        outlineActions.get(ACTION_CUT).setEnabled(false);
        outlineActions.get(ACTION_PASTE).setEnabled(false);
        outlineActions.get(ACTION_DELETE).setEnabled(false);
    }
    
    /**
     * Returns whether the current TexDocumentModel is dirty
     * 
     * @return if current model is dirty.
     */
    public boolean isModelDirty() {
        return editor.isModelDirty();
    }
    
    /**
     * Returns the editor associated with this outline page.
     * 
     * @return the editor associated with this outline page.
     */
    public TexEditor getEditor() {
        return this.editor;
    }
    
    public void setEditor(TexEditor editor) {
    	this.editor = editor;
    }
    
    /**
     * Gets the clipboard. Used by copy paste actions.
     * 
     * @return the clipboard associated with this outline
     */
    public Clipboard getClipboard() {
        return this.clipboard;
    }
    
    /*
     * Creates a new action to hide a certain nodeType
     */
    private IAction createHideAction(String desc, final int nodeType, ImageDescriptor img) {
        IAction action = new Action(desc, IAction.AS_CHECK_BOX) {
            public void run() {
                boolean oldState = filter.isTypeVisible(nodeType);
                filter.toggleType(nodeType, !oldState);
                TreeViewer viewer = getTreeViewer();
                if (oldState == false) {
                    revealNodes(nodeType);
                }
                viewer.refresh();
            }
        };
        action.setToolTipText(desc);
        action.setImageDescriptor(img);
        return action;
    }
    
    /**
     * Creates the actions associated with the outline. 
     */
    private void createActions() {
        // context menu actions 
        TexOutlineActionCut cut = new TexOutlineActionCut(this);
        this.outlineActions.put(ACTION_CUT, cut);
        
        TexOutlineActionCopy copy = new TexOutlineActionCopy(this);
        this.outlineActions.put(ACTION_COPY, copy);
        
        TexOutlineActionPaste paste = new TexOutlineActionPaste(this);
        this.outlineActions.put(ACTION_PASTE, paste);
        
        TexOutlineActionDelete delete = new TexOutlineActionDelete(this);
        this.outlineActions.put(ACTION_DELETE, delete);
        
        
        // toolbar actions
        TexOutlineActionUpdate update = new TexOutlineActionUpdate(this);
        this.outlineActions.put(ACTION_UPDATE, update);
        
        
        Action collapse = new Action("Collapse one level", IAction.AS_PUSH_BUTTON) {
            public void run() {
                if (expandLevel > 1) {
                    expandLevel--;
                    getTreeViewer().collapseAll();
                    getTreeViewer().expandToLevel(expandLevel);
                }
            }
        };
        collapse.setToolTipText("Collapse one level");
        collapse.setImageDescriptor(TexlipsePlugin.getImageDescriptor("collapse"));
        this.outlineActions.put(ACTION_COLLAPSE, collapse);
        
        Action expand = new Action("Expand one level", IAction.AS_PUSH_BUTTON) {
            public void run() {
                if (expandLevel < input.getTreeDepth()) {
                    expandLevel++;
                }
                getTreeViewer().collapseAll();
                getTreeViewer().expandToLevel(expandLevel);
            }
        };
        expand.setToolTipText("Expand one level");
        expand.setImageDescriptor(TexlipsePlugin.getImageDescriptor("expand"));
        this.outlineActions.put(ACTION_EXPAND, expand);
        
        IAction action = createHideAction("Hide sections", OutlineNode.TYPE_SECTION, 
        		TexlipsePlugin.getImageDescriptor("hide_sec"));
        this.outlineActions.put(ACTION_HIDE_SEC, action);
        
        action = createHideAction("Hide subsections", OutlineNode.TYPE_SUBSECTION, 
        		TexlipsePlugin.getImageDescriptor("hide_sub"));
        this.outlineActions.put(ACTION_HIDE_SUBSEC, action);
        
        action = createHideAction("Hide subsubsections", OutlineNode.TYPE_SUBSUBSECTION, 
        		TexlipsePlugin.getImageDescriptor("hide_subsub"));
        this.outlineActions.put(ACTION_HIDE_SUBSUBSEC, action);

        action = createHideAction("Hide paragraphs", OutlineNode.TYPE_PARAGRAPH, 
        		TexlipsePlugin.getImageDescriptor("hide_para"));
        this.outlineActions.put(ACTION_HIDE_PARAGRAPH, action);
                
        action = createHideAction("Hide floating environments", OutlineNode.TYPE_ENVIRONMENT, 
        		TexlipsePlugin.getImageDescriptor("hide_env"));
        this.outlineActions.put(ACTION_HIDE_FLOAT, action);

        action = createHideAction("Hide labels", OutlineNode.TYPE_LABEL, 
        		TexlipsePlugin.getImageDescriptor("hide_label"));
        this.outlineActions.put(ACTION_HIDE_LABEL, action);
    }
    
    /**
     * Initialize copy paste by getting the clipboard and hooking 
     * the actions to global edit menu.
     * 
     * @param viewer
     */
    private void initCopyPaste(TreeViewer viewer) {
        this.clipboard = new Clipboard(getSite().getShell().getDisplay());
        
        IActionBars bars = getSite().getActionBars();
        bars.setGlobalActionHandler(
                ActionFactory.CUT.getId(), 
                (Action)outlineActions.get(ACTION_CUT));
        
        bars.setGlobalActionHandler(
                ActionFactory.COPY.getId(),
                (Action)outlineActions.get(ACTION_COPY));
        
        bars.setGlobalActionHandler(
                ActionFactory.PASTE.getId(),
                (Action)outlineActions.get(ACTION_PASTE));
        
        bars.setGlobalActionHandler(
                ActionFactory.DELETE.getId(),
                (Action)outlineActions.get(ACTION_DELETE));
    }
    
    /**
     * Get the preferences.
     * 
     */
    private void getOutlinePreferences()  {
        filter.reset();
        
        // add node types to be included
        boolean preamble = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.OUTLINE_PREAMBLE);
        boolean part = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.OUTLINE_PART);
        boolean chapter = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.OUTLINE_CHAPTER);
        boolean section = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.OUTLINE_SECTION);
        boolean subsection = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.OUTLINE_SUBSECTION);
        boolean subsubsection = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.OUTLINE_SUBSUBSECTION);
        boolean paragraph = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.OUTLINE_PARAGRAPH);
        
        if (preamble) {
            filter.toggleType(OutlineNode.TYPE_PREAMBLE, true);
        }
        if (part) {
            filter.toggleType(OutlineNode.TYPE_PART, true);
        }
        if (chapter) {
            filter.toggleType(OutlineNode.TYPE_CHAPTER, true);
        }
        if (section) {
            filter.toggleType(OutlineNode.TYPE_SECTION, true);
        }
        if (subsection) {
            filter.toggleType(OutlineNode.TYPE_SUBSECTION, true);
        }
        if (subsubsection) {
            filter.toggleType(OutlineNode.TYPE_SUBSUBSECTION, true);
        }
        if (paragraph) {
            filter.toggleType(OutlineNode.TYPE_PARAGRAPH, true);
        }
        
        // add floats to be included (and env type)
        filter.toggleType(OutlineNode.TYPE_ENVIRONMENT, true);
        filter.toggleType(OutlineNode.TYPE_LABEL, true);
        
        String[] environments = TexlipsePlugin.getPreferenceArray(TexlipseProperties.OUTLINE_ENVS);
        for (String env : environments) {
            filter.toggleEnvironment(env, true);            
        }
    }
    
    /**
     * Fill the context menu.
     * 
     * @param the IMenuManager of the context menu
     */
    private void fillContextMenu(IMenuManager mgr) {
        mgr.add(outlineActions.get(ACTION_COPY));
        mgr.add(outlineActions.get(ACTION_CUT));
        mgr.add(outlineActions.get(ACTION_PASTE));
        mgr.add(new Separator());
        mgr.add(outlineActions.get(ACTION_DELETE));
    }
    
    
    private void resetToolbarButtons() {
        outlineActions.get(ACTION_HIDE_SEC).setChecked(!filter.isTypeVisible(OutlineNode.TYPE_SECTION));
        outlineActions.get(ACTION_HIDE_SUBSEC).setChecked(!filter.isTypeVisible(OutlineNode.TYPE_SUBSECTION));
        outlineActions.get(ACTION_HIDE_SUBSUBSEC).setChecked(!filter.isTypeVisible(OutlineNode.TYPE_SUBSUBSECTION));
        outlineActions.get(ACTION_HIDE_PARAGRAPH).setChecked(!filter.isTypeVisible(OutlineNode.TYPE_PARAGRAPH));
        outlineActions.get(ACTION_HIDE_FLOAT).setChecked(!filter.isTypeVisible(OutlineNode.TYPE_ENVIRONMENT));
        outlineActions.get(ACTION_HIDE_LABEL).setChecked(!filter.isTypeVisible(OutlineNode.TYPE_LABEL));
    }
    
    /**
     * Removes own SelectionChangeListener from TreeViewer and uses listener instead 
     * Needed for Full LaTeX Outline
     */
    public void switchTreeViewerSelectionChangeListener(ISelectionChangedListener listener) {
    	getTreeViewer().removeSelectionChangedListener(this);
    	getTreeViewer().addSelectionChangedListener(listener);
    }
    
    /**
     * Resets outline (needed for Full LaTeX outline)
     */
    public void reset() {
    	this.expandLevel = 1;
    }
    
    /**
     * Create the toolbar.
     *
     */
    private void createToolbar() {
        
        // add actions to the toolbar
        IToolBarManager toolbarManager = getSite().getActionBars().getToolBarManager();
        toolbarManager.add(outlineActions.get(ACTION_UPDATE));
        toolbarManager.add(outlineActions.get(ACTION_COLLAPSE));
        toolbarManager.add(outlineActions.get(ACTION_EXPAND));
        toolbarManager.add(outlineActions.get(ACTION_HIDE_SEC));
        toolbarManager.add(outlineActions.get(ACTION_HIDE_SUBSEC));
        toolbarManager.add(outlineActions.get(ACTION_HIDE_SUBSUBSEC));
        toolbarManager.add(outlineActions.get(ACTION_HIDE_PARAGRAPH));
        toolbarManager.add(outlineActions.get(ACTION_HIDE_FLOAT));
        toolbarManager.add(outlineActions.get(ACTION_HIDE_LABEL));
    }
    
    /**
     * Creates the context menu. 
     */
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
        
        //register menu for extensions
        //getSite().registerContextMenu(menuMgr, getTreeViewer());
    }
    
    /**
     * Reveals all the nodes of certain type in the outline tree.
     * 
     * @param nodeType the type of nodes to be revealed
     */
    private void revealNodes(int nodeType) {
        List<OutlineNode> nodeList = input.getTypeList(nodeType);
        if (nodeList != null) {
            for(OutlineNode node : nodeList) {
                getTreeViewer().reveal(node);
            }
        }
    }
    
    /*
     private void restoreExpandState(OutlineNode newNode, ArrayList oldNodes, ArrayList newNodes) {
     
     // check this node
      OutlineNode oldNode;
      for(Iterator iter = oldNodes.iterator(); iter.hasNext();) {
      oldNode = (OutlineNode)iter.next();
      if (newNode.likelySame(oldNode)) {
      //System.out.println(newNode.getName() + " LIKE " + oldNode.getName());
       newNodes.add(newNode);
       iter.remove();
       }
       }
       
       // continue with children
        ArrayList children = newNode.getChildren();
        if (children != null) {
        for (Iterator iter = children.iterator(); iter.hasNext();){
        restoreExpandState((OutlineNode)iter.next(), oldNodes, newNodes);
        }
        }
        }
        */
}