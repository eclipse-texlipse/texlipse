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

import java.util.List;

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
     * A simple getter for the Bibcontainer
     * 
     * @return the bibContainer
     */
    public ReferenceContainer getBibContainer() {
    	return this.bibContainer;
    }
    
    // B-----borisvl
    
    public ReferenceEntry getBib(String name) {
        List<ReferenceEntry> bibEntries = bibContainer.getSortedReferences();
        int nr = getEntry(name.toLowerCase(), bibEntries, true);
        if (nr != -1) return bibEntries.get(nr);
        else return null;
    }

    /**
     * Returns the ReferenceEntry of the label with the key ref. This 
     * function uses a binary search.
     * @param ref
     * @return The adequate entry or null if no entry was found
     */
    public ReferenceEntry getLabel(String ref) {
        List<ReferenceEntry> labels = labelContainer.getSortedReferences();
        int nr = getEntry(ref, labels, true);
        if (nr != -1) return labels.get(nr);
        else return null;
    }

    /**
     * Returns the CommandEntry of the command with the key name. This
     * function uses a binary search
     * @param name
     * @return The adequate entry or null if no entry was found
     */
    public TexCommandEntry getEntry(String name) {
        List<TexCommandEntry> commands = commandContainer.getSortedCommands(TexCommandEntry.MATH_CONTEXT);
        int nr = getEntry(name, commands, false);
        if (nr != -1) return commands.get(nr);
        // If no math command look at the normal commands
        commands = commandContainer.getSortedCommands(TexCommandEntry.NORMAL_CONTEXT);
        nr = getEntry(name, commands, false);
        if (nr != -1) return commands.get(nr);
        return null;
    }

    // E-----borisvl
    
    /**
     * Gets the completions for \ref (ie. the corresponding labels) that
     * start with the given string.
     * 
     * @param start The string with which the completions should start
     * @return An array of completions or null if there were no completions
     */
    public List<ReferenceEntry> getCompletionsRef(String start) {
        List<ReferenceEntry> labels = labelContainer.getSortedReferences();

        if (labels == null)
            return null;
        if (start.equals(""))
            return labels;

        // don't refetch the proposal list in partial fill;
        // use the existing proposal list and make it smaller
        int[] bounds;
        // if (lastLab.length() > 0 && start.startsWith(lastLab))
        // bounds = getCompletionsBin(start, labels, lastLabBounds);
        // else
        // bounds = getCompletionsBin(start, labels);

        bounds = getCompletionsBin(start, labels, true);

        if (bounds[0] == -1) return null;
        return labels.subList(bounds[0], bounds[1]);
    }

    /**
     * Gets the completions for \cite (ie. the corresponding BibTeX entries)
     * that start with the given string.
     * 
     * @param start The string with which the completions should start
     * @return An array of completions or null if there were no completions
     */
    public List<ReferenceEntry> getCompletionsBib(String start) {
        List<ReferenceEntry> bibEntries = bibContainer.getSortedReferences();

        if (bibEntries == null)
            return null;
        if (start.equals(""))
            return bibEntries;

        // don't refetch the proposal list in partial fill;
        // use the existing proposal list and make it smaller
        int[] bounds;
        // if (lastBib.length() > 0 && start.startsWith(lastBib))
        // bounds = getCompletionsBin(start, bibEntries, lastBibBounds);
        // else
        // bounds = getCompletionsBin(start, bibEntries);

        // ...either solve problems with bounds or remove them...
        bounds = getCompletionsBin(start, bibEntries, true);

        if (bounds[0] == -1) return null;
        return bibEntries.subList(bounds[0], bounds[1]);
    }

    /**
     * Returns command completions.
     * 
     * @param start The string with which the completions should start
     * @return An array of completions or null if there were no completions
     */
    public List<TexCommandEntry> getCompletionsCom(String start, int context) {
        List<TexCommandEntry> commands = commandContainer.getSortedCommands(context);
        if (commands == null)
            return null;
        if (start.equals(""))
            return commands;

        int[] bounds = getCompletionsBin(start, commands, false);
        if (bounds[1] == -1)
            return null;
        
        return commands.subList(bounds[0], bounds[1]);
    }
}
