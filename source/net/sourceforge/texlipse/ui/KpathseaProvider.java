/*
 * $Id$
 *
 * Copyright (c) 2008 by Christopher Hoskin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.ui;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.KpsewhichRunner;
import net.sourceforge.texlipse.builder.Kpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import org.eclipse.core.resources.IProject;
import java.io.File;
import java.io.FileFilter;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


/**
 * Adds a sub-tree to the Eclipse Project Explorer showing the Kpathsea search paths for projects
 * 
 * @author Christopher Hoskin
 *
 */
public class KpathseaProvider implements ITreeContentProvider, ILabelProvider {
	
	/**
	 * We need to provide information about the nodes in the sub-tree.
	 * Each node will be an object. ITreeNode is the interface for
	 * obtaining the common node information 
	 */
	interface ITreeNode {
		Object[] getChildren();
		boolean hasChildren();
		String getText();
		Object getParent();
		Image getImage();
	}
	
	/**
	 * 
	 * We will filter the search paths returned by kpsewhich for files
	 * with the appropriate extension. Optionally kpsewhich may search
	 * subdirectories.
	 *
	 */
	private class ExtFilter implements FileFilter {
		protected String extension;
		protected boolean folders; 
		
		public ExtFilter(String extension, boolean folders) {
			this.extension = extension;
			this.folders = folders;
		}

		public boolean accept(File pathname) {
			if (pathname.isDirectory())
				return folders;
			else
				return pathname.getName().endsWith(extension);
		}
	}
	
	/**
	 * 
	 * Path nodes are folders or files in the search path
	 *
	 */
	private class PathNode implements ITreeNode {
		protected ITreeNode parent;
		protected File path;
		protected FileFilter filter;
		
		
		public PathNode(ITreeNode parent, File path, FileFilter filter) {
			this.parent = parent;
			this.path = path;
			this.filter = filter;
		}

		public Object[] getChildren() {
			
			File[] files = path.listFiles(filter);
			PathNode[] nodes = new PathNode[files.length];
			for (int i=0; i < files.length; i++)
				nodes[i] = new PathNode(this,files[i],filter);
			return nodes;
			
		}

		public Object getParent() {
			return parent;
		}

		public String getText() {
			return path.getName();
		}

		public boolean hasChildren() {
			return (path.isDirectory()&&(getChildren().length>0));
		}

		public Image getImage() {
			// TODO Create more appropriate images
			if (path.isDirectory())
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER); 
			else if (!path.exists())
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);
			else
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
		}
		
	}
	
	/**
	 * 
	 * Override the label of top level folders to display the full path 
	 *
	 */
	private class KpathNode extends PathNode {
		
		public KpathNode(ITreeNode parent, File path, FileFilter filter) {
			super(parent, path, filter);
		}

		public String getText() {
			return path.getPath();
		}
	}
	
	/**
	 * 
	 * FileTypes (e.g. .bib, .tex, .bst) to search for
	 *
	 */
	private class FileType implements ITreeNode{
		protected String extension;
		protected TopLevel parent;
		protected Image image;

		public FileType(TopLevel parent, String extension, String imageFile) {
			this.parent = parent;
			this.extension = extension;
			this.image = TexlipsePlugin.getImage(imageFile);
		}

		public Object[] getChildren() {
			KpsewhichRunner filesearch = new KpsewhichRunner();
			try {
				Kpath[] paths = filesearch.getSearchPaths(parent.getProject(), extension);
				KpathNode[] nodes = new KpathNode[paths.length];
				for (int i=0; i<paths.length; i++)
					nodes[i] = new KpathNode(this,paths[i].path,new ExtFilter(extension,paths[i].searchChildren));
				return nodes;
			} catch (CoreException ce) {
				TexlipsePlugin.log("Can't run Kpathsea", ce);
				return null;
			}
		}

		public boolean hasChildren() {
			return (getChildren()!=null);
		}

		public String getText() {
			return extension;
		}

		public Object getParent() {
			return parent;
		}

		public Image getImage() {
			return image;
		}
	}
	
	/*
	 * This is the top level node - just a string, an image and some children
	 */
	private class TopLevel implements ITreeNode {
		protected IProject parent;
		protected FileType[] children;
		
		public TopLevel(IProject parent) {
			this.parent = parent;
			
			//Make this configurable through the GUI later?
			children = new FileType[3];
			children[0] = new FileType(this,"tex","texfile");
			children[1] = new FileType(this,"bib","bibfile");
			children[2] = new FileType(this,"bst","bibfile");
		}

		public Object[] getChildren() {
			return children;
		}
		
		public boolean hasChildren() {
			return true;
		}

		public String getText() {
			return "Search Paths";
		}

		public Object getParent() {
			return parent;
		}
		
		public IProject getProject() {
			return parent;
		}

		public Image getImage() {
			// TODO Generate a better icon
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		}
		
	}
	
	
	/*
	 * Implement ITreeContentProvider interface
	 */

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IProject)
			return new Object[] { this.new TopLevel((IProject) parentElement) };
		else if (parentElement instanceof ITreeNode)
			return ((ITreeNode) parentElement).getChildren();
		else 
			return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof ITreeNode)
			return ((ITreeNode) element).getParent();
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof ITreeNode)
			return ((ITreeNode) element).hasChildren();
		else
			return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		//When ProjectExplorer calls this function it passes WorkspaceRoot as inputElement.
		//Because our highest nodes are children of IProject
		//we never get called
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub

	}

	/*
	 * Implement ILabelProvider interface
	 */
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	
	public Image getImage(Object element) {
		if (element instanceof ITreeNode)
			return ((ITreeNode) element).getImage();
		else
			return null;
	}

	public String getText(Object element) {
		if (element instanceof ITreeNode)
			return ((ITreeNode) element).getText();
		else if (element instanceof File)
			return ((File) element).getName();
		else
			return element.toString();
	}

	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub
		
	}

}
