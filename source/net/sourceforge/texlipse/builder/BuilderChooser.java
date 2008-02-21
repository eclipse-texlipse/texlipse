/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;


/**
 * An UI component for choosing the builder.
 * Consists of two Combo components and a Label.
 * 
 * @author Kimmo Karlsson
 */
public class BuilderChooser {

    // container region
    private Group formatGroup;

    // list of output formats to choose from
    private Combo formatChooser;
    
    // list of possible sequences to choose from
    private Combo sequenceChooser;
    
    // mapping of output format indexes to possible sequences
    private String[][] mapping;
    
    // mapping of sequences to builder ids
    private HashMap<String, Integer> idMap;

    // list of (format-) selection listeners
    private ArrayList<SelectionListener> selectionListeners;

    /**
     * 
     */
    public BuilderChooser(Composite parent) {
        
        selectionListeners = new ArrayList<SelectionListener>();
        createMappings();
        
        formatGroup = new Group(parent, SWT.NONE);
        GridLayout gl = new GridLayout();
        gl.numColumns = 4;
        formatGroup.setLayout(gl);
        formatGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        // descriptive label
        Label label = new Label(formatGroup, SWT.LEFT);
        label.setText(TexlipsePlugin.getResourceString("propertiesOutputFormatLabel"));
        label.setLayoutData(new GridData());
        
        // create output format chooser
        formatChooser = new Combo(formatGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        formatChooser.setLayoutData(new GridData());
        formatChooser.setToolTipText(TexlipsePlugin.getResourceString("propertiesOutputFormatTooltip"));
        formatChooser.setItems(new String[] { TexlipseProperties.OUTPUT_FORMAT_DVI, TexlipseProperties.OUTPUT_FORMAT_PS, TexlipseProperties.OUTPUT_FORMAT_PDF });
        formatChooser.select(0);
        formatChooser.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                int index = formatChooser.getSelectionIndex();
                if (index >= 0) {
                    sequenceChooser.setItems(mapping[index]);
                    sequenceChooser.select(0);
                    fireSelectionEvent(event);
                }
            }
        });
        
        // descriptive label
        Label slabel = new Label(formatGroup, SWT.LEFT);
        slabel.setText(TexlipsePlugin.getResourceString("propertiesOutputSequenceLabel"));
        slabel.setLayoutData(new GridData());
        
        // create sequence chooser
        sequenceChooser = new Combo(formatGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        sequenceChooser.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sequenceChooser.setToolTipText(TexlipsePlugin.getResourceString("propertiesOutputSequenceTooltip"));
        sequenceChooser.setItems(mapping[0]);
        sequenceChooser.select(0);
        sequenceChooser.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                int index = sequenceChooser.getSelectionIndex();
                if (index >= 0) {
                    fireSelectionEvent(event);
                }
            }
        });
		
		final Link prefLink = new Link(formatGroup, SWT.NONE);
		prefLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		prefLink.setText(TexlipsePlugin.getResourceString("propertiesOutputLinkToPrefs"));
		prefLink.addSelectionListener(new SelectionAdapter() {
            @Override
			public void widgetSelected(SelectionEvent e) {
				PreferencesUtil.createPreferenceDialogOn(formatGroup.getShell(), "TexlipseBuilderPreferencePage", new String[] {
						"TexlipseBuilderPreferencePage",
						"net.sourceforge.texlipse.properties.BuilderEnvPreferencePage"
				}, null).open();
			}
		});
    }
    
    /**
     * Creates the Sequence to Id mappings
     */
    private void createMappings() {
        idMap = new HashMap<String, Integer>();
        
        Builder[] dvis = BuilderRegistry.getAll(TexlipseProperties.OUTPUT_FORMAT_DVI);
        Builder[] pses = BuilderRegistry.getAll(TexlipseProperties.OUTPUT_FORMAT_PS);
        Builder[] pdfs = BuilderRegistry.getAll(TexlipseProperties.OUTPUT_FORMAT_PDF);
        
        mapping = new String[3][];
        mapping[0] = new String[dvis.length];
        for (int i = 0; i < dvis.length; i++) {
            mapping[0][i] = dvis[i].getSequence();
            idMap.put(mapping[0][i], new Integer(dvis[i].getId()));
        }
        mapping[1] = new String[pses.length];
        for (int i = 0; i < pses.length; i++) {
            mapping[1][i] = pses[i].getSequence();
            idMap.put(mapping[1][i], new Integer(pses[i].getId()));
        }
        mapping[2] = new String[pdfs.length];
        for (int i = 0; i < pdfs.length; i++) {
            mapping[2][i] = pdfs[i].getSequence();
            idMap.put(mapping[2][i], new Integer(pdfs[i].getId()));
        }
    }

    /**
     * Forward the event forward.
     * @param event event
     */
    private void fireSelectionEvent(SelectionEvent event) {
        for (int i = 0; i < selectionListeners.size(); i++) {
            SelectionListener lis = (SelectionListener) selectionListeners.get(i);
            lis.widgetSelected(event);
        }
    }

    /**
     * Add a selection listener to this component.
     */
    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }
    
    /**
     * @return the output format that is currently selected
     */
    public String getSelectedFormat() {
        int index = formatChooser.getSelectionIndex();
        if (index < 0) {
            return null;
        }
        return formatChooser.getItem(index);
    }

    /**
     * @return the id of the currently selected builder
     */
    public int getSelectedBuilder() {
        
        int index = sequenceChooser.getSelectionIndex();
        if (index >= 0) {
            
            String seq = sequenceChooser.getItem(index);
            Integer num = (Integer) idMap.get(seq);
            
            index = -1;
            if (num != null) {
                index = num.intValue();
            }
        }
        
        return index;
    }

    /**
     * Selects the correct values from combo components.
     * @param num the builder id to select
     */
    public void setSelectedBuilder(int num) {
        
        Builder b = BuilderRegistry.get(num);
        if (b != null) {
            
            String sequence = b.getSequence();
            int index = formatChooser.indexOf(b.getOutputFormat());
            if (index < 0) {
                index = 0;
            }
            
            formatChooser.select(index);
            sequenceChooser.setItems(mapping[index]);
            index = sequenceChooser.indexOf(sequence);
            if (index < 0) {
                index = 0;
            }
            
            sequenceChooser.select(index);
        }
    }
	
	/**
	 * Access to the SWT control to set layout data, disable group etc.
	 */
	public Composite getControl() {
		return formatGroup;
	}
	
}
