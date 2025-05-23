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

import java.util.ArrayList;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.editor.partitioner.FastLaTeXPartitionScanner;
import org.eclipse.texlipse.model.OutlineNode;


/**
 * Label provider for the OutlineNodes. The getName() method of the
 * OutlineNodes is used to get the label for a node. 
 * 
 * All the for nodes of type ENVIRONMENT the name is used to find
 * the right image. 
 * 
 * If no image is found for a node, default_outline.gif is used.
 * 
 * The images are loaded and disposed by the TexlipsePlugin.
 *
 * @author Laura Takkinen, Taavi Hupponen, Boris von Loesch
 *  
 */
public class TexLabelProvider extends LabelProvider {

	/**
	 * Returns an image for the given element.
     * 
     * @return the image to view at the given element
	 */
	public Image getImage(Object element) {
		OutlineNode node = (OutlineNode) element;
		Image image;
		
		switch(node.getType()) {
		case OutlineNode.TYPE_PREAMBLE:
			image = TexlipsePlugin.getImage("preamble");
        	break;
		case OutlineNode.TYPE_PART:
            image = TexlipsePlugin.getImage("part");
        	break;
		case OutlineNode.TYPE_CHAPTER:
            image = TexlipsePlugin.getImage("chapter");
			break;
		case OutlineNode.TYPE_SECTION:
			image = TexlipsePlugin.getImage("section");
			break;
		case OutlineNode.TYPE_SUBSECTION:
			image = TexlipsePlugin.getImage("subsection");
			break;
		case OutlineNode.TYPE_SUBSUBSECTION:
			image = TexlipsePlugin.getImage("subsubsection");
			break;
        case OutlineNode.TYPE_PARAGRAPH:
            image = TexlipsePlugin.getImage("paragraph");
        	break;
		case OutlineNode.TYPE_ENVIRONMENT:
			image = getEnvImage(node.getName());
			break;
		case OutlineNode.TYPE_LABEL:
		    image = TexlipsePlugin.getImage("label");
		    break;
		default:
			image = TexlipsePlugin.getImage("default_outline");
		}
		if (image == null) {
			image = TexlipsePlugin.getImage("default_outline");
		}
		return image;
		
	}

	/**
     * Returns the text description of the element. That is element 
     * name for OutlineNode.
     * 
	 * @return the text to view at the given element
	 */
	public String getText(Object element) {
	    OutlineNode node = (OutlineNode)element;
	    String text = node.getName();
	    if (node.hasChildren()) { 
            ArrayList<OutlineNode> childs = node.getChildren();
            //If first child is a label, add it to the name of the element
	        if (childs.get(0).getType() == OutlineNode.TYPE_LABEL) {
	            text = text + " (L: " + childs.get(0).getName() + ")";
	        }
	    }
		return text;
	}

    private static Image getEnvImage(String envName) {
        Image image = TexlipsePlugin.getImage(envName);
        if (image == null && FastLaTeXPartitionScanner.isMathEnv(envName)) {
            //Return formula image if math environment
            image = TexlipsePlugin.getImage("formula");
        }
        return image;
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
