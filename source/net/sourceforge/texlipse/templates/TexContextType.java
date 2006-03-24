/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.templates;

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;


/**
 * Simple class for representing TeX content type.
 * 
 * @author Esa Seuranen
 * @author Oskar Ojala
 */
public class TexContextType extends TemplateContextType {
    public static final String TEX_CONTEXT_TYPE = TexlipseProperties.PACKAGE_NAME + ".templates.tex";
    public static final String MATH_CONTEXT_TYPE = TexlipseProperties.PACKAGE_NAME + ".templates.math";
    
    /**
     * Constructor
     */
    public TexContextType() {
        addGlobalResolvers();
    }
    
    /*
     * All default solvers provided by eclipse are enabled
     */
    private void addGlobalResolvers() {
        addResolver(new GlobalTemplateVariables.Cursor());
        addResolver(new GlobalTemplateVariables.WordSelection());
        addResolver(new GlobalTemplateVariables.LineSelection());
        addResolver(new GlobalTemplateVariables.Dollar());
        addResolver(new GlobalTemplateVariables.Date());
        addResolver(new GlobalTemplateVariables.Year());
        addResolver(new GlobalTemplateVariables.Time());
        addResolver(new GlobalTemplateVariables.User());
    }
}
