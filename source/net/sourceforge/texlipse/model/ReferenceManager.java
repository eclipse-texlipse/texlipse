/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.model;


/**
 * Manages the references (BibTeX and \label) and provides an interface
 * for searching them efficiently by partial matches.
 * 
 * @author Oskar Ojala
 */
public class ReferenceManager extends PartialRetriever {

    private ReferenceContainer bibContainer;
    private ReferenceContainer labelContainer;
    private TexCommandContainer commandContainer;
    
    private String lastBib = "";
    private int[] lastBibBounds;
    private String lastLab = "";
    private int[] lastLabBounds;
    
    /**
     * Creates new ReferenceManager that uses the given BibTeX,
     * label and command-containers for searching.
     * 
     * @param bibRc BibTeX reference container
     * @param labRc Label container
     * @param commands Command container
     */
    public ReferenceManager(ReferenceContainer bibRc, ReferenceContainer labRc, TexCommandContainer commands) {
        this.bibContainer = bibRc;
        this.labelContainer = labRc;
        this.commandContainer = commands;
    }

    /**
     * Gets the completions for \ref (ie. the corresponding labels) that
     * start with the given string.
     * 
     * @param start The string with which the completions should start
     * @return An array of completions or null if there were no completions
     */
    public ReferenceEntry[] getCompletionsRef(String start) {
        ReferenceEntry[] labels = labelContainer.getSortedReferences();
        
        if (labels == null)
            return null;
        if (start.equals(""))
            return labels;

        // don't refetch the proposal list in partial fill;
        // use the existing proposal list and make it smaller
        int [] bounds;
//        if (lastLab.length() > 0 && start.startsWith(lastLab))
//            bounds = getCompletionsBin(start, labels, lastLabBounds);
//        else
//            bounds = getCompletionsBin(start, labels);

        bounds = getCompletionsBin(start, labels);
        
        if (bounds[0] == -1) {
            lastLab = "";
            return null;
        } else {
            lastLab = start;
            lastLabBounds = bounds;
        }
        ReferenceEntry[] compls = new ReferenceEntry[bounds[1] - bounds[0]];
        System.arraycopy(labels, bounds[0], compls, 0, bounds[1] - bounds[0]);
        return compls;
    }

    /**
     * Gets the completions for \cite (ie. the corresponding BibTeX entries)
     * that start with the given string.
     * 
     * @param start The string with which the completions should start
     * @return An array of completions or null if there were no completions
     */    
    public ReferenceEntry[] getCompletionsBib(String start) {
        ReferenceEntry[] bibEntries = bibContainer.getSortedReferences();
        
        if (bibEntries == null)
            return null;
        if (start.equals(""))
            return bibEntries;

        // don't refetch the proposal list in partial fill;
        // use the existing proposal list and make it smaller
        int [] bounds;
//        if (lastBib.length() > 0 && start.startsWith(lastBib))
//            bounds = getCompletionsBin(start, bibEntries, lastBibBounds);
//        else
//            bounds = getCompletionsBin(start, bibEntries);

        // ...either solve problems with bounds or remove them...
        bounds = getCompletionsBin(start, bibEntries);
        
        if (bounds[0] == -1) {
            lastBib = "";
            return null;
        } else {
            lastBib = start;
            lastBibBounds = bounds;
        }
        ReferenceEntry[] compls = new ReferenceEntry[bounds[1] - bounds[0]];
        System.arraycopy(bibEntries, bounds[0], compls, 0, bounds[1] - bounds[0]);
        return compls;
    }
    
    /**
     * Returns command completions.
     * 
     * @param start The string with which the completions should start
     * @return An array of completions or null if there were no completions
     */
    public CommandEntry[] getCompletionsCom(String start) {
        CommandEntry[] commands = commandContainer.getSortedCommands();
        if (commands == null)
            return null;
        if (start.equals(""))
            return commands;
        
        int[] bounds = this.getCompletionsBin(start, commands);
        if (bounds[0] == -1)
            return null;
        
        CommandEntry[] compls = new CommandEntry[bounds[1] - bounds[0]];
        System.arraycopy(commands, bounds[0], compls, 0, bounds[1] - bounds[0]);
        return compls;
    }
}
