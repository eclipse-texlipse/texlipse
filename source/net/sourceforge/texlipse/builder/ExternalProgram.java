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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Properties;

import net.sourceforge.texlipse.PathUtils;
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;


/**
 * Helper methods to run an external program.
 * 
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 */
public class ExternalProgram {
    
    // the command to run
    private String[] command;

    // the directory to the command in
    private File dir;

    // the process that executes the command
    private Process process;

    // output messages to this console
    private String consoleOutput;
    
    /**
     * Creates a new command runner.
     */
    public ExternalProgram() {
        this.command = null;
        this.dir = null;
        this.process = null;
        this.consoleOutput = null;
    }

    /**
     * Resets the command runner.
     * 
     * @param command command to run
     * @param dir directory to run the command in
     */
    public void setup(String[] command, File dir, String console) {
        this.command = command;
        this.dir = dir;
        this.process = null;
        this.consoleOutput = console;
    }

    /**
     * Force termination of the running process.
     */
    public void stop() {
        if (process != null) {
            process.destroy();
            // can't null the process here, because run()-method of this class is still executing
            //process = null;
        }
    }
    
    /**
     * Reads the contents of a stream.
     * 
     * @param is the stream
     * @return the contents of the stream as a String
     */
    protected String readOutput(InputStream is) {
        
        StringWriter store = new StringWriter();
        try {
            
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;

            while ((line = br.readLine()) != null) {
                store.write(line + '\n');
                if (consoleOutput != null) {
                    BuilderRegistry.printToConsole(consoleOutput + "> " + line);
                }
            }
            
        } catch (IOException e) {
        }
        
        store.flush();
        return store.getBuffer().toString();
    }

    /**
     * Runs the external program as a process and waits 
     * for the process to finish execution.
     * 
     * @param queryMessage text which will trigger the query dialog
     * @return the text produced to standard output by the process
     * @throws Exception
     */
    public String run(String[] queryMessage) throws Exception {
        return run(true, queryMessage);
    }

    /**
     * Runs the external program as a process and waits 
     * for the process to finish execution.
     * 
     * @return the text produced to standard output by the process
     * @throws Exception
     */
    public String run() throws Exception {
        return run(true, null);
    }
    
    /**
     * Runs the external program as a process and waits 
     * for the process to finish execution.
     * 
     * @param wait if true, this method will block until
     *             the process has finished execution
     * @return the text produced to standard output by the process
     * @throws IOException 
     */
    protected String run(boolean wait, String[] queryMessage) throws IOException {
        
        String output = null;
        String errorOutput = null;
        if ((command != null) && (dir != null)) {
            
        	StringBuffer commandSB = new StringBuffer();
        	for (int i = 0; i < command.length; i++) {
        		commandSB.append(command[i]);
        		commandSB.append(" ");
        	}
        	
            BuilderRegistry.printToConsole("running: " + commandSB.toString());
            Runtime rt = Runtime.getRuntime();
            
            // Add builder program path to environmet variables.
            // This is needed at least on Mac OS X, where Eclipse overwrites
            // the "path" environment variable, and xelatex needs its directory in the path.
            Properties envProp = PathUtils.getEnv();
            int index = command[0].lastIndexOf(File.separatorChar);
            if (index > 0) {
	            String commandPath = command[0].substring(0, index);
	            String key = PathUtils.findPathKey(envProp);
	            envProp.setProperty(key, envProp.getProperty(key) + File.pathSeparatorChar + commandPath);
            }
            
            String[] env = PathUtils.mergeEnvFromPrefs(envProp, TexlipseProperties.BUILD_ENV_SETTINGS);
            process = rt.exec(command, env, dir);
            
        } else {
            throw new IllegalStateException();
        }

        final StringBuffer thErrorOutput = new StringBuffer();
        final StringBuffer thOutput = new StringBuffer();
        
        // scan the standard output stream
        final OutputScanner scanner = new OutputScanner(process.getInputStream(), 
                process.getOutputStream(), queryMessage, consoleOutput);
        
        // scan also the standard error stream
        final OutputScanner errorScanner = new OutputScanner(process.getErrorStream(), 
                process.getOutputStream(), queryMessage, consoleOutput);
        
        final Thread errorThread = new Thread() {
            public void run() {
                if (errorScanner.scanOutput()) {
                    thErrorOutput.append(errorScanner.getText());
                }
            };
        };
        final Thread outputThread = new Thread() {
            public void run() {
                if (scanner.scanOutput()) {
                    thOutput.append(scanner.getText());
                } else {
                    // Abort by user: Abort build, clear all output
                    process.destroy();
                    try {
                        errorThread.join();
                    } catch (InterruptedException e) {
                        // Should not happen
                        TexlipsePlugin.log("Output scanner interrupted", e);
                    }
                    thOutput.setLength(0);
                    thErrorOutput.setLength(0);
                }
            };
        };

        outputThread.start();
        errorThread.start();
        try {
            // Wait until stream read has finished
            errorThread.join();
            outputThread.join();
        } catch (InterruptedException e) {
            TexlipsePlugin.log("Output scanner interrupted", e);
            // Should not happen
        }

        output = thOutput.toString();
        errorOutput = thErrorOutput.toString();
        
        
        if (wait) {
            // the process status code is not useful here
            //int code = 
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                //Should not happen
                TexlipsePlugin.log("Process interrupted", e);                
            }
        }
        
        process = null;
        
        // combine the error output with normal output
        // to collect information from for example makeindex
        if (errorOutput.length() > 0) {
        	output += "\n" + errorOutput;
        }
        return output;
    }
}
