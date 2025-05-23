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
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.ui.ISharedImages;


/**
 * The outline paste action. 
 * 
 * @author Taavi Hupponen
 */
public class TexOutlineActionPaste extends Action {
	
	private TexOutlinePage outline;
	
	public TexOutlineActionPaste(TexOutlinePage outline) {
		super("Paste");
		setToolTipText("Paste");
		setImageDescriptor(TexlipsePlugin.getDefault().getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setDisabledImageDescriptor(TexlipsePlugin.getDefault().getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));

		this.outline = outline;
	}
	
	public void run() {
		if (outline.isModelDirty()) {
			return;
		}

		String text = (String)outline.getClipboard().getContents(TextTransfer.getInstance());
		if (text == null) {
			return;
		}
		outline.paste(text);
	}
}