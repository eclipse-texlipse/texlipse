/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.treeview.views;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.TexOutlineInput;
import net.sourceforge.texlipse.outline.TexOutlinePage;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.PageSite;
import org.eclipse.ui.part.ViewPart;

/**
 * The view for the full outline.
 *
 * @author Boris von Loesch
 */
public class TexOutlineTreeView extends ViewPart implements  
	ISelectionChangedListener, IPartListener { 
    
    private TexOutlinePage outline;

    /**
     * The constructor.
     *
     */
    public TexOutlineTreeView() {
    	super();
    	outline = new TexOutlinePage(null);
    }
    
    
    /**
     * Creates the viewer. Registers the full outline at the document model.
     */
    public void createPartControl(Composite parent) {
		PageSite site = new PageSite(getViewSite());
		outline.init(site);
    	outline.createControl(parent);
    	((SubActionBars) site.getActionBars()).activate(true);
    	outline.switchTreeViewerSelectionChangeListener(this);
    	
    	// add a part listener if the editor isn't available when the view is created.
        getSite().getPage().addPartListener(this);
        // register it directly if the view is already created.
        IEditorPart part = getSite().getPage().getActiveEditor();
        if (part != null && part instanceof TexEditor) {
            TexEditor e = (TexEditor) part;
            e.registerFullOutline(this);
        }
    }
    

    /**
     * Updates the outline with the new input.
     * @param input the new input.
     */
    public void update(TexOutlineInput input) {
    	outline.update(input);
    }
    
    /**
     * Focuses the editor to the text of the selected item. Opens a new editor if
     * the node is from a different file.
     * 
     * @param event the selection event
     */
    public void selectionChanged(SelectionChangedEvent event) {
        //fireSelectionChanged(event.getSelection());
        ISelection selection = event.getSelection();
        if (selection.isEmpty()) {
            outline.getEditor().resetHighlightRange();
        }
        else {
            OutlineNode node = (OutlineNode) ((IStructuredSelection) selection).getFirstElement();
            outline.getEditor().resetHighlightRange();
            
            if (node.getIFile() != null){
                FileEditorInput input = new FileEditorInput(node.getIFile());
                try {
                    // open the editor and go to the correct position.
                    // this position must be calculated here, because
                    // the position of a node in an other file isn't available.
                    IWorkbenchPage cPage = TexlipsePlugin.getCurrentWorkbenchPage();
                    TexEditor e = (TexEditor) cPage.findEditor(input);
                    if (e == null)
                        e = (TexEditor) cPage.openEditor(input, "net.sourceforge.texlipse.TexEditor");
                    if (cPage.getActiveEditor() != e)
                        cPage.activate(e);
                    IDocument doc = e.getDocumentProvider().getDocument(e.getEditorInput());
                    int beginOffset = doc.getLineOffset(node.getBeginLine() - 1);
                    int length;
                    if (node.getEndLine() - 1 == doc.getNumberOfLines())
                        length = doc.getLength() - beginOffset;
                    else
                        length = doc.getLineOffset(node.getEndLine() - 1) - beginOffset;
                    e.setHighlightRange(beginOffset, length, true);
                } catch (PartInitException e) {
                    TexlipsePlugin.log("Can't open editor.", e);
                } catch (BadLocationException e) {
                	outline.getEditor().resetHighlightRange();
                }
            }
        }
    }      
    

    /**
     * Returns whether the current TexDocumentModel is dirty
     * 
     * @return if current model is dirty.
     */
    public boolean isModelDirty() {
    	return outline.isModelDirty();
    }
    
    /**
     * Call this method to reset all internal states after a project change 
     *
     */
    public void projectChanged() {
        outline.reset();
    }
    
    
    /**
     * Called by the TexDocumentModel when it gets dirty. Enables
     * the update button.
     */
    public void modelGotDirty() {
        outline.modelGotDirty();
    }
    
    /**
     * 
     * @return the editor
     */
    public TexEditor getEditor() {
    	return outline.getEditor();
    }
    
    /**
     * 
     * @param editor the editor.
     */
    public void setEditor(TexEditor editor) {
    	outline.setEditor(editor);
    }
    
    /**
     * registers the full outline, when the editor is activated.
     */
    public void partActivated(IWorkbenchPart part) {
        if (part instanceof TexEditor) {
            if (outline.getEditor() != null) {
            	outline.getEditor().unregisterFullOutline(this);
            }
            TexEditor e = (TexEditor) part;
            e.registerFullOutline(this);
            setEditor(e);
        }
    }
    
    /* (non-Javadoc)
     * Method declared on IViewPart.
     * Treat this the same as part activation.
     */
    public void partBroughtToTop(IWorkbenchPart part) {
        partActivated(part);
    }
    
    /**
     * Unregisters the full outline and removes the PartListener
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        getSite().getPage().removePartListener(this);
        outline.getEditor().unregisterFullOutline(this);
    }


	@Override
	public void setFocus() {
		outline.setFocus();
	}


	public void partClosed(IWorkbenchPart part) {
	}


	public void partDeactivated(IWorkbenchPart part) {
	}


	public void partOpened(IWorkbenchPart part) {
	}
}
