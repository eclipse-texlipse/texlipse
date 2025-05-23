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

package org.eclipse.texlipse.tableview.views;

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