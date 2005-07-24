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

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;

/**
 * @author Esa Seuranen
 * 
 * A key listener class for individual cells in the table viewer.
 * 
 * The main purpose for this class is to enable better (more usable) controls
 * when editing table entries in the table viewer (TableViewer), which, as the
 * name suggest, is not originally designed to be a table editor. However, this
 * class enables fast moving around between neighboring cells by using
 * CTRL+ArrowKeys.
 */
public class TexCellListener implements KeyListener {
    /** Table Viewer to which listener belongs */
    private TableViewer viewer;

    /** Column, which the listener listens
     * This is here, because there seems to be no way
     * to extract the "current column" information from the viewer...
     */
    private int column;

    /** The underlying datastructure, TexRowList, of the whole table */
    private TexRowList texRowList;

    /**
     * Constructor
     * 
     * DO NOT USE THIS, use the other constructor(s) instead
     */
    private TexCellListener() {
    }

    /**
     * Constructor for the cell listener.
     * 
     * Column information is necessary, so that moving around neighboring cell
     * listener (i.e. activating the editing controls) can be done.
     * 
     * @param viewer
     *            to which the cell listener belongs to
     * @param texRowList
     *            the underlying datastructure for the whole table
     * @param column
     *            which the cell listener listens to
     */
    public TexCellListener(TableViewer viewer, TexRowList texRowList, int column) {
        this.viewer = viewer;
        this.texRowList = texRowList;
        this.column = column;
    }

    /**
     * KeyListener method from KeyListener Interface
     * 
     * Determines the actions to be taken, when a key is pressed. That means
     * either moving the editing controls to neigboring cell, if a)
     * CTRL+ArrowKey was pressed b) neighboring cell in that direction exists or
     * calculating a sum of cells in given direction (and copying it to
     * clipboard) c) CTRL+KEYPAD_8,_2,_4 or _6 (up, down, left, right) is
     * pressed
     * 
     * @param e key event that occured
     */
    public void keyPressed(KeyEvent e) {
        if ((e.stateMask & (SWT.CTRL | SWT.ALT | SWT.SHIFT)) != SWT.CTRL)
            return;

        int row = viewer.getTable().getSelectionIndex();
        int columns = viewer.getTable().getColumnCount();
        int rows = viewer.getTable().getItemCount();
        // FIXME redundant test
        if ((e.stateMask & (SWT.CTRL | SWT.ALT | SWT.SHIFT)) == SWT.CTRL) {
            //TexRow texRow = (TexRow) texRowList.getRows().get(row);
            double sum = 0.0;
            switch (e.keyCode) {
            case SWT.ARROW_UP:
                if (row > 0)
                    viewer.editElement(viewer.getElementAt(row - 1), column);
                break;
            case SWT.ARROW_DOWN:
            	/*
                if (row < rows - 1) // TODO else { add one row to the model }
                    viewer.editElement(viewer.getElementAt(row + 1), column);
                    */
            	if (row >= rows - 1)
            		texRowList.addRow();
            	viewer.editElement(viewer.getElementAt(row + 1), column);
                break;
            case SWT.ARROW_LEFT:
                if (column > 0)
                    viewer.editElement(viewer.getElementAt(row), column - 1);
                break;
            case SWT.ARROW_RIGHT:
                if (column < columns - 1)
                    viewer.editElement(viewer.getElementAt(row), column + 1);
                break;
            case SWT.KEYPAD_8:
                sum = texRowList.sum(column, row, TexRowList.SUM_UP);
                if(row>0) {
                	viewer.editElement(viewer.getElementAt(row - 1), column);
            		((TexRow)texRowList.getRows().get(row)).setCol(column,
            				((int)sum==sum)?Integer.toString((int)sum):Double.toString(sum));
                	viewer.editElement(viewer.getElementAt(row), column);
                }
                break;
            case SWT.KEYPAD_2:
                sum = texRowList.sum(column, row, TexRowList.SUM_DOWN);
            	
            	if(row<texRowList.getRows().size()-1) {
            		viewer.editElement(viewer.getElementAt(row + 1), column);
            		((TexRow)texRowList.getRows().get(row)).setCol(column,
            				((int)sum==sum)?Integer.toString((int)sum):Double.toString(sum));
            		viewer.editElement(viewer.getElementAt(row), column);
            	}
                break;
            case SWT.KEYPAD_4:
                sum = texRowList.sum(column, row, TexRowList.SUM_LEFT);
            	if(column>0) {
            		viewer.editElement(viewer.getElementAt(row), column-1);
            		((TexRow)texRowList.getRows().get(row)).setCol(column,
            				((int)sum==sum)?Integer.toString((int)sum):Double.toString(sum));
            		viewer.editElement(viewer.getElementAt(row), column);
            	}
                break;
            case SWT.KEYPAD_6:
                sum = texRowList.sum(column, row, TexRowList.SUM_RIGHT);
            	if(column<TexRow.COLUMNS-1) {
            		viewer.editElement(viewer.getElementAt(row), column+1);
            		((TexRow)texRowList.getRows().get(row)).setCol(column,
        				((int)sum==sum)?Integer.toString((int)sum):Double.toString(sum));
            		viewer.editElement(viewer.getElementAt(row), column);
            	}
                break;
            }
        }
    }

    /**
     * KeyListener method from KeyListener Interface
     * 
     * Determines the actions to be taken, when a key is released
     * (which is absolutely nothing).
     * 
     * @param e key event, that occurred
     */
    public void keyReleased(KeyEvent e) {
    }
}