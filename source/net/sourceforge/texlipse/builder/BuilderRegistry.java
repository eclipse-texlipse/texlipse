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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.factory.BuilderDescription;
import net.sourceforge.texlipse.builder.factory.BuilderFactory;
import net.sourceforge.texlipse.builder.factory.BuilderXmlReader;
import net.sourceforge.texlipse.builder.factory.RunnerDescription;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;


/**
 * Holds a registry of available builders and runners.
 * Implemented using the Singleton pattern.
 * 
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 */
public class BuilderRegistry {

    // the singleton instance
    private static BuilderRegistry instance = new BuilderRegistry();

    // map with runner ids and runner descriptions
    private final Map<String, RunnerDescription> runners;

    // map with builder ids and builder descriptions
    private final Map<String, BuilderDescription> builders;

    // stream to write builder status messages to
    private MessageConsoleStream consoleStream;

    // the console for creating console streams
    private MessageConsole console;

    
    /**
     * Print a message to the console.
     */
    public static void printToConsole(String msg) {
        instance.getConsoleStream().println(msg);
    }

    /**
     * Clear the console window.
     */
    public static void clearConsole() {
        instance.consoleStream = null;
        TexlipsePlugin.getDefault().getWorkbench().getDisplay().syncExec(new Runnable() {
            public void run() {
                instance.getConsole().getDocument().set("");
            }});
    }
    
    /**
     * Return the console. Instantiate if necessary.
     * @return the output console
     */
    private MessageConsole getConsole() {
        if (console == null) {
            console = new MessageConsole("Texlipse", null);
            IConsoleManager mgr = ConsolePlugin.getDefault().getConsoleManager();
            mgr.addConsoles(new IConsole[] { console });
        }
        return console;
    }
    
    /**
     * Return the console output stream. Instantiate if necessary.
     * @return the output stream to console
     */
    private MessageConsoleStream getConsoleStream() {
        if (consoleStream == null) {
            consoleStream = getConsole().newMessageStream();
        }
        return consoleStream;
    }

    /**
     * Return the builder description for the old builder id.
     *
     * @param id legacy builder id
     * @return the builder description matching the legacy id,
     *  or <code>null</code> if not found.
     */
    private static BuilderDescription getBuilderByLegacyId(int id) {
        for (BuilderDescription bd : instance.builders.values()) {
            if (bd.getLegacyId() == id) {
                return bd;
            }
        }
        return null;
    }

    /**
     * Finds all builders that produce the given output format.
     *
     * @param format output format
     * @return builder descriptions
     */
    public static Collection<BuilderDescription> getAllBuilders(String format) {
        return instance.findBuilders(format);
    }

    /**
     * Returns all available runners.
     *
     * @return runner descriptions
     */
    public static Collection<RunnerDescription> getAllRunners() {
        return instance.runners.values();
    }

    /**
     * Returns the builder description for the unique builder id.
     *
     * @param builderId builder id
     * @return builder description, or <code>null</code> if no builder
     *  with this id exists
     */
    public static BuilderDescription getBuilderDescription(String builderId) {
        if (builderId != null) {
            return instance.builders.get(builderId);
        } else {
            return null;
        }
    }

    /**
     * Looks up the current builder id for given the legacy builder number, which may still
     * be written in the preferences.
     *
     * @param legacyId legacy builder number
     * @return the unique builder id, or <code>null</code> if no builder with this
     *  number was found
     */
    public static String getBuilderIdByLegacy(int legacyId) {
        BuilderDescription bd = getBuilderByLegacyId(legacyId);
        if (bd != null) {
            return bd.getId();
        }
        else {
            return null;
        }
    }

    /**
     * Instantiates and returns a new builder for the given id.
     *
     * @param builderId builder id
     * @return the new builder instance; returns <code>null</code> if no such
     *  id exists or the builder could not be instantiated
     */
    public static Builder getBuilder(String builderId) {
        try {
            return BuilderFactory.getInstance().getBuilderInstance(
                    getBuilderDescription(builderId));
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the runner description for the unique runner id.
     *
     * @param runnerId runner id
     * @return runner description, or <code>null</code> if no runner
     *  with this id exists
     */
    public static RunnerDescription getRunnerDescription(String runnerId) {
        return instance.runners.get(runnerId);
    }

    /**
     * Instantiates and returns a runner for the given id.
     *
     * @param runnerId runner id
     * @return runner instance; <code>null</code> if no such runner
     *  exists or it could not be instantiated
     */
    public static ProgramRunner getRunner(String runnerId) {
        try {
            return BuilderFactory.getInstance().getRunnerInstance(
                    getRunnerDescription(runnerId));
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a program runner for the given conversion.
     * <i>This will only return utility runners. Core runners should be identified
     * directly</i>.
     *
     * @param in input file format
     * @param out output file format
     * @return a program runner capable of converting from the given
     *         input format to the given output format; <code>null</code>
     *         if no appropriate runner was found in the registry
     */
    public static ProgramRunner getRunner(String in, String out) {
        String id = instance.findProgramRunner(in, out);
        try {
            if (id != null) {
                return BuilderFactory.getInstance().getRunnerInstance(
                        getRunnerDescription(id));
            }
            else {
                return null;
            }
        }
        catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Hidden constructor.
     * Creates the shared instances of the program runners and the builders.
     */
    protected BuilderRegistry() {
        final BuilderXmlReader xmlReader = new BuilderXmlReader();
        runners = xmlReader.getDefaultRunners();
        builders = xmlReader.getDefaultBuilders();
    }

    /**
     * Find all builders that produce the given output format.
     * 
     * @param format the output format
     * @return map with ids and labels for the builders
     */
    protected Collection<BuilderDescription> findBuilders(String format) {
        final List<BuilderDescription> list = new ArrayList<BuilderDescription>();

        for (BuilderDescription bd : builders.values()) {
            if (bd.getOutputFormat().equals(format)) {
                list.add(bd);
            }
        }

        return list;
    }
    
    /**
     * Get the named program runner.
     * 
     * @param in input format of the runner
     * @param out output format of the runner
     * @return the id of the program runner
     */
    protected String findProgramRunner(String in, String out) {

        for (RunnerDescription runner : instance.runners.values()) {
            if (!runner.isCoreRunner() && in.equals(runner.getInputFormat())
                    && (out == null || out.equals(runner.getOutputFormat()))) {
                return runner.getId();
            }
        }
        
        return null;
    }

}


