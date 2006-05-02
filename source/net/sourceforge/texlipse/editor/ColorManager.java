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
import org.eclipse.swt.SWT;
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
    public static final String VERBATIM  = "verb.TexColor";
	
	//public static final String TEX_WHITE  = "whi.TexColor";
	public static final String TEX_NUMBER = "num.TexColor";
	
	public static final String TEX_SPECIAL = "spe.TexColor";
    
	//The style for the syntax highlighting
	public static final String DEFAULT_STYLE = "def.TexStyle";
	
	public static final String STRING_STYLE     = "str.TexStyle";
	public static final String COMMAND_STYLE    = "cmd.TexStyle";
	public static final String CURLY_BRACKETS_STYLE = "crl.TexStyle";
	public static final String SQUARE_BRACKETS_STYLE = "sqr.TexStyle";
	public static final String EQUATION_STYLE = "equ.TexStyle";
	public static final String COMMENT_STYLE  = "com.TexStyle";
    public static final String VERBATIM_STYLE  = "verb.TexStyle";
	
	//public static final String TEX_WHITE  = "whi.TexColor";
	public static final String TEX_NUMBER_STYLE = "num.TexStyle";
	
	public static final String TEX_SPECIAL_STYLE = "spe.TexStyle";
    
    // default colors
    private static final RGB DEFAULT_DEFAULT_COLOR = new RGB(0, 0, 0);
    
    private static final RGB DEFAULT_STRING_COLOR = new RGB(255, 0, 0);
    private static final RGB DEFAULT_COMMAND_COLOR = new RGB(128, 0, 255);
    private static final RGB DEFAULT_CURLY_BRACKETS_COLOR = new RGB(3, 54, 222);
    private static final RGB DEFAULT_SQUARE_BRACKETS_COLOR = new RGB(243, 129, 37);
    private static final RGB DEFAULT_EQUATION_COLOR = new RGB(190, 100, 100);
    private static final RGB DEFAULT_COMMENT_COLOR = new RGB(190, 190, 190);
    private static final RGB DEFAULT_VERBATIM_COLOR = new RGB(80, 80, 80);
    
    //private static final RGB DEFAULT_TEX_WHITE_COLOR = new RGB(125, 125, 125);
    private static final RGB DEFAULT_TEX_NUMBER_COLOR = new RGB(255, 100, 0);
    
    private static final RGB DEFAULT_TEX_SPECIAL_COLOR = new RGB(255, 0, 0);
    
    // default styles
    private static final int DEFAULT_DEFAULT_STYLE = SWT.NORMAL;
    
    private static final int DEFAULT_STRING_STYLE = SWT.NORMAL;
    private static final int DEFAULT_COMMAND_STYLE = SWT.BOLD;
    private static final int DEFAULT_CURLY_BRACKETS_STYLE = SWT.NORMAL;
    private static final int DEFAULT_SQUARE_BRACKETS_STYLE = SWT.NORMAL;
    private static final int DEFAULT_EQUATION_STYLE = SWT.NORMAL;
    private static final int DEFAULT_COMMENT_STYLE = SWT.NORMAL;
    private static final int DEFAULT_VERBATIM_STYLE = SWT.ITALIC;
    
    //private static final RGB DEFAULT_TEX_WHITE_COLOR = new RGB(125, 125, 125);
    private static final int DEFAULT_TEX_NUMBER_STYLE = SWT.NORMAL;
    
    private static final int DEFAULT_TEX_SPECIAL_STYLE = SWT.NORMAL;
    // the color and style cache
    
    protected Map fColorTable = new HashMap(10);
    protected Map fStyleTable = new HashMap(10);
    protected Color bgColor;

    /**
     * Initialize default colors and styles to preferences.
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
        PreferenceConverter.setDefault(preferences, VERBATIM, DEFAULT_VERBATIM_COLOR);

        //PreferenceConverter.setDefault(preferences, TEX_WHITE, DEFAULT_TEX_WHITE_COLOR);
        PreferenceConverter.setDefault(preferences, TEX_NUMBER, DEFAULT_TEX_NUMBER_COLOR);

        PreferenceConverter.setDefault(preferences, TEX_SPECIAL, DEFAULT_TEX_SPECIAL_COLOR);

        preferences.setDefault(DEFAULT_STYLE, DEFAULT_DEFAULT_STYLE);

        preferences.setDefault(STRING_STYLE, DEFAULT_STRING_STYLE);
        preferences.setDefault(COMMAND_STYLE, DEFAULT_COMMAND_STYLE);
        preferences.setDefault(CURLY_BRACKETS_STYLE, DEFAULT_CURLY_BRACKETS_STYLE);
        preferences.setDefault(SQUARE_BRACKETS_STYLE, DEFAULT_SQUARE_BRACKETS_STYLE);
        preferences.setDefault(EQUATION_STYLE, DEFAULT_EQUATION_STYLE);
        preferences.setDefault(COMMENT_STYLE, DEFAULT_COMMENT_STYLE);
        preferences.setDefault(VERBATIM_STYLE, DEFAULT_VERBATIM_STYLE);

        //PreferenceConverter.setDefault(preferences, TEX_WHITE, DEFAULT_TEX_WHITE_COLOR);
        preferences.setDefault(TEX_NUMBER_STYLE, DEFAULT_TEX_NUMBER_STYLE);

        preferences.setDefault(TEX_SPECIAL_STYLE, DEFAULT_TEX_SPECIAL_STYLE);
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
     * Returns the style for some style name
     * @param styleC   one of the predefined styles
     * @return the     color object
     */
       public int getStyle(String styleC) {
               Integer style = (Integer) fStyleTable.get(styleC);
               if (style == null) {
                       style = new Integer(TexlipsePlugin.getDefault().getPreferenceStore().getInt(styleC));
                       fColorTable.put(styleC, style);
               }
               return style.intValue();
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
