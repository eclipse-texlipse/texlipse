/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.bibeditor;

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
 * Color provider for BibTeX syntax highlighting. 
 * 
 * @author Oskar Ojala
 */
public class BibColorProvider {

    public static final String SINGLE_LINE_COMMENT = "slc.BibColor";
    public static final String KEYWORD = "key.BibColor";
    public static final String TYPE = "typ.BibColor";
    public static final String STRING = "str.BibColor";
    public static final String DEFAULT = "def.BibColor";
    
    private static final RGB SINGLE_LINE_COMMENT_DEFAULT_COLOR = new RGB(128, 128, 0);
    private static final RGB KEYWORD_DEFAULT_COLOR = new RGB(255, 127, 0);
    private static final RGB TYPE_DEFAULT_COLOR = new RGB(0, 0, 128);
    private static final RGB STRING_DEFAULT_COLOR = new RGB(0, 128, 0);
    private static final RGB DEFAULT_DEFAULT_COLOR = new RGB(0, 0, 0);
    
    protected Map fColorTable = new HashMap(6);
    

    /**
     * Initialize default colors to preferences.
     * This should be called only from PreferenceInitializer.
     * @param preferences preferences
     */
    public static void initializeDefaults(IPreferenceStore preferences) {
        PreferenceConverter.setDefault(preferences, SINGLE_LINE_COMMENT, SINGLE_LINE_COMMENT_DEFAULT_COLOR);
        PreferenceConverter.setDefault(preferences, KEYWORD, KEYWORD_DEFAULT_COLOR);
        PreferenceConverter.setDefault(preferences, TYPE, TYPE_DEFAULT_COLOR);
        PreferenceConverter.setDefault(preferences, STRING, STRING_DEFAULT_COLOR);
        PreferenceConverter.setDefault(preferences, DEFAULT, DEFAULT_DEFAULT_COLOR);
    }
    
    /**
     * Release all of the color resources held onto by the receiver.
     */ 
    public void dispose() {
        Iterator e = fColorTable.values().iterator();
        while (e.hasNext())
            ((Color) e.next()).dispose();
    }
    
    /**
     * Return the Color that is stored in the Color table as rgb.
     * 
     * @param rgb
     * @return The color
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
}
