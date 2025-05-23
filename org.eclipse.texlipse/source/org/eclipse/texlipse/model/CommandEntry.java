/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.model;

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
