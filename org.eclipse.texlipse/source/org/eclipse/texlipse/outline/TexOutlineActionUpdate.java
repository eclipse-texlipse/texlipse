/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
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