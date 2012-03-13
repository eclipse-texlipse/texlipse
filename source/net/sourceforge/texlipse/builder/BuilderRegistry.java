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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

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
    
    // registry of outputformat -> builder mappings
    private Builder[] builderList;
    
    // array of program runners
    private ProgramRunner[] runnerList;

    private final List<ProgramRunner> runners;
    private final List<BuilderDescriptor> builders;

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
     * Get the builder for the given format.
     * @param clazz the class of the builder
     * @param format the output format
     * 
     * @return the builder or null if there is no such builder configured
     */
    public static Builder get(Class<? extends Builder> clazz, String format) {
        return instance.getBuilder(clazz, format);
    }
    
    /**
     * Find all builders that produce the given output format.
     * 
     * @param format output format
     * @return references to the builder instances
     */
    public static Builder[] getAll(String format) {
        if (instance.builderList == null) {
            instance.initBuilders();
        }
        return instance.getAllBuilders(format);
    }

    /**
     * Get the ith builder
     * @param i id number of the builder
     * @return the builder instance, or null if index out of bounds
     */
    public static Builder get(int i) {
        if (instance.builderList == null) {
            instance.initBuilders();
        }
        if (i >= 0 && i < instance.builderList.length) {
            return instance.builderList[i];
        }
        return null;
    }
    
    /**
     * Get the ith program runner.
     * 
     * @param i the index of a runner
     * @return a program runner
     */
    public static ProgramRunner getRunner(int i) {
        return instance.getProgramRunner(i);
    }

    /**
     * Get a program runner for the given conversion.
     * 
     * @param in input file format
     * @param out output file format
     * @param alternative if there are more than one runner, take the <code>alernative</code>'th
     *         runner
     * @return a program runner capable of converting from the given
     *         input format to the given output format
     */
    public static ProgramRunner getRunner(String in, String out, int alternative) {
        return instance.getProgramRunner(in, out, alternative);
    }
    
    /**
     * Hidden constructor.
     * Creates the shared instances of the program runners and the builders.
     * 
     * We could use lazy instantiation, but the ProgramRunners don't allocate
     * much memory at construction time and their construction requires almost
     * no processing, so it is easier to allocate them all at once.
     * 
     * Also: - we need all the runners when constructing builders
     *       - we need the number of runners when initializing preferences
     */
    protected BuilderRegistry() {
        runnerList = new ProgramRunner[] {
                new LatexRunner(),
                new PslatexRunner(),
                new PdflatexRunner(),
                new XelatexRunner(),
                new LualatexRunner(),
                new BibtexRunner(),
                new BiberRunner(),
                new MakeindexRunner(),
                new DvipsRunner(),
                new DvipdfRunner(),
                new Ps2pdfRunner(),
                new MakeindexNomenclRunner(),
                new KpsewhichRunner()
        };
        runners = new ArrayList<ProgramRunner>();
        runners.add(new LatexRunner());
        runners.add(new PslatexRunner());
        runners.add(new PdflatexRunner());
        runners.add(new XelatexRunner());
        runners.add(new LualatexRunner());
        runners.add(new BibtexRunner());
        runners.add(new BiberRunner());
        runners.add(new MakeindexRunner());
        runners.add(new DvipsRunner());
        runners.add(new DvipdfRunner());
        runners.add(new Ps2pdfRunner());
        runners.add(new MakeindexNomenclRunner());
        runners.add(new KpsewhichRunner());
        builders = new ArrayList<BuilderDescriptor>();
        try {
            builders.add(new BuilderDescriptor(TexBuilder.class, LatexRunner.class));
            builders.add(new BuilderDescriptor(TexBuilder.class, PslatexRunner.class));
            builders.add(new BuilderDescriptor(TexBuilder.class, PdflatexRunner.class));

            builders.add(new BuilderDescriptor(DviBuilder.class, DvipsRunner.class));
            builders.add(new BuilderDescriptor(DviBuilder.class, DvipdfRunner.class));

            builders.add(new BuilderDescriptor(PsBuilder.class, TexBuilder.class, null));
            builders.add(new BuilderDescriptor(PsBuilder.class, DviBuilder.class, null));

            builders.add(new BuilderDescriptor(TexBuilder.class, XelatexRunner.class));
            builders.add(new BuilderDescriptor(TexBuilder.class, LualatexRunner.class));
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Initializes builders. This cannot be done in constructor,
     * because builders need to use BuilderRegistry to resolve runners.
     */
    protected void initBuilders() {
        builderList = new Builder[14];

        builderList[0] = new TexBuilder(0, TexlipseProperties.OUTPUT_FORMAT_DVI, 0);
        builderList[1] = new TexBuilder(1, TexlipseProperties.OUTPUT_FORMAT_PS, 0);
        builderList[2] = new TexBuilder(2, TexlipseProperties.OUTPUT_FORMAT_PDF, 0);

        builderList[3] = new DviBuilder(3, TexlipseProperties.OUTPUT_FORMAT_PS);
        builderList[4] = new DviBuilder(4, TexlipseProperties.OUTPUT_FORMAT_PDF);

        builderList[5] = new PsBuilder(5, TexBuilder.class);
        builderList[6] = new PsBuilder(6, DviBuilder.class);
        builderList[7] = new TexBuilder(7, TexlipseProperties.OUTPUT_FORMAT_PDF, 1);
        builderList[8] = new TexBuilder(8, TexlipseProperties.OUTPUT_FORMAT_PDF, 2);

        builderList[9] = new TexCycleBuilder(9, TexlipseProperties.OUTPUT_FORMAT_DVI, 0);
        builderList[10] = new TexCycleBuilder(10, TexlipseProperties.OUTPUT_FORMAT_PS, 0);
        builderList[11] = new TexCycleBuilder(11, TexlipseProperties.OUTPUT_FORMAT_PDF, 0);
        builderList[12] = new TexCycleBuilder(12, TexlipseProperties.OUTPUT_FORMAT_PDF, 1);
        builderList[13] = new TexCycleBuilder(13, TexlipseProperties.OUTPUT_FORMAT_PDF, 2);
    }
    
    /**
     * Find a builder from the registry for the given output format.
     *  
     * @param builderClass the implementing class of the builder
     * @param outputFormat the output format of the builder
     * @return the builder instance, or null if none found
     */
    protected Builder getBuilder(Class<? extends Builder> builderClass, String outputFormat) {

        if (outputFormat == null) {
            return null;
        }

        Builder builder = null;
        for (int i = 0; i < builderList.length; i++) {
            if (builderList[i] != null
                    && builderList[i].getOutputFormat().equals(outputFormat)
                    && (builderClass == null || builderList[i].getClass().equals(builderClass))) {
                builder = builderList[i];
            }
        }
        
        if (builder == null) {
            return null;
        }
        
        return builder;
    }

    /**
     * Find all builders that produce the given output format.
     * 
     * @param format the output format
     * @return references to the builders
     */
    protected Builder[] getAllBuilders(String format) {

        List<Builder> list = new ArrayList<Builder>();

        for (int i = 0; i < builderList.length; i++) {
            if (builderList[i] != null
                    && builderList[i].getOutputFormat().equals(format)) {
                list.add(builderList[i]);
            }
        }

        return list.toArray(new Builder[0]);
    }
    
    /**
     * Get the ith program runner.
     * 
     * @param i the index number of the runner
     * @return the program runner
     */
    protected ProgramRunner getProgramRunner(int i) {
        if (i >= 0 && i < runnerList.length) {
            return runnerList[i];
        }
        return null;
    }

    /**
     * Get the named program runner.
     * 
     * @param in input format of the runner
     * @param out output format of the runner
     * @return the program runner
     */
    protected ProgramRunner getProgramRunner(String in, String out, int alternative) {
        
        int size = getNumberOfRunners();
        for (int i = 0; i < size; i++) {
            ProgramRunner r = getProgramRunner(i);
            if (in.equals(r.getInputFormat()) && 
                    (out == null || r.getOutputFormat().equals(out))) {
                if (alternative == 0) return r;
                alternative--;
            }
        }
        
        return null;
    }
    
    /**
     * Returns the number of implemented runners.
     * @return the number of implemented runners
     */
    public static int getNumberOfRunners() {
        return instance.runnerList.length;
    }
}


