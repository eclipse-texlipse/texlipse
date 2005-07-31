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
 * A class representing a one row in LaTeX Table View Plugin
 */
public class TexRow {
    //the amount of columns, not changable in the current implementation
    public final static int COLUMNS = 32;

    //array of string for holding all row data
    private String col[] = new String[COLUMNS];

    /**
     * Constructor, initial values for the columns are ""
     *  
     */
    public TexRow() {
        for (int i = 0; i < COLUMNS; i++)
            col[i] = "";
    }

    /**
     * Constructor, the initial values for the columns are copied from the given
     * row
     * 
     * @param row
     *            TexRow which is copied (duplicated)
     */
    public TexRow(TexRow row) {
        for (int i = 0; i < COLUMNS; i++)
            col[i] = row.getCol(i);
    }

    /**
     * Gets the string in given column
     * 
     * @param index
     *            the column
     * @return data in given column or "" if the index is invalid
     */
    public String getCol(int index) {
        if ((index < 0) || (index >= COLUMNS))
            return ("");
        else
            return (col[index]);
    }

    /**
     * Sets the string in given column. Does nothing, if the index is invalid.
     * 
     * @param index
     *            The column to be set
     * @param val
     *            The value the given column receives
     */
    public void setCol(int index, String val) {
        if ((index >= 0) || (index < COLUMNS))
            col[index] = val;
    }

    /**
     * Check, whether the entire row is empty
     * 
     * @return true if all columns are empty (or they contain only whitespaces),
     *         false otherwise
     */
    public boolean empty() {
        for (int i = 0; i < COLUMNS; i++)
            if (col[i].trim().length() > 0)
                return false;
        return true;
    }

    /**
     * Returns the number of the last column of this row containing data
     * 
     * @return Last column with data (indexed from 0) or -1 if the row is empty
     */
    public int lastColumn() {
    	int lastCol = -1;
        for (int i = 0; i < COLUMNS; i++)
            if (col[i].trim().length() > 0)
                lastCol = i;    	
    	return lastCol;
    }
    
    /**
     * Clears all columns (i.e. sets them to "")
     */
    public void clear() {
        for (int i = 0; i < COLUMNS; i++)
            col[i] = "";
    }
}