package net.sourceforge.texlipse.extension;

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
