/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;


/**
 * Color cache for the Latex file editor.
 * These colors will be used in syntax coloring, 
 * the text background, for example, is defined in the editor preferences.
 * 
 * @author Antti Pirinen
 * @author Kimmo Karlsson
 */
public class ColorManager {
    
	// The colors that are used for tex source syntax highlighting
    // ".TexColor" -suffix is there because these names are used also in the preferences
	public static final String DEFAULT = "def.TexColor";
	
	public static final String STRING     = "str.TexColor";
	public static final String COMMAND    = "cmd.TexColor";
	public static final String CURLY_BRACKETS = "crl.TexColor";
    public static final String SQUARE_BRACKETS = "sqr.TexColor";
	public static final String EQUATION = "equ.TexColor";
	public static final String COMMENT  = "com.TexColor";
	
	//public static final String TEX_WHITE  = "whi.TexColor";
	public static final String TEX_NUMBER = "num.TexColor";
	
	public static final String TEX_SPECIAL = "spe.TexColor";
    
    // default colors
    private static final RGB DEFAULT_DEFAULT_COLOR = new RGB(0, 0, 0);
    
    private static final RGB DEFAULT_STRING_COLOR = new RGB(255, 0, 0);
    private static final RGB DEFAULT_COMMAND_COLOR = new RGB(128, 0, 255);
    private static final RGB DEFAULT_CURLY_BRACKETS_COLOR = new RGB(3, 54, 222);
    private static final RGB DEFAULT_SQUARE_BRACKETS_COLOR = new RGB(243, 129, 37);
    private static final RGB DEFAULT_EQUATION_COLOR = new RGB(190, 100, 100);
    private static final RGB DEFAULT_COMMENT_COLOR = new RGB(190, 190, 190);
    
    //private static final RGB DEFAULT_TEX_WHITE_COLOR = new RGB(125, 125, 125);
    private static final RGB DEFAULT_TEX_NUMBER_COLOR = new RGB(255, 100, 0);
    
    private static final RGB DEFAULT_TEX_SPECIAL_COLOR = new RGB(255, 0, 0);
    
	// the color cache
	protected Map fColorTable = new HashMap(10);


    /**
     * Initialize default colors to preferences.
     * This should be called only from PreferenceInitializer.
     * @param preferences preferences
     */
    public static void initializeDefaults(IPreferenceStore preferences) {
        PreferenceConverter.setDefault(preferences, DEFAULT, DEFAULT_DEFAULT_COLOR);

        PreferenceConverter.setDefault(preferences, STRING, DEFAULT_STRING_COLOR);
        PreferenceConverter.setDefault(preferences, COMMAND, DEFAULT_COMMAND_COLOR);
        PreferenceConverter.setDefault(preferences, CURLY_BRACKETS, DEFAULT_CURLY_BRACKETS_COLOR);
        PreferenceConverter.setDefault(preferences, SQUARE_BRACKETS, DEFAULT_SQUARE_BRACKETS_COLOR);
        PreferenceConverter.setDefault(preferences, EQUATION, DEFAULT_EQUATION_COLOR);
        PreferenceConverter.setDefault(preferences, COMMENT, DEFAULT_COMMENT_COLOR);

        //PreferenceConverter.setDefault(preferences, TEX_WHITE, DEFAULT_TEX_WHITE_COLOR);
        PreferenceConverter.setDefault(preferences, TEX_NUMBER, DEFAULT_TEX_NUMBER_COLOR);

        PreferenceConverter.setDefault(preferences, TEX_SPECIAL, DEFAULT_TEX_SPECIAL_COLOR);
    }
    
    /**
     * Returns the color object for some color.
     * The color object may not represent the same color as the parameter.
     * @param rgb 	one of the predefined colors
     * @return the 	color object
     */
	public Color getColor(String rgb) {
		Color color = (Color) fColorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(),
                    PreferenceConverter.getColor(TexlipsePlugin.getDefault().getPreferenceStore(), rgb));
			fColorTable.put(rgb, color);
		}
		return color;
	}

    /**
     * Dispose the color provider.
     */
	public void dispose() {
		Iterator e = fColorTable.values().iterator();
		while (e.hasNext())
			((Color) e.next()).dispose();
	}
}
