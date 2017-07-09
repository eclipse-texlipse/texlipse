/*******************************************************************************
 * Copyright (c) 2017 the TeXlipse team and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/
package org.eclipse.texlipse.outline;

import org.eclipse.jface.action.Action;
import org.eclipse.texlipse.TexlipsePlugin;


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