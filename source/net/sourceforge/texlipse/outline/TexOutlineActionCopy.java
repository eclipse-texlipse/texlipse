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

import org.eclipse.jface.action.Action;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.ISharedImages;


/**
 * The outline copy action. 
 * 
 * @author Taavi Hupponen
 */
public class TexOutlineActionCopy extends Action {
	
	private TexOutlinePage outline;
	
	public TexOutlineActionCopy(TexOutlinePage outline) {
		super("Copy");
		setToolTipText("Copy");
		setImageDescriptor(TexlipsePlugin.getDefault().getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		setDisabledImageDescriptor(TexlipsePlugin.getDefault().getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
		this.outline = outline;
	}
	
	public void run() {
		if (outline.isModelDirty()) {
			return;
		}
		String text = outline.getSelectedText();
		if (text != null) {
			outline.getClipboard().setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
		}
	}
}