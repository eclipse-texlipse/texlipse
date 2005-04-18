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
import net.sourceforge.texlipse.SelectedResourceManager;
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
import org.eclipse.jface.preference.IPreferenceStore;


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
 * @author Anton Klimovsky
 * @author Kimmo Karlsson
 */
public class ViewerManager {

    // the file name variable in the arguments
    public static final String FILENAME_PATTERN = "%file";

    // the line number variable in the arguments
    public static final String LINE_NUMBER_PATTERN = "%line";

    // the source file name variable in the arguments
    public static final String TEX_FILENAME_PATTERN = "%texfile";

    // file name with absolute path
    public static final String FILENAME_FULLPATH_PATTERN = "%fullfile";
    
    // viewer attributes
    private ViewerAttributeRegistry registry;

    // environment variables to add to current environment
    private Map envSettings;

    // the current project
    private IProject project;

    // project output file to display 
    private IResource outputRes;

    
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
        
        ViewerManager mgr = new ViewerManager(reg, addEnv);
        if (!mgr.initialize()) {
            return null;
        }
        
        Process process = mgr.getExisting();
        if (process != null) {
            return process;
        }
        
        return mgr.execute();
    }

    /**
     * Construct a new viewer launcher.
     * @param reg viewer attributes
     * @param addEnv environment variables to add to the current environment
     */
    protected ViewerManager(ViewerAttributeRegistry reg, Map addEnv) {
        this.registry = reg;
        this.envSettings = addEnv;
    }
    
    /**
     * Find out the current project.
     * @return true, if success
     */
    protected boolean initialize() {
        
        project = TexlipsePlugin.getCurrentProject();
        if (project == null) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("viewerNoCurrentProject"));
            return false;
        }
        return true;
    }
    
    /**
     * Check if viewer already running.
     * This method returns false also, if the user has enabled multiple viewer instances.
     * @return the running viewer process, or null if viewer has already terminated
     */
    protected Process getExisting() {
        
        Object o = TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_ATTRIBUTE_VIEWER);
        
        if (o != null) {
            
            if (o instanceof Process) {
                Process p = (Process) o;
                
                int code = -1;
                try {
                    code = p.exitValue();
                } catch (IllegalThreadStateException e) {
                }

                // there is a viewer running and forward search is not supported
                if (code == -1 && !registry.getForward()) {
                    // ... so don't launch another viewer window
                    return p;
                }
            }
            
            TexlipseProperties.setSessionProperty(project,
                    TexlipseProperties.SESSION_ATTRIBUTE_VIEWER, null);
        }
        
        return null;
    }
    
    /**
     * Run the viewer configured in the given viewer attributes.
     * Paths are resolved so that the viewer program is run in source directory.
     * The viewer program is given a relative pathname and filename as a command line
     * argument. 
     * 
     * @return the viewer process
     * @throws CoreException if launching the viewer fails
     */
    protected Process execute() throws CoreException {

        //load settings, if changed on disk
        if (TexlipseProperties.isProjectPropertiesFileChanged(project)) {
            TexlipseProperties.loadProjectProperties(project);
        }
        
        // rebuild, if needed
        IPreferenceStore prefs = TexlipsePlugin.getDefault().getPreferenceStore();
        if (prefs.getBoolean(TexlipseProperties.BUILD_BEFORE_VIEW)) {

            if (TexlipseBuilder.needsRebuild()) {
                try {
                    project.build(TexlipseBuilder.FULL_BUILD, new NullProgressMonitor());
                } catch (CoreException e) {
                    // build failed, so no output file
                    return null;
                }
            }
        }
        
        String outFileName = TexlipseProperties.getProjectProperty(project,
                TexlipseProperties.OUTPUTFILE_PROPERTY);
        if (outFileName == null || outFileName.length() == 0) {
            throw new CoreException(TexlipsePlugin.stat("Empty output file name."));
        }
        
        // find out the directory where the file should be
        IContainer outputDir = null;
        String fmtProp = TexlipseProperties.getProjectProperty(project,
                TexlipseProperties.OUTPUT_FORMAT);
        if (registry.getFormat().equals(fmtProp)) {
            outputDir = TexlipseProperties.getProjectOutputDir(project);
        } else {
            String base = outFileName.substring(0, outFileName.lastIndexOf('.') + 1);
            outFileName = base + registry.getFormat();
            outputDir = TexlipseProperties.getProjectTempDir(project);
        }
        if (outputDir == null) {
            outputDir = project;
        }
        
        outputRes = outputDir.findMember(outFileName);
        if (outputRes == null || !outputRes.exists()) {
            String msg = TexlipsePlugin.getResourceString("viewerNothingWithExtension");
            BuilderRegistry.printToConsole(msg.replaceAll("%s", registry.getFormat()));
            return null;
        }

        // resolve the directory to run the viewer in
        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
        if (sourceDir == null) {
            sourceDir = project;
        }
        File dir = sourceDir.getLocation().toFile();
        
        // resolve relative path to the output file
        outFileName = resolveRelativePath(sourceDir.getFullPath(),
                outputDir.getFullPath()) + outFileName;
        
        try {
            return execute(dir, outFileName);
        } catch (IOException e) {
            throw new CoreException(TexlipsePlugin.stat("Could not start previewer.", e));
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
    private String resolveRelativePath(IPath sourcePath, IPath outputPath) {

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
     * Returns the current line number of the current page, if possible.
     * 
     * @author Anton Klimovsky
     * @return the current line number of the current page
     */
    private int getCurrentLineNumber() {

        int lineNumber = 0;
        /* Commented this out, because the selection-code did not work
         * with launch configurations. IWorkbenchPage.getSelection() needs
         * to run in a UI thread and ILaunchConfigurationDelegate is not one of those.
         *  - Kimmo Karlsson
         * 
        IWorkbenchPage currentWorkbenchPage = TexlipsePlugin.getCurrentWorkbenchPage();
        if (currentWorkbenchPage != null) {
            
            ISelection selection = currentWorkbenchPage.getSelection();
            if (selection != null) {
                if (selection instanceof ITextSelection) {
                    ITextSelection textSelection = (ITextSelection) selection;
                    // The "srcltx" package's line numbers seem to start from 1
                    // it is also the case with latex's --source-specials option
                    lineNumber = textSelection.getStartLine() + 1;
                }
            }
        }
        */
        lineNumber = SelectedResourceManager.getDefault().getSelectedLine();
        if (lineNumber <= 0) {
            lineNumber = 1;
        }
        return lineNumber;
    }
    
    /**
     * Run the given viewer in the given directory with the given file.
     * Also start viewer output listener to enable inverse search.
     * 
     * @param dir the directory to run the viewer in
     * @param file the file name command line argument
     * @return viewer process
     * @throws IOException if launching the viewer fails
     */
    private Process execute(File dir, String file) throws IOException {

        // argument list
        ArrayList list = new ArrayList();
        
        // add command as arg0
        String command = registry.getCommand();
        if (command.indexOf(' ') > 0) {
            command = "\"" + command + "\"";
        }
        list.add(command);

        // add arguments
        String args = createArgumentString(file);
        PathUtils.tokenizeEscapedString(args, list);
        
        // create environment
        Properties env = PathUtils.getEnv();
        if (envSettings != null) {
            env.putAll(envSettings);
        }
        //String envp[] = PathUtils.getStrings(env);
        String envp[] = PathUtils.mergeEnvFromPrefs(env, TexlipseProperties.VIEWER_ENV_SETTINGS);
        
        // print command
        BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("viewerRunning")
                + " " + command + " " + args);

        // start viewer process
        Runtime runtime = Runtime.getRuntime();
        Process process = runtime.exec((String[]) list.toArray(new String[0]), envp, dir);
        
        // save attribute
        TexlipseProperties.setSessionProperty(project,
                TexlipseProperties.SESSION_ATTRIBUTE_VIEWER, process);
        
        // start viewer listener
        startOutputListener(process.getInputStream(), registry.getInverse());
        // start error reader
        new Thread(new ViewerErrorScanner(process)).start();
        
        return process;
    }

    /**
     * Format argument string.
     * @param file input file for the viewer
     * @return the argument string
     */
    private String createArgumentString(String file) {
        
        String args = null;
        String argumentsTemplate = registry.getArguments();

        if (argumentsTemplate.indexOf(FILENAME_PATTERN) >= 0) {
            args = argumentsTemplate.replaceAll(FILENAME_PATTERN, escapeBackslashes(file));
        } else {
            args = argumentsTemplate + " " + file;
        }

        if (args.indexOf(LINE_NUMBER_PATTERN) >= 0) {
            args = args.replaceAll(LINE_NUMBER_PATTERN, ""+getCurrentLineNumber());
        }
        
        IContainer srcDir = TexlipseProperties.getProjectSourceDir(project);
        if (srcDir == null) {
            srcDir = project;
        }
        
        IResource selectedRes = SelectedResourceManager.getDefault().getSelectedResource();
        if (selectedRes.getType() != IResource.FOLDER) {
            selectedRes = SelectedResourceManager.getDefault().getSelectedTexResource();
        }
        String relPath = resolveRelativePath(srcDir.getFullPath(), selectedRes.getFullPath().removeLastSegments(1));
        String texFile = relPath + selectedRes.getName();
        
        if (args.indexOf(TEX_FILENAME_PATTERN) >= 0) {
            args = args.replaceAll(TEX_FILENAME_PATTERN, texFile);
        }
        
        if (args.indexOf(FILENAME_FULLPATH_PATTERN) >= 0) {
            args = args.replaceAll(FILENAME_FULLPATH_PATTERN, outputRes.getLocation().toFile().getAbsolutePath());
        }
        
        return args;
    }

    /**
     * Escapes backslashes, so that the string can be given to String.replaceAll()
     * as argument without the backslashes disappearing. 
     * @param file input string, typically a filename
     * @return the input string with backslashes doubled
     */
    private String escapeBackslashes(String file) {
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
    private void startOutputListener(InputStream in, String inverse) {
        
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
