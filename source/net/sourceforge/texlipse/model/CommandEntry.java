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
 * A class for containing LaTeX commands.
 * 
 * @author Oskar Ojala
 */
public final class CommandEntry extends AbstractEntry {

    /**
     * A descriptive text of the command
     */
    public String info;
    /**
     * The number of arguments the command has
     */
    public int arguments;
    
    /**
     * Constructs a new entry with the given key (command name without the
     * leading slash).
     * 
     * @param key Command name without the leading slash
     */
    public CommandEntry(String key) {
        this.key = key;
    }

    /**
     * Constructs a new entry with the given key (command name without the
     * leading slash) and a descriptive text telling something about the
     * command.
     * 
     * @param key Command name without the leading slash
     * @param info A descriptive text of the command
     */
    public CommandEntry(String key, String info, int args) {
        this.key = key;
        this.info = info;
        this.arguments = args;
    }
}
