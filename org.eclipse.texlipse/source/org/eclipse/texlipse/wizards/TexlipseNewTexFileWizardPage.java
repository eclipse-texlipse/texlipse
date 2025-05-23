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