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

package org.eclipse.texlipse.templates;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.texlipse.properties.TexlipseProperties;


/**
 * @author Esa Seuranen
 *
 * Simple class for representing BiBTeX content type.
 */
public class BibTexContextType extends TemplateContextType {
    public static final String BIBTEX_CONTEXT_TYPE = TexlipseProperties.PACKAGE_NAME + ".templates.bibtex";
    
    /**
     * Constructor
     */
    public BibTexContextType() {
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
