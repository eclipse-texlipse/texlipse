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

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.OutlineNode;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.TransferData;


/**
 * The Drag'n'Drop adapter for TexOutline. All the drag'n'drop 
 * operations are text based. 
 * 
 * Since the Positions do not seem to get updated fast enough
 * the drag source position is calculated manually.
 * 
 * @author Taavi Hupponen
 *
 */
public class TexOutlineDNDAdapter extends ViewerDropAdapter implements
		DragSourceListener {

	private TexOutlinePage outline;
	private OutlineNode dragSource;
	private int removeOffset;
	
	public TexOutlineDNDAdapter(Viewer viewer, TexOutlinePage outlinePage) {
		super(viewer);
		this.outline = outlinePage;
		this.setFeedbackEnabled(false);
	}
	
	/** 
	 * Perform the drop. Also calculate the source remove offset before
     * actual dropping.
     * 
     * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	public boolean performDrop(Object data) {

		// get target and calculate remove offset if target is 
        // above source
		OutlineNode target = (OutlineNode)this.getCurrentTarget();
		int targetOffset = target.getPosition().getOffset() + target.getPosition().getLength();
		int sourceOffset = this.dragSource.getPosition().getOffset();
		if (targetOffset < sourceOffset) {
			removeOffset = sourceOffset + ((String)data).length();
		} else {
			removeOffset = sourceOffset;
		}
		
        // drop return false if fails
		try {
			getDocument().replace(targetOffset, 0, (String)data);
		} catch (BadLocationException e) {
		    TexlipsePlugin.log("Could not perform drop operation.", e);
            return false;
		}
		
		return true;
	}

	/**
	 * Validate the drop. Invalidation is caused if
     * 
     * - outline is not uptodate
     * - transfer type is other than text
     * - drop target is equal or children of source
     * - target is preamble
     * 
     * @param target
     * @param operation
     * @param transferType
     * 
     * @return true if drop is valid
     * 
     * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {

		// deny if outline is dirty
		if (this.outline.isModelDirty()) {
			return false;
		}
		
		// check transfer type, only allow text
		if (!TextTransfer.getInstance().isSupportedType(transferType)) {
			return false;
		}
				
		// get the selected node, check null and type
		OutlineNode targetNode = (OutlineNode)target;
		if (targetNode == null || targetNode.getType() == OutlineNode.TYPE_PREAMBLE) {
			return false;
		}
    
		// deny dropping over oneself or over ones children
		if (targetNode.equals(this.dragSource) ||
				isAncestor(dragSource, targetNode)) {
			return false;
		}
		
		return true;
	}


	/** 
     * Validate the drag start. Dragging is denied if:
     * 
     * - outline is not uptodate
     * - source is preamble
     *
     * @param event the drag event 
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public void dragStart(DragSourceEvent event) {
		event.doit = false;
		
		// deny if outline is dirty
		if (this.outline.isModelDirty()) {
			return;
		}
		
		// get the selected node
		OutlineNode node = this.getSelection();
		if (node == null) {
			return;
		}
		
		// deny dragging of certain elements
		if (node.getType() == OutlineNode.TYPE_PREAMBLE) {
			return;
		}
        
		// proceed
		this.dragSource = node;
		event.doit = true;
	}

	/**
     * Set the text data into TextTransfer.
     * 
     * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
    public void dragSetData(DragSourceEvent event) {

		// check that requested data type is supported
		if (!TextTransfer.getInstance().isSupportedType(event.dataType)) {
			return;
		}

        // get the source text
		int sourceOffset = this.dragSource.getPosition().getOffset();
		int sourceLength = this.dragSource.getPosition().getLength();
		
		Position sourcePosition = dragSource.getPosition();
		String sourceText = "";
		try {
			sourceText = getDocument().get(sourcePosition.getOffset(), sourcePosition.getLength());
		} catch (BadLocationException e) {
		    TexlipsePlugin.log("Could not set drag data.", e);
			return;
		}

        // set the data
        event.data = sourceText;
	}

	/**
     * Finish the drag by removing the source text if the operation
     * was MOVE.
     * 
     * Trigger updating of TexlipseModel and outline when done.
     * 
     * @param event the dragEvent
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	public void dragFinished(DragSourceEvent event) {

        // remove MOVE source
        if (event.detail == DND.DROP_MOVE) {
			int sourceLength = this.dragSource.getPosition().getLength();
			try {
				getDocument().replace(removeOffset, sourceLength, "");
			} catch (BadLocationException e) {
			    TexlipsePlugin.log("Could not remove drag'n'drop source.", e);
			}
		}

        // trigger parsing
		this.outline.getEditor().updateModelNow();
	}

	/**
     * Helper for getting the IDocument.
	 * 
     * @return the IDocument assosiated with the outline.
	 */
    private IDocument getDocument() {
		return outline.getEditor().getDocumentProvider().getDocument(outline.getEditor().getEditorInput());
	}
    
    /**
     * Helper for getting the current selection in the viewer.
     * 
     * @return currently selected OutlineNode or null if none is selected
     */
	private OutlineNode getSelection() {
		ISelection selection = this.getViewer().getSelection();
		if (selection == null) {
			return null;
		}
		return (OutlineNode)((IStructuredSelection)selection).getFirstElement();
	}

    /**
     * Returns whether the given ancestor is ancestor to the given child.
     * 
     * @param ancestor
     * @param child
     * @return true if ancestor is ancestor for the given child
     */
	private boolean isAncestor(OutlineNode ancestor, OutlineNode child) {
		OutlineNode parent = child.getParent();
		
		if (parent == null) {
			return false;
		}
		else if (parent.equals(ancestor)) {
			return true;
		}
		else {
			return isAncestor(ancestor, parent);
		}
	}
}
