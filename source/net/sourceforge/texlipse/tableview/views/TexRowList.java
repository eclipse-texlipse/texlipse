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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexSelections;


/**
 * @author Esa Seuranen
 *
 * The class for holding the table's data, and all the operations that
 * can be done with the data.
 */
public class TexRowList {
    /** 
     * Constant for defining up direction for function sum(int column, int row, int direction)
     */
    public final static int SUM_UP=1;
    /** 
     * Constant for defining down direction for function sum(int column, int row, int direction)
     */
    public final static int SUM_DOWN=2;
    /** 
     * Constant for defining left direction for function sum(int column, int row, int direction)
     */
    public final static int SUM_LEFT=3;
    /** 
     * Constant for defining right direction for function sum(int column, int row, int direction)
     */
    public final static int SUM_RIGHT=4;
    
    //default number of rows upon initialization
    private final static int ROW_COUNT=10;
    
    private Vector rows = new Vector(ROW_COUNT);
    
    /**
     * Constructor
     */
    public TexRowList(){
        for (int i = 0; i < ROW_COUNT; i++)
            rows.add(i, new TexRow());  	
    }
    
    // change listeners
    private Set changeListeners = new HashSet();
    
    //Vector for holding TexRows
    public Vector getRows() { return rows; }
    
    /**
     * Adds row to the end of the row list
     * 
     * @return the added TexRow
     */
    public TexRow addRow() {
        TexRow row = new TexRow();
        rows.add(rows.size(), row);
        Iterator iterator = changeListeners.iterator();			
        while (iterator.hasNext())
            ((ITexRowListViewer) iterator.next()).addRow(row);
        
        return row;
    }
    
    /**
     * Inserts the given row to the given place(index) in the row list
     * 
     * @param index where the row is inserted
     * @param row the TexRow which is inserted
     * @return the inserted TexRow (the same as row)
     */
    public TexRow insertRow(int index,TexRow row) {
        rows.insertElementAt(row,index);
        Iterator iterator = changeListeners.iterator();			
        while (iterator.hasNext())
            ((ITexRowListViewer) iterator.next()).insertRow(row);			
        return row;
    }
    
    /**
     * Inserts new TexRow to the given place(index) in the row list
     * 
     * @param index where the row is inserted
     * @return the inserted TexRow
     */
    public TexRow insertRow(int index) {
        return insertRow(index,new TexRow());
    }
    
    /**
     * Returns the index of given row in the row list
     * 
     * @param row
     * @return index of the row (or -1 if the row is not in the row list)
     */
    public int indexOf(TexRow row){
        return rows.indexOf(row);
    }
    
    /**
     * Removes the given row (first occurrance) from the row list 
     * (or does nothing, if the row is not in the row list)
     * 
     * @param row to be removed
     */
    public void removeRow(TexRow row) {
        rows.remove(row); // FIXME now maxrowcount becomes one less, since the row isn't cleared
        Iterator iterator = changeListeners.iterator();
        while (iterator.hasNext())
            ((ITexRowListViewer) iterator.next()).removeRow(row);
    }
    
    /**
     * Notifies the changelistener, that a row has been changed
     * 
     * @param row which was changed (and should be updated in table viewer)
     */
    public void rowChanged(TexRow row) {
        Iterator iterator = changeListeners.iterator();
        while (iterator.hasNext())
            ((ITexRowListViewer) iterator.next()).updateRow(row);
    }
    
    /**
     * Removes change listener from the given viewer 
     * 
     * @param viewer
     */
    public void removeChangeListener(ITexRowListViewer viewer) {
        changeListeners.remove(viewer);
    }
    
    /**
     * Adds change listener to the given viewer 
     * 
     * @param viewer
     */
    public void addChangeListener(ITexRowListViewer viewer) {
        changeListeners.add(viewer);
    }
    
    /*
     * Reads an table item from a string representing a LaTeX table row.
     * The items separated by '&' characters (exlcluding the combnation "\&").
     *  
     * @param source LateX table row
     * @param index (of the source string) from which the search for the next item
     *   is started
     * @return the item as string 
     */
    private String readItemFromTexTableLine(String source,int index){
        int check, end = source.indexOf("&", index);
        while (end > 0) {
            check = source.lastIndexOf("\\", end);
            if (check + 1 == end)
                end = source.indexOf("&", end + 1);
            else
                break;
        }

        if (end == -1)
            return source.substring(index);
        else
            return source.substring(index, end + 1);
    }
    
    /*
     * Reads an table row from a string representing the whole LaTeX table row.
     * The rows are separeted by string "\\", and the last line does not
     * need to have "\\"
     *  
     * @param source LateX table
     * @param index (of the source string) from which the search for the next line
     *   is started
     * @return the line as string 
     */
    private String readTexTableLine(String source,int index){
        int end = source.indexOf("\\\\", index);
        if (end == -1)
            return source.substring(index);

        return source.substring(index, end + 2);
    }
    
    /**
     * Interprets the given selection as simple LaTeX table and imports
     * in to the given location. If the row is empty, then the import
     * is done to that row, otherwise a new row is created and inserted
     * and the import is done to that row.
     * 
     * Comments (i.e. "LaTeXTableStuff %ThisPartOfTheLineIsRemoved")
     * are ignored, as well as "\.*line.*" parts. 
     * 
     * @param selection TexSelection object
     * @param importAt the starting row (index) for the import
     *   
     */
    public void importSelection(TexSelections selection,int importAt){
        String line, s, item, tmps;
        int index = 0, index2, tmpi;
        int row = importAt, column;

        s = selection.getCompleteLines() + "\n";
        //added \n so that last line comment is also removed
        s = s.replaceAll("%.*\n", "\n"); //remove comments
        s = s.replaceAll("\n\r*", " "); //remove newlines

        while (index < s.length()) {
            if (row >= rows.size())
                addRow();
            //check, whether the current row is empty... if not, create a new
            // one
            if (!((TexRow) rows.get(row)).empty())
                insertRow(row);

            line = readTexTableLine(s, index);
            index += line.length();
            line = line.trim();
            while (line.startsWith("\\")) {
                tmpi = line.indexOf(" ");
                if (tmpi == -1)
                    break;
                tmps = line.substring(0, tmpi);
                if (tmps.indexOf("line") == -1)
                    break;
                line = line.substring(tmpi);
            }
            if (line.endsWith("\\\\"))
                line = line.substring(0, line.length() - 2);
            //We need this to ensure that the last empty field will be recognized (Case: &\\)
            if (line.endsWith("&")) line = line + " ";
            index2 = 0;
            column = 0;
            while ((index2 < line.length()) && (column < TexRow.COLUMNS)) {
                item = readItemFromTexTableLine(line, index2);
                index2 += item.length();
                item = item.trim();
                if (item.endsWith("&"))
                    item = item.substring(0, item.length() - 1);
                if ("".equals(item)) item = "&";
                ((TexRow) rows.get(row)).setCol(column, item);
                column++;
            }
            rowChanged((TexRow) rows.get(row));
            row++;
        }			
    }
    
    /**
     * Clears all data in the row list
     */
    public void clearAll() {
        for (int i = 0; i < rows.size(); i++) {
            ((TexRow) rows.get(i)).clear();
            rowChanged((TexRow) rows.get(i));
        }
    }
    
    /**
     * Exports the table editor's content to clipboard
     * in LaTeX table format.
     * 
     * @return string representing the whole table
     */
    public String export() {			
        String s, value = "";
        boolean first;
        
        for (int i = 0; i < rows.size(); i++) {
            TexRow row = (TexRow) rows.get(i);
            int lastCol = row.lastColumn();
            if (lastCol == -1)
                continue;
            
            first = true;
            boolean amp = false;
            for (int j = 0; j <= lastCol; j++) {
                s = row.getCol(j).trim();
                if (s.compareTo("&") == 0){
                	if (first) value += s;
                	else value += " " + s;
                	first = false;
                	amp = true;
                }
                else {
                    if (first) {
                        value += s;
                        first = false;
                    } else {
                        if (!amp) value += " & " + s;
                        else value += " " + s;
                        amp = false;
                    }
                }
            }
            value += "\\\\\n";
        }
        return value;
    }

    /**
     * Returns the table editor's content in a raw,
     * tabulated format suitable for e.g. gnuplot.
     * 
     * @return string representing the whole table
     */
    public String exportRaw() {
        String s, value = "";
        boolean first;
        
        for (int i = 0; i < rows.size(); i++) {
            TexRow row = (TexRow) rows.get(i);
            int lastCol = row.lastColumn();
            if (lastCol == -1)
                continue;
            
            first = true;
            for (int j = 0; j <= lastCol; j++) {
                s = row.getCol(j).trim();
                if (s.compareTo("&") == 0)
                    value += "\t" + s;
                else {
                    if (first) {
                        value += s;
                        first = false;
                    } else {
                        value += "\t" + s;
                    }
                }
            }
            value += "\n";
        }
        return value;        
    }
    
    /**
     * Flips the rows and columns of the table, i.e.
     *  1 2 3
     *  4 5 6
     * becomes
     *  1 4
     *  2 5
     *  3 6
     */
    public void flipRowsAndColumns() {
        String s;
        boolean swap;
        TexRow texRow;
        if (rows.size() >= TexRow.COLUMNS) {
            TexlipsePlugin
                    .stat("Flipping rows and columns in LaTeX Table View for more than "
                            + Integer.toString(TexRow.COLUMNS)
                            + " rows is not supported.");
            return;
        }
        for (int column = 0; column < TexRow.COLUMNS; column++) {
            for (int row = 0; ((row < column) && (row < rows.size())); row++) {
                swap = false;
                texRow = (TexRow) rows.get(row);
                if (texRow.getCol(column).length() > 0)
                    swap = true;
                if ((rows.size() > column)
                        && ((TexRow) rows.get(column)).getCol(row).length() > 0)
                    swap = true;

                if (swap) {
                    while (rows.size() <= column)
                        addRow();
                    s = texRow.getCol(column);
                    texRow.setCol(column, ((TexRow) rows.get(column))
                            .getCol(row));
                    ((TexRow) rows.get(column)).setCol(row, s);
                }
            }
        }
        for (int i = 0; i < rows.size(); i++)
            rowChanged((TexRow) rows.get(i));
    }
    
    /**
     * Mirrors the columns of the table, i.e.
     *  1 2 3
     *  4 5 6
     * becomes
     *  3 2 1
     *  6 5 4
     */
    public void mirrorColumns() {
        int i, j, last = 0;
        String s;
        TexRow row;
        for (i = 0; i < rows.size(); i++) {
            row = (TexRow) rows.get(i);
            for (j = last; j < TexRow.COLUMNS; j++)
                if (row.getCol(j).length() > 0)
                    last = j;
        }

        for (i = 0; i < rows.size(); i++) {
            row = (TexRow) rows.get(i);
            for (j = 0; j < (int) ((last + 1) / 2); j++) {
                s = row.getCol(j);
                row.setCol(j, row.getCol(last - j));
                row.setCol(last - j, s);
            }
            rowChanged(row);
        }			
    }
    
    /**
     * Mirrors the rows of the table, i.e.
     *  1 2 3
     *  4 5 6
     * becomes
     *  4 5 6
     *  1 2 3
     */
    public void mirrorRows() {
        int i, j, last = 0;
        String s;
        TexRow row1, row2;
        for (i = 0; i < rows.size(); i++) {
            row1 = (TexRow) rows.get(i);
            if (!(row1.empty()))
                last = i;
        }

        for (i = 0; i < ((last + 1) / 2); i++) {
            row1 = (TexRow) rows.get(i);
            row2 = (TexRow) rows.get(last - i);

            for (j = 0; j < TexRow.COLUMNS; j++) {
                s = row1.getCol(j);
                row1.setCol(j, row2.getCol(j));
                row2.setCol(j, s);
            }
            rowChanged(row1);
            rowChanged(row2);
        }			
    }
    
    /**
     * Moves given row into given location
     * 
     * @param row
     * @param toIndex
     */
    public void move(TexRow row, int toIndex) {
        int fromIndex = indexOf(row);

        //check parameters
        if ((fromIndex < 0) || (toIndex < 0) || (toIndex >= rows.size()))
            return;

        if (toIndex > fromIndex)
            toIndex--;
        removeRow(row);
        insertRow(toIndex, row);				
    }
    
    /**
     * Sums the cells in given area, defined with top left cell
     * and the areas width and height
     * 
     * @param column top left cell's column
     * @param row top left cell's row
     * @param width the are width (the number of cells)
     * @param height the are height (the number of cells)
     */
    public double sum(int column, int row, int width, int height) {
        int i, j;
        double value = 0.0;
        TexRow texRow;
        for (i = row; (i < rows.size()) && (i < row + height); i++) {
            if (i < 0)
                continue; //just being paranoid
            texRow = (TexRow) rows.get(i);
            for (j = column; (j < TexRow.COLUMNS) && (j < column + width); j++) {
                if (j < 0)
                    continue; //just being paranoid
                try {
                    value += Double.parseDouble(texRow.getCol(j));
                } catch (NumberFormatException nfe) {
                    //this is expected to happen because of text strings
                }
            }
        }
        return (value);
    }
    
    /**
     * Sums the cells in given direction from the defined cell (by column and row).
     * The cell itself is not included into the sum
     * 
     * @param column  cell's column
     * @param row top left cell's row
     * @param direction one of SUM_UP, SUM_DOWN, SUM_LEFT, SUM_RIGHT
     */
    public double sum(int column, int row, int direction){
        switch(direction){
        case SUM_UP:
            return sum(column, 0, 1, row);
        case SUM_DOWN:
            return sum(column, row + 1, 1, rows.size()-row-1);
        case SUM_LEFT:
            return sum(0, row, column, 1);
        case SUM_RIGHT:
            return sum(column+1, row, TexRow.COLUMNS-column-1, 1);
        }
        //invalid direction was given
        return 0.0;
    }
    
    /**
     * Duplicates the given row and insert the dublicate into the row list
     * (location specified by index) 
     * 
     * @param row
     * @param index
     */
    public void copy(TexRow row, int index) {
        insertRow(index, new TexRow(row));
    }
}
