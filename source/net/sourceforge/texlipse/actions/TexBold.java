/*
 * $Id$
 *
 * Copyright (c) 2005 by the TeXlipse Project.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions;

/**
 * Turns bold on and off of the selection.
 * 
 * @author Andrew Eisenberg
 */
public class TexBold extends AbstractTexSelectionChange {

    /* (non-Javadoc)
     * @see net.sourceforge.texlipse.actions.AbstractTexSelectionChange#getOpenString()
     */
    protected String getStartTag() {
        return "\\textbf{";
    }
}
