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

package org.eclipse.texlipse.editor.hover;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.texlipse.editor.TexEditor;

/**
 * @author boris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TexHoverControlCreator implements IInformationControlCreator {

	private TexEditor editor;
	
	public TexHoverControlCreator(TexEditor editor){
		this.editor = editor;
	}
	
	public IInformationControl createInformationControl(Shell parent) {
		return new TexInformationControl(editor, parent);
	}

}
