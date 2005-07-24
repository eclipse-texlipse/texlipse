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

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Esa Seuranen
 *
 * A class for handling cell editing in the table viewer.
 */
public class TexCellModifier implements ICellModifier {
    //column labels
    private String columnNames[];

    //Table viewer, to which the TexCellModifier belongs to
    //private TableViewer viewer;

    //The actual data in the table (list of rows)
    private TexRowList rowList;

    /**
     * Constructor
     *  
     * @param viewer is parent TableViewer 
     * @param rowList is the table (in "list of rows" format) 
     */
    public TexCellModifier(TableViewer viewer, TexRowList rowList) {
        super();
        //this.viewer = viewer;
        this.rowList = rowList;
        columnNames = new String[TexRow.COLUMNS];
        for (int i = 0; i < TexRow.COLUMNS; i++)
            columnNames[i] = "" + (TexRow.COLUMNS - i);
    }

    /**
     * @see org.eclipse.jface.viewers.ICellModifier#canModify(java.lang.Object, java.lang.String)
     */
    public boolean canModify(Object element, String property) {
        return true;
    }

    /**
     * @see org.eclipse.jface.viewers.ICellModifier#getValue(java.lang.Object, java.lang.String)
     */
    public Object getValue(Object element, String property) {
        List cn = Arrays.asList(columnNames);
        int columnIndex = cn.indexOf(property);

        Object result = null;
        TexRow row = (TexRow) element;

        result = row.getCol(columnIndex);
        return result;
    }

    /**
     * @see org.eclipse.jface.viewers.ICellModifier#modify(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public void modify(Object element, String property, Object value) {
        List cn = Arrays.asList(columnNames);
        int columnIndex = cn.indexOf(property);

        TableItem item = (TableItem) element;
        TexRow row = (TexRow) item.getData();
        String valueString;

        valueString = ((String) value).trim();
        row.setCol(columnIndex, valueString);

        rowList.rowChanged(row);
    }
}