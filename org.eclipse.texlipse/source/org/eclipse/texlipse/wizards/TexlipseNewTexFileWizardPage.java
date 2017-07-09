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
package org.eclipse.texlipse.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

/**
 * @author Boris von Loesch
 */

public class TexlipseNewTexFileWizardPage extends WizardNewFileCreationPage {

	/**
	 * Constructor
	 * 
	 */
	public TexlipseNewTexFileWizardPage(IStructuredSelection selection) {
		super("wizardPage", selection);
		setTitle("LaTeX/TeX file");
		setDescription("This wizard creates a new file with *.tex extension.");
		setFileExtension("tex");
	}
}