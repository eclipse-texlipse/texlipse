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


/**
 * The outline update (refresh) action. 
 * 
 * @author Taavi Hupponen
 */
public class TexOutlineActionUpdate extends Action {
	
	private TexOutlinePage outline;
	
	public TexOutlineActionUpdate(TexOutlinePage outline) {
		super("Update");
		setToolTipText("Update");
		
		setImageDescriptor(TexlipsePlugin.getImageDescriptor("refresh"));
		this.outline = outline;
	}
	
	public void run() {
		if (outline.isModelDirty()) {
			outline.getEditor().updateModelNow();
		}
	}
}