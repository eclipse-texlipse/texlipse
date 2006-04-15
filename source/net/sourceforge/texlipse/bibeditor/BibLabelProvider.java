/* 
 * Copyright (c) 2006 by Kai Krueger.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibeditor;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.ReferenceEntry;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class BibLabelProvider extends LabelProvider {
	
	/**
	 *  Returns an image for the given element.
	 *  
	 * @return returns an Image corresponding to the format if there is
	 * a file available for this Bibtex entry. 
	 */
	public Image getImage(Object element) {
	  if (element instanceof ReferenceEntry) {
		  ReferenceEntry re = (ReferenceEntry) element;
		  if (re.refFile != null) {
			  String fileName = re.refFile.getName();
			  String format = "";
			  if (fileName.lastIndexOf(".") > 0)
				  format = fileName.substring(fileName.lastIndexOf("."));
			  if (format.equalsIgnoreCase("pdf"))
				  return TexlipsePlugin.getImage("pdf");
			  if (format.equalsIgnoreCase("ps"))
				  return TexlipsePlugin.getImage("ps");
			  if (format.equalsIgnoreCase("dvi"))
				  return TexlipsePlugin.getImage("dvi");
			  if (format.equalsIgnoreCase("djvu"))
				  return TexlipsePlugin.getImage("djvu");
			  return TexlipsePlugin.getImage("file");
		  }
	  }
	  return null;
	}
	
	/**
	 * Returns the text description of the element. That is the key element 
     * for a ReferenceEntry, or the string it self for a string.
     * 
	 * @return the text to view at the given element
	 */
	public String getText(Object element) {
		return element.toString();
	}
	
	/**
     * Not used atm. Simply return false.
     * 
     * @return true, if the given property affects the given element
     */
	public boolean isLabelProperty(Object element, String property) {
        return false;
    }

}
