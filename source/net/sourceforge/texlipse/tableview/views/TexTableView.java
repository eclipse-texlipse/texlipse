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

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexSelections;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * @author Esa Seuranen
 * 
 * The main class for LaTeX Table Editor view
 */

public class TexTableView extends ViewPart {
    //viewer of the table
    private TableViewer viewer;

    //right click menu
    private Menu menu;

    //data of the table
    private TexRowList rowList = new TexRowList();

    //headers for the columns
    private String columnNames[];

    /**
     * The content provider class is responsible for
     * providing objects to the view. It can wrap
     * existing objects in adapters or simply return
     * objects as-is.
     */
    class ViewContentProvider implements IStructuredContentProvider,
            ITexRowListViewer {
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            if (newInput != null) {
                ((TexRowList) newInput).addChangeListener(this);
            }
            if (oldInput != null)
                ((TexRowList) oldInput).removeChangeListener(this);
        }

        public void dispose() {
            rowList.removeChangeListener(this);
        }

        public Object[] getElements(Object parent) {
            return rowList.getRows().toArray();
        }

        public void addRow(TexRow row) {
            viewer.add(row);
        }

        public void insertRow(TexRow row) {
            viewer.insert(row, rowList.indexOf(row));
        }

        public void removeRow(TexRow row) {
            viewer.remove(row);
        }

        public void updateRow(TexRow row) {
            viewer.update(row, null);
        }
    }

    /**
     * The label provider class is responsible for
     * providing the lables for the objects 
     */
    class ViewLabelProvider extends LabelProvider implements
            ITableLabelProvider {
        public String getColumnText(Object obj, int index) {
            return ((TexRow) obj).getCol(index);
        }

        public Image getColumnImage(Object obj, int index) {
            return (null);
            //return getImage(obj);
        }

        public Image getImage(Object obj) {
            return (null);
            //return PlatformUI.getWorkbench().
            //		getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
        }
    }

    /**
     * The constructor.
     */
    public TexTableView() {
        columnNames = new String[TexRow.COLUMNS];
        for (int i = 0; i < TexRow.COLUMNS; i++)
            columnNames[i] = "" + (TexRow.COLUMNS - i);
    }

    /**
     * Creates the table component attaching it to the given composite object
     * 
     * @param parent composite object
     * @return The created table component
     */
    public Table createTable(Composite parent) {
        int style = SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.HIDE_SELECTION | SWT.FULL_SELECTION;

        Table table = new Table(parent, style);

        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.grabExcessVerticalSpace = true;
        gridData.horizontalSpan = 3;
        table.setLayoutData(gridData);

        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        table.setToolTipText(TexlipsePlugin.getResourceString("tableviewTableTooltip"));

        TableColumn column;
        for (int i = 0; i < TexRow.COLUMNS; i++) {
            column = new TableColumn(table, SWT.LEFT, 0);
            column.setText(columnNames[i]);
            column.setWidth(50);
        }

        //The way to add listener to column, so that rows are sorted when header is clicked
        /*
         column.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
         tableViewer.setSorter(new TexRowSorter());
         }
         });
         */

        menu = new Menu(parent);
        MenuItem mi;
        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewInsertRow"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TexRow row = (TexRow) ((IStructuredSelection) viewer
                        .getSelection()).getFirstElement();
                int index = -1;
                if (row != null)
                    index = rowList.indexOf(row);
                if (index != -1)
                    rowList.insertRow(index);
                else
                    rowList.addRow(); // FIXME this is probably never executed
            }
        });
        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewDeleteRow"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TexRow row = (TexRow) ((IStructuredSelection) viewer
                        .getSelection()).getFirstElement();
                if (row != null) {
                    rowList.removeRow(row);
                }
            }
        });
        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewClearAll"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                rowList.clearAll();
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewRowUp"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TexRow row = (TexRow) ((IStructuredSelection) viewer
                        .getSelection()).getFirstElement();
                if (row != null) {
                    rowList.move(row, rowList.indexOf(row) - 1);
                }
            }
        });
        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewRowDown"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TexRow row = (TexRow) ((IStructuredSelection) viewer
                        .getSelection()).getFirstElement();
                if (row != null) {
                    rowList.move(row, rowList.indexOf(row) + 2);
                }
            }
        });
        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewDuplicateRow"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                TexRow row = (TexRow) ((IStructuredSelection) viewer
                        .getSelection()).getFirstElement();
                if (row != null) {
                    rowList.copy(row, rowList.indexOf(row));
                }
            }
        });

        new MenuItem(menu, SWT.SEPARATOR);

        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewEditorImport"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                //Get selection from texteditor
                IEditorPart targetEditor = TexlipsePlugin
                        .getCurrentWorkbenchPage().getActiveEditor();

                if (!(targetEditor instanceof ITextEditor)) {
                    return;
                }
                TexSelections selection = new TexSelections(
                        (ITextEditor) targetEditor);
                TexRow row = (TexRow) ((IStructuredSelection) viewer
                        .getSelection()).getFirstElement();
                int index = 0;
                if (row != null)
                    index = rowList.indexOf(row);
                rowList.importSelection(selection, index);
            }
        });

        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewEditorExport"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String value = rowList.export();

                //transfer string to clipboard
                Clipboard cb = new Clipboard(null);
                TextTransfer textTransfer = TextTransfer.getInstance();
                cb.setContents(new Object[] { value },
                        new Transfer[] { textTransfer });
            }
        });

        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewRawExport"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String value = rowList.exportRaw();

                //transfer string to clipboard
                Clipboard cb = new Clipboard(null);
                TextTransfer textTransfer = TextTransfer.getInstance();
                cb.setContents(new Object[] { value },
                        new Transfer[] { textTransfer });
            }
        });

        
        new MenuItem(menu, SWT.SEPARATOR);

        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewFlipRows"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                rowList.flipRowsAndColumns();
            }
        });
        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewMirrorColumns"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                rowList.mirrorColumns();
            }
        });
        mi = new MenuItem(menu, SWT.SINGLE);
        mi.setText(TexlipsePlugin.getResourceString("tableviewMirrorRows"));
        mi.setEnabled(true);
        mi.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                rowList.mirrorRows();
            }
        });

        table.setMenu(menu);

        return (table);
    }

    /**
     * This is a callback that will allow us
     * to create the viewer and initialize it.
     */
    public void createPartControl(Composite parent) {
        // Create a composite to hold the children
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.FILL_BOTH);
        parent.setLayoutData(gridData);

        //only one component...
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 4;
        parent.setLayout(layout);

        Table table = createTable(parent);

        viewer = new TableViewer(table);

        viewer.setUseHashlookup(true);
        viewer.setColumnProperties(columnNames);

        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setInput(rowList);

        CellEditor[] editors = new CellEditor[TexRow.COLUMNS];

        //ICellEditorListeners
        for (int i = 0; i < TexRow.COLUMNS; i++) {
            editors[i] = new TextCellEditor(table);
            ((Text) editors[i].getControl()).setTextLimit(256);
            editors[i].getControl().addKeyListener(
                    new TexCellListener(viewer, rowList, i));
        }

        // Assign the cell editors to the viewer 
        viewer.setCellEditors(editors);

        // Set the cell modifier for the viewer
        viewer.setCellModifier(new TexCellModifier(viewer, rowList));

        // Set the default sorter for the viewer 
        //viewer.setSorter(new LaTeXRowSorter(LaTeXRowSorter.COLUMN_1));
    }

    /**
     * Shows user a message
     */
    private void showMessage(String message) {
        MessageDialog.openInformation(viewer.getControl().getShell(),
        		TexlipsePlugin.getResourceString("tableviewTableTitle"), message);

    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    //	public void dispose(){
    //		super.dispose();
    //	}
}