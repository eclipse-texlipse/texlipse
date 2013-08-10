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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.factory.BuilderDescription;
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
    
    // mapping of output formats to possible sequences
    private Map<String, BuilderDescription[]> mappings;

    // mapping for box indexes to output formats
    private String[] mappingIdx;

    // mapping for box indexes to builders
    private BuilderDescription[] currentMap;

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
                    setCurrentMap(index);
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
        setCurrentMap(0);
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
        mappings = new HashMap<String, BuilderDescription[]>();
        mappingIdx = new String[] { TexlipseProperties.OUTPUT_FORMAT_DVI,
                TexlipseProperties.OUTPUT_FORMAT_PS,
                TexlipseProperties.OUTPUT_FORMAT_PDF };
        for (String m : mappingIdx) {
            BuilderDescription[] builders = BuilderRegistry.getAllBuilders(m).toArray(
                    new BuilderDescription[0]);
            Arrays.sort(builders, new Comparator<BuilderDescription>() {
                public int compare(BuilderDescription o1, BuilderDescription o2) {
                    return o1.getLabel().compareTo(o2.getLabel());
                }
            });
            mappings.put(m, builders);
        }
    }

    private void setCurrentMap(int mapIdx) {
        if (mapIdx > 0 && mapIdx < mappingIdx.length) {
            currentMap = mappings.get(mappingIdx[mapIdx]);
        }
        else {
            currentMap = mappings.get(mappingIdx[0]);
        }
        formatChooser.select(mapIdx);
        sequenceChooser.removeAll();
        for (BuilderDescription bd : currentMap) {
            sequenceChooser.add(bd.getLabel());
        }
    }

    private int getMapIndex(String outputFormat) {
        boolean found = false;
        int i = 0;
        while (!found && i < mappingIdx.length) {
            found = mappingIdx[i].equals(outputFormat);
            i++;
        }
        if (found) {
            return i - 1;
        } else {
            return 0;
        }
    }

    private int getBuilderIndex(String builderId) {
        boolean found = false;
        int i = 0;
        while (!found && i < currentMap.length) {
            found = currentMap[i].getId().equals(builderId);
            i++;
        }
        if (found) {
            return i - 1;
        } else {
            return 0;
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
    public String getSelectedBuilder() {

        int index = sequenceChooser.getSelectionIndex();
        if (currentMap != null && index >= 0
                && index < currentMap.length) {
            return currentMap[index].getId();
        }
        else {
            return null;
        }
    }

    /**
     * Selects the correct values from combo components.
     *
     * @param builderId the builder id to select
     */
    public void setSelectedBuilder(String builderId) {

        BuilderDescription b = BuilderRegistry.getBuilderDescription(builderId);
        if (b != null) {
            setCurrentMap(getMapIndex(b.getOutputFormat()));
            sequenceChooser.select(getBuilderIndex(builderId));
        }
    }
	
	/**
	 * Access to the SWT control to set layout data, disable group etc.
	 */
	public Composite getControl() {
		return formatGroup;
	}
	
}
