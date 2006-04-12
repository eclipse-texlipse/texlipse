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

    // B-----borisvl
    
    public ReferenceEntry getBib(String name) {
        ReferenceEntry[] bibEntries = bibContainer.getSortedReferences();
        int nr = getEntry(name, bibEntries);
        if (nr != -1) return bibEntries[nr];
        else return null;
    }

    /**
     * Returns the ReferenceEntry of the label with the key ref. This 
     * function uses a binary search.
     * @param ref
     * @return The adequate entry or null if no entry was found
     */
    public ReferenceEntry getLabel(String ref) {
        ReferenceEntry[] labels = labelContainer.getSortedReferences();
        int nr = getEntry(ref, labels);
        if (nr != -1) return labels[nr];
        else return null;
    }

    /**
     * Returns the CommandEntry of the command with the key name. This
     * function uses a binary search
     * @param name
     * @return The adequate entry or null if no entry was found
     */
    public TexCommandEntry getEntry(String name) {
        TexCommandEntry[] commands = commandContainer.getSortedCommands(TexCommandEntry.MATH_CONTEXT);
        int nr = getEntry(name, commands);
        if (nr != -1) return commands[nr];
        // If no math command look at the normal commands
        commands = commandContainer.getSortedCommands(TexCommandEntry.NORMAL_CONTEXT);
        nr = getEntry(name, commands);
        if (nr != -1) return commands[nr];
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
    public ReferenceEntry[] getCompletionsRef(String start) {
        ReferenceEntry[] labels = labelContainer.getSortedReferences();

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
        int[] bounds;
        // if (lastBib.length() > 0 && start.startsWith(lastBib))
        // bounds = getCompletionsBin(start, bibEntries, lastBibBounds);
        // else
        // bounds = getCompletionsBin(start, bibEntries);

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
    public TexCommandEntry[] getCompletionsCom(String start, int context) {
        TexCommandEntry[] commands = commandContainer.getSortedCommands(context);
        if (commands == null)
            return null;
        if (start.equals(""))
            return commands;

        int[] bounds = getCompletionsBin(start, commands);
        if (bounds[1] == -1)
            return null;
        TexCommandEntry[] compls = new TexCommandEntry[bounds[1] - bounds[0]];

        System.arraycopy(commands, bounds[0], compls, 0, bounds[1] - bounds[0]);
        return compls;
    }
}
