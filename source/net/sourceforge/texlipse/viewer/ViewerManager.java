/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.viewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.texlipse.PathUtils;
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.BuilderRegistry;
import net.sourceforge.texlipse.builder.TexlipseBuilder;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.viewer.util.FileLocationListener;
import net.sourceforge.texlipse.viewer.util.FileLocationServer;
import net.sourceforge.texlipse.viewer.util.ViewerErrorScanner;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;



/**
 * A helper class for opening source files.
 * Defined separately, so that it could be used inside a static method.
 * 
 * @author Kimmo Karlsson
 */
class FileLocationOpener implements FileLocationListener {
    private IProject project;
    public FileLocationOpener(IProject p) {
        project = p;
    }
    public void showLineOfFile(String file, int lineNumber) {
        ViewerOutputScanner.openInEditor(project, file, lineNumber);
    }
}

/**
 * Previewer helper class. Includes methods for launching the previewer.
 * There's no need to create instances of this class.
 * 
 * @author Kimmo Karlsson
 */
public class ViewerManager {

    // attribute for session properties to hold the viewer process object
    private static final String SESSION_ATTRIBUTE_VIEWER = "active.viewer";
    
    // the file name variable in the configurations
    public static final String FILENAME_PATTERN = "%file";

    
    /**
     * Run the viewer configured in the preferences.
     * @throws CoreException if launching the viewer fails
     */
    public static void preview() throws CoreException {
        preview(new ViewerAttributeRegistry(), null);
    }
    
    /**
     * Run the viewer configured in the given viewer attributes.
     * First check if there is a viewer already running,
     * and if there is, return that process.
     * 
     * @param reg the viewer attributes
     * @param addEnv additional environment variables, or null
     * @return the viewer process
     * @throws CoreException if launching the viewer fails
     */
    public static Process preview(ViewerAttributeRegistry reg, Map addEnv) throws CoreException {

        IProject project = TexlipsePlugin.getCurrentProject();
        if (project == null) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("viewerNoCurrentProject"));
            return null;
        }
        
        // check if viewer already running
        Object o = TexlipseProperties.getSessionProperty(project, SESSION_ATTRIBUTE_VIEWER);
        if (o != null) {
            
            if (o instanceof Process) {
                Process p = (Process) o;
                
                int code = -1;
                try {
                    code = p.exitValue();
                } catch (IllegalThreadStateException e) {
                }
                
                if (code == -1) {
                    return p;
                }
            }
            
            TexlipseProperties.setSessionProperty(project, SESSION_ATTRIBUTE_VIEWER, null);
        }
        
        Process process = checkViewer(project, reg, addEnv);
        TexlipseProperties.setSessionProperty(project, SESSION_ATTRIBUTE_VIEWER, process);
        return process;
    }
    
    /**
     * Run the viewer configured in the given viewer attributes.
     * Paths are resolved so that the viewer program is run in source directory.
     * The viewer program is given a relative pathname and filename as a command line
     * argument. 
     * 
     * @param project the current project
     * @param reg the viewer attributes
     * @param addEnv additional environment variables, or null
     * @return the viewer process
     * @throws CoreException if launching the viewer fails
     */
    private static Process checkViewer(IProject project, ViewerAttributeRegistry reg, Map addEnv) throws CoreException {

        String outFileName = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUTFILE_PROPERTY);
        if (outFileName == null || outFileName.length() == 0) {
            throw new CoreException(TexlipsePlugin.stat("Empty output file name."));
        }
        
        // rebuild, if needed
        if (TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.BUILD_BEFORE_VIEW)) {

            if (TexlipseBuilder.needsRebuild()) {
                try {
                    project.build(TexlipseBuilder.FULL_BUILD, new NullProgressMonitor());
                } catch (CoreException e) {
                    return null;
                }
            }
        }
        
        // find out the directory where the file should be
        IContainer outputDir = null;
        if (reg.getFormat().equals(TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT))) {
            outputDir = TexlipseProperties.getProjectOutputDir(project);
        } else {
            String base = outFileName.substring(0, outFileName.lastIndexOf('.') + 1);
            outFileName = base + reg.getFormat();
            outputDir = TexlipseProperties.getProjectTempDir(project);
        }
        if (outputDir == null) {
            outputDir = project;
        }
        
        // check if file exists
        IResource outputRes = outputDir.findMember(outFileName);
        if (outputRes == null || !outputRes.exists()) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("viewerNothingWithExtension").replaceAll("%s", reg.getFormat()));
            return null;
        }

        // resolve the directory to run the viewer in
        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
        if (sourceDir == null) {
            sourceDir = project;
        }
        File dir = sourceDir.getLocation().toFile();
        
        // resolve relative path to the output file
        outFileName = resolveRelativePath(sourceDir.getFullPath(), outputDir.getFullPath()) + outFileName;
        
        try {
            return runViewer(dir, outFileName, reg, addEnv, project);
        } catch (IOException e) {
            throw new CoreException(TexlipsePlugin.stat("Could not start previewer."));
        }
    }

    /**
     * Resolves a relative path from one directory to another.
     * The path is returned as an OS-specific string with
     * a terminating separator.
     * 
     * @param sourcePath a directory to start from 
     * @param outputPath a directory to end up to
     * @return a relative path from sourcePath to outputPath
     */
    private static String resolveRelativePath(IPath sourcePath, IPath outputPath) {

        int same = sourcePath.matchingFirstSegments(outputPath);
        if (same == sourcePath.segmentCount()
                && same == outputPath.segmentCount()) {
            return "";
        }
            
        outputPath = outputPath.removeFirstSegments(same);
        sourcePath = sourcePath.removeFirstSegments(same);
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < sourcePath.segmentCount(); i++) {
            sb.append("..");
            sb.append(File.separatorChar);
        }
        
        for (int i = 0; i < outputPath.segmentCount(); i++) {
            sb.append(outputPath.segment(i));
            sb.append(File.separatorChar);
        }
        return sb.toString();
    }

    /**
     * Run the given viewer in the given directory with the given file.
     * Also start viewer output listener to enable inverse search.
     * 
     * @param dir the directory to run the viewer in
     * @param file the file name command line argument
     * @param reg the viewer attributes
     * @param addEnv additional environment variables or null
     * @return viewer process
     * @throws IOException if launching the viewer fails
     */
    private static Process runViewer(File dir, String file, ViewerAttributeRegistry registry, Map addEnv, IProject project) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        
        String viewer = registry.getActiveViewer();
        String command = registry.getCommand();
        String arguments = registry.getArguments();

        ArrayList list = new ArrayList();
        if (command.indexOf(' ') > 0) {
            command = "\"" + command + "\"";
        }
        list.add(command);

        String args = null;
        if (arguments.indexOf(FILENAME_PATTERN) >= 0) {
            args = arguments.replaceAll(FILENAME_PATTERN, escapeBackslashes(file));
        } else {
            args = arguments + " " + file;
        }
        PathUtils.tokenizeEscapedString(args, list);
        
        Properties env = PathUtils.getEnv();
        if (addEnv != null) {
            env.putAll(addEnv);
        }
        String envp[] = PathUtils.getStrings(env);
        
        BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("viewerRunning") + " " + command + " " + args);
        Process proc = runtime.exec((String[]) list.toArray(new String[0]), envp, dir);
        startOutputListener(proc.getInputStream(), registry.getInverse(), project);
        new Thread(new ViewerErrorScanner(proc)).start();
        return proc;
    }

    /**
     * Escapes backslashes, so that the string can be given to String.replaceAll()
     * as argument without the backslashes disappearing. 
     * @param file input string, typically a filename
     * @return the input string with backslashes doubled
     */
    private static String escapeBackslashes(String file) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < file.length(); i++) {
            char c = file.charAt(i);
            sb.append(c);
            if (c == '\\') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Start a listener thread for the viewer program's standard output.
     * 
     * @param in input stream where the output of a viewer program will be available
     * @param viewer the name of the viewer
     */
    private static void startOutputListener(InputStream in, String inverse, IProject project) {
        
        if (inverse.equals(ViewerAttributeRegistry.INVERSE_SEARCH_RUN)) {
            
            FileLocationServer server = FileLocationServer.getInstance();
            server.setListener(new FileLocationOpener(project));
            if (!server.isRunning()) {
                new Thread(server).start();
            }
        } else if (inverse.equals(ViewerAttributeRegistry.INVERSE_SEARCH_STD)) {
            new Thread(new ViewerOutputScanner(project, in)).start();
        }
    }
}
