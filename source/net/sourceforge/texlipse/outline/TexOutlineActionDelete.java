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
import org.eclipse.ui.ISharedImages;


/**
 * The outline delete action. 
 * 
 * @author Taavi Hupponen
 */
public class TexOutlineActionDelete extends Action {
	
	private TexOutlinePage outline;
	
	public TexOutlineActionDelete(TexOutlinePage outline) {
		super("Delete");
		setToolTipText("Delete");
		setImageDescriptor(TexlipsePlugin.getDefault().getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		setDisabledImageDescriptor(TexlipsePlugin.getDefault().getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));
	
		this.outline = outline;
	}
	
	public void run() {
		if (outline.isModelDirty()) {
			return;
		}

		outline.removeSelectedText();
	}
}