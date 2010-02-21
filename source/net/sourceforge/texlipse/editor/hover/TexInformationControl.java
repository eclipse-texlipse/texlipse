/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor.hover;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.model.AbstractEntry;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.model.ReferenceManager;
import net.sourceforge.texlipse.model.TexCommandEntry;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * This class creates a informative hover for commands and BibTex entries.
 * 
 * @author Boris von Loesch
 * @author Oskar Ojala
 */
public class TexInformationControl implements IInformationControl,
        IInformationControlExtension {
    
    private AbstractEntry entry = null;
    private ReferenceManager refMana;
    private IDocument document;
    private TexEditor editor;
    private Image image;
    private Composite imageComposite;
    private Composite textComposite;
    private Shell shell;
    private int maxWidth;
    // private int maxHeight;
    private Display display;
    private boolean hasImage = false;
    // private ScrolledFormText hoverText;
    private StyledText hoverText;

    public TexInformationControl(TexEditor editor, Shell container) {
        this.editor = editor;
        document = editor.getTexDocument();
        refMana = editor.getDocumentModel().getRefMana();
        shell = new Shell(container, SWT.NO_FOCUS | SWT.ON_TOP | SWT.MODELESS);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 3;
        layout.marginWidth = 3;
        shell.setLayout(layout);
        display = shell.getDisplay();
        shell.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
    }

    /**
     * Creates a composite with a ScrolledFormText to display the info text
     */
    private void initTextBox() {
        textComposite = new Composite(shell, SWT.NONE);
        textComposite.setLayout(new FillLayout());
        GridData gdata = new GridData(SWT.FILL, SWT.TOP, true, false);
        textComposite.setLayoutData(gdata);
        hoverText = new StyledText(textComposite, SWT.WRAP);
        hoverText.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
    }

    /**
     * Creates a composites which contains an image of the command
     */
    private void createImageComp() {
        this.imageComposite = new Composite(shell, SWT.NONE);
        imageComposite.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
        imageComposite.addPaintListener(new PaintListener() {
            public void paintControl(PaintEvent e) {
                try {
                    GC gc = e.gc;
                    gc.drawRectangle(0, 0, e.width - 1, e.height - 1);
                    if (hasImage) {
                        gc.drawImage(image, 2, 2);
                    }
                } catch (Exception ex) {
                    TexlipsePlugin.log("TexInformationControl: ", ex);
                }
            }
        });
        GridData data = new GridData();
        data.grabExcessHorizontalSpace = false;
        data.grabExcessVerticalSpace = false;
        data.verticalAlignment = SWT.TOP;
        if (image != null) {
            data.widthHint = image.getBounds().width + 4;
            data.heightHint = image.getBounds().height + 4;
        }
        imageComposite.setLayoutData(data);
        imageComposite.pack();
    }

    /**
     * Set the text of the ScrolledFormText for a command help
     * 
     * @param text
     */
    private void setCommandText(String text) {
        StyleRange bold = new StyleRange();
        bold.fontStyle = SWT.BOLD;
        int endLine = text.indexOf('\n');
        int endBold = endLine >= 0 ? endLine : text.length();
        hoverText.append(text.substring(0, endBold));
        bold.start = 0;
        bold.length = endBold;
        hoverText.setStyleRange(bold);
        if (endLine >= 0) {
            hoverText.append(text.substring(endLine));
        }
    }

    /**
     * Retrieves the argument from the given string containing
     * a full TeX command
     * 
     * @param text The TeX command
     * @return The argument without braces
     */
    private static String getArgument(String text) {
        int begin = text.indexOf('{');
        int end = text.indexOf('}');

        // "{" has to be to the first character of the text
        if (begin > -1 && end > begin) {
            return text.substring(begin + 1, end);
        }
        return "";
    }

    /**
     * Retrieves the command from the given string containing
     * a full TeX command
     * 
     * @param text The TeX command in the form <code>command{arg}</code>
     * @return <code>command</code>
     */
    private static String getCommand(String text) {
        int begin = text.indexOf('{');

        if (begin > -1) {
            return text.substring(0, begin);
        }
        return text;
    }

    /**
     * Sets the hover for a reference to a BibTeX entry
     * 
     * @param ref The reference
     */
    private void setBibHover(String ref) {
        ReferenceEntry bibentry = refMana.getBib(ref);
        if (bibentry != null) {
            entry = bibentry;
            initTextBox();
            hoverText.setText(bibentry.info);
        }
    }

        
    /**
     * Sets the hover for a reference to a label
     * 
     * @param labelName The name of the label that's referred to
     * @return true, if label exist
     */
    private boolean setRefHover(String labelName) {
        
        ReferenceEntry label = refMana.getLabel(labelName);
        if (label != null) {
            entry = label;
            initTextBox();
            
            hoverText.setText(label.info);
            return true;
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.jface.text.IInformationControl#setInformation(java.lang.String)
     */
    public void setInformation(String information) {
        entry = null;
        hasImage = false;
        // Only processing of commands
        if (information.startsWith("\\")) {
            String command = information.substring(1);
            if (command.indexOf("ref") > -1 && command.indexOf("{") > -1) {
                if (!setRefHover(getArgument(command)))
                    return; 
            } else {
                TexCommandEntry comEntries = refMana.getEntry(getCommand(command));
                if (comEntries != null) {
                    entry = comEntries;
                    if (comEntries.imageDesc != null) {
                        hasImage = true;
                        image = comEntries.getImage();
                        createImageComp();
                    }
                    initTextBox();
                    if (comEntries.info != null) {
                        setCommandText(comEntries.info);
                    }
                } else
                    return;
            }
            shell.pack();
            int w = shell.getSize().x;
            int h = shell.getSize().y;
            int fontHeight = hoverText.getFont().getFontData()[0].getHeight() + 2;
            if (w > maxWidth) {
                int plus = (h * w) / maxWidth - h;
                if (plus % fontHeight != 0)
                    plus = (plus / fontHeight + 1) * fontHeight;
                h += plus;
                w = maxWidth;
            }
            shell.setSize(w, h);
            shell.layout();
        } else {
            // if there's just text, then it's a bibtex-entry
            setBibHover(information.trim());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#setSizeConstraints(int,
     *      int)
     */
    public void setSizeConstraints(int maxWidth, int maxHeight) {
        // this.maxHeight = maxHeight;
        this.maxWidth = maxWidth;
        /*
         * shell.setSize(maxWidth, maxHeight); shell.layout();
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#computeSizeHint()
     */
    public Point computeSizeHint() {
        if (entry != null) {
            return shell.getSize();
        }
        return shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        shell.setVisible(visible);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#setSize(int, int)
     */
    public void setSize(int width, int height) {
        shell.setSize(width, height);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#setLocation(org.eclipse.swt.graphics.Point)
     */
    public void setLocation(Point location) {
        shell.setLocation(location);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#dispose()
     */
    public void dispose() {
        shell.dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#addDisposeListener(org.eclipse.swt.events.DisposeListener)
     */
    public void addDisposeListener(DisposeListener listener) {
        shell.addDisposeListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#removeDisposeListener(org.eclipse.swt.events.DisposeListener)
     */
    public void removeDisposeListener(DisposeListener listener) {
        shell.removeDisposeListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#setForegroundColor(org.eclipse.swt.graphics.Color)
     */
    public void setForegroundColor(Color foreground) {
        hoverText.setForeground(foreground);
        shell.setForeground(foreground);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#setBackgroundColor(org.eclipse.swt.graphics.Color)
     */
    public void setBackgroundColor(Color background) {
        hoverText.setBackground(background);
        shell.setBackground(background);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#isFocusControl()
     */
    public boolean isFocusControl() {
        if (!hasImage)
            return true;
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#setFocus()
     */
    public void setFocus() {
        if (!hasImage) {
            shell.forceFocus();
            hoverText.setFocus();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#addFocusListener(org.eclipse.swt.events.FocusListener)
     */
    public void addFocusListener(FocusListener listener) {
        shell.addFocusListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControl#removeFocusListener(org.eclipse.swt.events.FocusListener)
     */
    public void removeFocusListener(FocusListener listener) {
        shell.removeFocusListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.IInformationControlExtension#hasContents()
     */
    public boolean hasContents() {
        if (entry == null)
            return false;
        return true;
    }

}