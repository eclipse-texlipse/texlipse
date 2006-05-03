/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.model;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * @author Boris von Loesch
 *
 */
public class TexCommandEntry extends AbstractEntry {

    // Type of paramter
    public final static int MAND_PARAMETER = 1;
    public final static int BRACE_PARAMETER = 2;
    public final static int OPT_PARAMETER = 3;

    // Type of context
    public final static int NORMAL_CONTEXT = 1;
    public final static int PREAMBLE_CONTEXT = 2;
    public final static int MATH_CONTEXT = 3;
    public final static int NUMBER_OF_CONTEXTS = 3;

    /**
     * A descriptive Infotext
     */
    public String info;

    /**
     * Number of arguments (including optional arguments)
     */
    public int arguments;

    /**
     * Type of parameter (mandatory, optional or in parenthesis)
     */
    public int[] parameter;

    /**
     * Type of context for this command (normal, preamble or math)
     */
    public int context;

    /**
     * Name of the package in which the command is defined
     */
    public String depend;

    /**
     * Image of the command (useful for math symbols)
     */
    public ImageDescriptor imageDesc;
    
    private Image image;

    /**
     * Creates a new TexCommandEntry
     * 
     * @param _key Unique key-Value
     * @param _info A desriptive Info text
     * @param _arguments
     * @param _parameter
     * @param _context
     */
	public TexCommandEntry(String _key, String _info, int _arguments, 
			int[] _parameter, int _context) {
        this.key = _key;
        this.info = _info;
        this.arguments = _arguments;
        this.parameter = _parameter;
        this.context = _context;
    }

    /**
     * Creates a new TexCommandEntry in the normal context with only mandatory
     * paramters
     * 
     * @param _key Unique key-Value
     * @param _info A desriptive Info text
     * @param _arguments Number of mandatory parameters
     */
    public TexCommandEntry(String _key, String _info, int _arguments) {
        this.key = _key;
        this.info = _info;
        this.arguments = _arguments;
        this.context = NORMAL_CONTEXT;
        parameter = new int[_arguments];
        for (int i = 0; i < _arguments; i++)
            parameter[i] = MAND_PARAMETER;
    }

    public TexCommandEntry(String _key, String _info, int _arguments, int _context) {
        this(_key, _info, _arguments);
        this.context = _context;
    }

    /**
     * Creates a new math TexCommand with zero arguments (e.g. a symbol)
     * 
     * @param _key
     * @param _info
     * @param _imageName Name of an image of this command
     */
    public TexCommandEntry(String _key, String _info, String _imageName) {
        this(_key, _info, 0, MATH_CONTEXT);
        this.imageDesc = TexlipsePlugin.getImageDescriptor(_imageName);
    }

    /**
     * Creates a new math TexCommand 
     * 
     * @param _key
     * @param _info Descriptive info text
     * @param _arguments Number of mandatory arguments
     * @param _imageName Name of an image of this command
     */
    public TexCommandEntry(String _key, String _info, int _arguments, String _imageName) {
        this(_key, _info, _arguments, MATH_CONTEXT);
        this.imageDesc = TexlipsePlugin.getImageDescriptor(_imageName);
    }

    /**
     * Copy construktor
     * 
     * @param c
     */
    public TexCommandEntry(TexCommandEntry c) {
        this(c.key, c.info, c.arguments, c.context);
        parameter = (int[]) c.parameter.clone();
        depend = c.depend;
        imageDesc = c.imageDesc;
    }
    
    public boolean equals(Object object) {
        if (object instanceof TexCommandEntry) {
            TexCommandEntry obj = (TexCommandEntry) object;
            //TODO this is no true equals method...
            if (obj.key.equals(key) && obj.info.equals(info) && obj.arguments == arguments 
                    && ((obj.depend == null && depend == null) || obj.depend.equals(depend))
                    && obj.imageDesc == imageDesc && obj.parameter.length == parameter.length)
                return true;
            else
                return false;
        } else {
            return super.equals(object);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object ae) {
        int c = context - ((TexCommandEntry) ae).context;
        if (c != 0)
            return c;
        else
            return key.compareTo(((AbstractEntry) ae).key);
    }
    
    /**
     * If exists returns an image for this command
     * @return image or null if no image exists
     */
    public Image getImage(){
        if (image != null) return image;
        else if (imageDesc != null)
            image = imageDesc.createImage();
        return image;
    }

}
