/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.tableview.views;

/**
 * @author Esa Seuranen
 * 
 * An interface for linkin together table viewer and the actual row information
 * in the table.
 * 
 * Table viewer's content provider implements this interface
 */
public interface ITexRowListViewer {
    /**
     * Inserts row into the table viewer. The location is determined by the
     * row's actual location in the data structure.
     * 
     * @param row
     *            to be inserted into the table viewer
     */
    public void insertRow(TexRow row);

    /**
     * Adds new row to the end of the table viewer.
     * 
     * @param row
     *            to be added (appended) to the table viewer
     */
    public void addRow(TexRow row);

    /**
     * Removes the given row from the table viewer.
     * 
     * @param row
     *            to be removed from the table viewer
     */
    public void removeRow(TexRow row);

    /**
     * Notifies the table viewer, that given row has been changes and should be
     * updated (redrawn).
     * 
     * @param row
     *            to be updated
     */
    public void updateRow(TexRow row);
}