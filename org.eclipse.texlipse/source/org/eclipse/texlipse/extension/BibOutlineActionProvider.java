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

package org.eclipse.texlipse.extension;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * Interface that allows to add buttons to the BibContainer Outline Page.
 * These Buttons allow interaction with the currently opened .Bib File
 * 
 * @author Manuel
 *
 */
public interface BibOutlineActionProvider {

	/**
	 * This method is used to fetch an optional Button for the .bib Outline Page.
	 * 
	 * @param treeView The {@link TreeViewer} of the outline page
	 * @param res The {@link IResource} representation of the opened file
	 * 
	 * @return the Button as {@link Action}
	 */
	public Action getAction(TreeViewer treeView, IResource res);
}
