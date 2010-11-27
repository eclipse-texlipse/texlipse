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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sourceforge.texlipse.DDEClient;
import net.sourceforge.texlipse.PathUtils;
import net.sourceforge.texlipse.SelectedResourceManager;
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.BuilderRegistry;
import net.sourceforge.texlipse.builder.TexlipseNature;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.viewer.util.FileLocationListener;
import net.sourceforge.texlipse.viewer.util.FileLocationServer;
import net.sourceforge.texlipse.viewer.util.ViewerErrorScanner;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;


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
 * @author Tor Arne Vestbø
 * @author Boris von Loesch
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
    
    // the source file name variable in the arguments with absolute path
    public static final String TEX_FILENAME_FULLPATH_PATTERN = "%fulltexfile";

    // viewer attributes
    private ViewerAttributeRegistry registry;

    // environment variables to add to current environment
    private Map envSettings;

    // the current project
    private IProject project;
    
       
    /**
     * Run the viewer configured in the given viewer attributes.
     * First check if there is a viewer already running,
     * and if there is, return that process.
     * 
     * @param reg the viewer attributes
     * @param addEnv additional environment variables, or null
     * @param monitor monitor for process
     * @return the viewer process
     * @throws CoreException if launching the viewer fails
     */
    public static Process preview(ViewerAttributeRegistry reg, Map addEnv, IProgressMonitor monitor) throws CoreException {
           	
    	ViewerManager mgr = new ViewerManager(reg, addEnv);
        if (!mgr.initialize()) {
            return null;
        }
        
        Process process = mgr.getExisting();
        if (process != null) {        	
        	// Can send DDE right away
        	mgr.sendDDEViewCommand();
			
        	return process;
        }
                
        // Process must be started first
    	process = mgr.execute();
    	
        // Send DDE if on Win32
    	if (Platform.getOS().equals(Platform.OS_WIN32)) {
        	// Since the process can take a while to start we must wait 
        	// to "make sure" the DDE command gets there. 
        	// This is probably a HACK: should be fixed/changed.
       	   		
        	try {
        		Thread.sleep(1000); // 1000 enough? Who knows!?
        		// The timeout should probably be a config setting?
			} catch (InterruptedException e) {
			}
			
        	mgr.sendDDEViewCommand();
        }
        
        return process;
    }
    
    /**
     * Closes the target output document using the DDE command from the
     * default viewer, or the most recently launched preview. This method
     * is probably fragile since the process and launches handling in
     * Texlipse is too weak to always know what documents are locked and
     * needs closing.  
     * 
     * @throws CoreException
     */
    public static void closeOutputDocument() throws CoreException {
		
        ViewerAttributeRegistry registry = new ViewerAttributeRegistry();
		
    	// Check to see if we have a running launch configuration which should
        // override the DDE close command
	 	ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
	    ILaunch[] launches = manager.getLaunches();
	    
	    for (int i = 0; i < launches.length; i++) {
	    	ILaunch l = launches[i];
	    	ILaunchConfiguration configuration = l.getLaunchConfiguration();
	    	if (configuration != null && configuration.exists() && configuration.getType().getIdentifier().equals(
	    			TexLaunchConfigurationDelegate.CONFIGURATION_ID)) {
	    		Map regMap = configuration.getAttributes();
		        registry.setValues(regMap);
		        break;
	    	}
		}
	    
    	ViewerManager mgr = new ViewerManager(registry, null);    	
		
		 if (!mgr.initialize()) {
	         return;
	     }
		
		// Can only close documents opened by DDE commands themselves
		Process process = mgr.getExisting();        
		if (process != null) {       
			mgr.sendDDECloseCommand();
            
			try {
                Thread.sleep(500); // A small delay required
            } catch (InterruptedException e) {
                // swallow
            }

            returnFocusToEclipse(false);
		}
	}
    
    /**
     * Returns the application focus to Eclipse after launching an
     * external previewer. This is done by first activating the
     * eclipse window, and then setting focus in the editor in a
     * worker thread. Note the delay needed before setting focus. 
     * 
     * @param useMinimizeTrick if true uses a trick to force focus by
     *                         minimizing and restoring the eclipse window 
     */
    public static void returnFocusToEclipse(final boolean useMinimizeTrick) {

        // Return focus/activation to Eclipse/Texlipse
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (null != display) {
            display.asyncExec(new Runnable() {
                public void run() {
                    IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
                    for (int i = 0; i < workbenchWindows.length; i++) {
                        Shell shell = workbenchWindows[i].getShell();

                        if (useMinimizeTrick) {
                            shell.setMinimized(true);
                            shell.setMinimized(false);
                        }

                        shell.setActive();
                        shell.forceActive();
                        break;
                    }
                }
            });
        }
        
        // Spawn thread to set focus in the editor after the launch has completed
        // The reason we cannot do this in the current thread is because the progress
        // window is in the way and will not allow us to set focus on the editor.
        new Thread(new Runnable() {
            public void run() {
                  try { Thread.sleep(500); } catch (InterruptedException e) { }
                  Display display = PlatformUI.getWorkbench().getDisplay();
                  if (null != display) {
                      display.asyncExec(new Runnable() {
                          public void run() {
                              IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
                              for (int i = 0; i < workbenchWindows.length; i++) {
                                  IWorkbenchPage activePage = workbenchWindows[i].getActivePage();
                                  activePage.activate(activePage.getActiveEditor());
                                  // Although setFocus should not be called by clients it is
                                  // required for the focus to work. Activate alone is not enough.
                                  activePage.getActiveEditor().setFocus(); 
                              }
                          }
                      });
                  }
            }
         }).start();
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
        
        try { // Make sure it's a LaTeX project
			if (project.getNature(TexlipseNature.NATURE_ID) == null)
			{
				return false;
			}
		} catch (CoreException e) {			
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
            if (o instanceof HashMap) {
            	HashMap viewerInfo = (HashMap) o;
            	
                Process p = (Process) viewerInfo.get("process");
                String cmd = (String) viewerInfo.get("command"); 

                int code = -1;
                try {
                    code = p.exitValue();
                } catch (IllegalThreadStateException e) {
                }
                
                // there is a viewer running and forward search is not supported    
                if (code == -1 && !registry.getForward()) {
                    // ... so don't launch another viewer window
                    return p;
                } else if (cmd.toLowerCase().indexOf("acrobat.exe") > -1 && code == 1) {
                	// This is a fix for Acrobat Professional returning 1 even 
                	// though it's still running. Probably because it's using a
                	// launcher process of some kind which spawns the real acrobat.
                	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	                	try {
	                		String s = "";
	                		Runtime Rt = Runtime.getRuntime();
	                		InputStream ip = Rt.exec("tasklist").getInputStream();
	                		BufferedReader in = new BufferedReader(new InputStreamReader(ip));
	                		while ((s = in.readLine()) != null) {
	                			if (s.toLowerCase().indexOf("acrobat.exe") > -1)
	                				return p;
	                		}
	                	} catch (IOException e) {
	                	}
                	}
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
        
        IResource outputRes = getOuputResource(project);
        
        if (outputRes == null || !outputRes.exists()) {
            String msg = TexlipsePlugin.getResourceString("viewerNothingWithExtension");
            BuilderRegistry.printToConsole(msg.replaceAll("%s", registry.getFormat()));
            return null;
        }

        // resolve the directory to run the viewer in  
        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
        File dir = sourceDir.getLocation().toFile();
                
        try {
            return execute(dir);
        } catch (IOException e) {
            throw new CoreException(TexlipsePlugin.stat("Could not start previewer '"
                + registry.getActiveViewer() + "'. Please make sure you have entered "
                + "the correct path and filename in the viewer preferences.", e));
        }
    }

    protected void sendDDEViewCommand() throws CoreException {

    	if (Platform.getOS().equals(Platform.OS_WIN32)) {
        	String command =  translatePatterns(registry.getDDEViewCommand());
        	String server = registry.getDDEViewServer();
        	String topic = registry.getDDEViewTopic();

        	int error = DDEClient.execute(server, topic, command);
            if (error != 0) {
                String errorMessage = "DDE command " + command + " failed! " +
                "(server: " + server + ", topic: " + topic + ")";
                TexlipsePlugin.log(errorMessage, new Throwable(errorMessage));
            }
    	}
    }
    
    protected void sendDDECloseCommand() throws CoreException {

    	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    	String command =  translatePatterns(registry.getDDECloseCommand());
	    	String server = registry.getDDECloseServer();
	    	String topic = registry.getDDECloseTopic();
	
	    	int error = DDEClient.execute(server, topic, command);
	  		if (error != 0) {
              String errorMessage = "DDE command " + command + " failed! " +
              "(server: " + server + ", topic: " + topic + ")";
              TexlipsePlugin.log(errorMessage, new Throwable(errorMessage));
            }
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
    public static String resolveRelativePath(IPath sourcePath, IPath outputPath) {

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
     * Determines the Resource which should be shown. Respects partial builds.
     * @return
     * @throws CoreException
     */
    public static IResource getOuputResource(IProject project) throws CoreException { 
    	
    	String outFileName = TexlipseProperties.getOutputFileName(project);
        if (outFileName == null || outFileName.length() == 0) {
            throw new CoreException(TexlipsePlugin.stat("Empty output file name."));
        }
        
        // find out the directory where the file should be
        IContainer outputDir = null;
//        String fmtProp = TexlipseProperties.getProjectProperty(project,
//                TexlipseProperties.OUTPUT_FORMAT);
//        if (registry.getFormat().equals(fmtProp)) {
            outputDir = TexlipseProperties.getProjectOutputDir(project);
/*        } else {
            String base = outFileName.substring(0, outFileName.lastIndexOf('.') + 1);
            outFileName = base + registry.getFormat();
            outputDir = TexlipseProperties.getProjectTempDir(project);
        }*/
        if (outputDir == null) {
            outputDir = project;
        }
        
        IResource resource = outputDir.findMember(outFileName);
        return resource != null ? resource : project.getFile(outFileName);
    }
    

    /**
     * Returns the current line number of the current page, if possible.
     * 
     * @author Anton Klimovsky
     * @return the current line number of the current page
     */
    private int getCurrentLineNumber() {
        //Fix for Bug: 1637560
        int lineNumber = 0;
        final IWorkbenchPage currentWorkbenchPage = TexlipsePlugin.getCurrentWorkbenchPage();
        if (currentWorkbenchPage != null) {
            final int[] buf = new int[1];
            //This must run in UI thread
            Display.getDefault().syncExec(
                    new Runnable() {
                      public void run(){
                          ISelection selection = currentWorkbenchPage.getSelection();
                          if (selection != null && selection instanceof ITextSelection) {                                  
                              ITextSelection textSelection = (ITextSelection) selection;
                              // The "srcltx" package's line numbers seem to start from 1
                              // it is also the case with latex's --source-specials option
                              buf[0] = textSelection.getStartLine() + 1;
                          }
                      }
                    });
            lineNumber = buf[0];
            }
        
//        lineNumber = SelectedResourceManager.getDefault().getSelectedLine();
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
     * @return viewer process
     * @throws IOException if launching the viewer fails
     */
    private Process execute(File dir) throws IOException, CoreException {

        // argument list
        ArrayList<String> list = new ArrayList<String>();
        
        // add command as arg0
        String command = registry.getCommand();
        if (command.indexOf(' ') > 0) {
            command = "\"" + command + "\"";
        }
        list.add(command);
        
        // add arguments
        String args = translatePatterns(registry.getArguments());
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
        HashMap viewerInfo = new HashMap();
        viewerInfo.put("process", process);
        viewerInfo.put("command", command);
        viewerInfo.put("arguments", args);
        
        TexlipseProperties.setSessionProperty(project,
                TexlipseProperties.SESSION_ATTRIBUTE_VIEWER, viewerInfo );
        
        // start viewer listener
        startOutputListener(process.getInputStream(), registry.getInverse());
        // start error reader
        new Thread(new ViewerErrorScanner(process)).start();
        
        return process;
    }

    /**
     * Fills the %arg of the input pattern with the real values
     * @param input The input pattern
     * @return the filled string
     * @throws CoreException
     */
    private String translatePatterns(String input) throws CoreException {
    	
    	if (input == null) return null;
        
        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);

        if (input.indexOf(FILENAME_PATTERN) >= 0) {
            // resolve relative path to the output file
            IResource outputRes = getOuputResource(project);
            String outFileName = outputRes.getName();
            outFileName = resolveRelativePath(sourceDir.getFullPath(), outputRes.getFullPath());
            outFileName = outFileName.substring(0, outFileName.length() - 1);
            if (outFileName.indexOf(' ') >= 0) {
                //Quote filenames with spaces
                outFileName = "\"" + outFileName +"\"";
            }
            input = input.replaceAll(FILENAME_PATTERN, escapeBackslashes(outFileName));
        }
    	
        if (input.indexOf(FILENAME_FULLPATH_PATTERN) >= 0) {
        	input = input.replaceAll(FILENAME_FULLPATH_PATTERN, escapeBackslashes(getOuputResource(project).getLocation().toOSString()));
        }
        
        if (input.indexOf(LINE_NUMBER_PATTERN) >= 0) {
        	input = input.replaceAll(LINE_NUMBER_PATTERN, "" + getCurrentLineNumber());
        }
        
        if (input.indexOf(TEX_FILENAME_PATTERN) >= 0) {
        	
        	IResource selectedRes = SelectedResourceManager.getDefault().getSelectedResource();
        	if (selectedRes.getType() != IResource.FOLDER) {
                selectedRes = SelectedResourceManager.getDefault().getSelectedTexResource();
            }
        	
        	String relPath = resolveRelativePath(sourceDir.getFullPath(), 
                    selectedRes.getFullPath().removeLastSegments(1));
            String texFile = relPath + selectedRes.getName();
        	input = input.replaceAll(TEX_FILENAME_PATTERN, escapeBackslashes(texFile));
        }
        
        if (input.indexOf(TEX_FILENAME_FULLPATH_PATTERN) >= 0) {
            IResource selectedRes = SelectedResourceManager.getDefault().getSelectedResource();
            if (selectedRes.getType() != IResource.FOLDER) {
                selectedRes = SelectedResourceManager.getDefault().getSelectedTexResource();
            }

            input = input.replaceAll(TEX_FILENAME_FULLPATH_PATTERN, escapeBackslashes(selectedRes.getLocation().toOSString()));
        }
        
        return input;
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
    private void startOutputListener(final InputStream in, String inverse) {
        
        if (inverse.equals(ViewerAttributeRegistry.INVERSE_SEARCH_RUN)) {
            
            FileLocationServer server = FileLocationServer.getInstance();
            server.setListener(new FileLocationOpener(project));
            if (!server.isRunning()) {
                new Thread(server).start();
            }
            
            //Read everything from InputStream, otherwise the process will stay open in some cases
            //happens e.g. with sumatrapdf
            new Thread(new Runnable(){
                public void run() {
                    InputStream st = new BufferedInputStream(in);
                    try {
                        
                        byte[] buf = new byte[1024];
                        //read as long as the process exists and dump its content
                        while (st.read(buf) != -1) {
                            //System.out.println(new String(buf));
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {}
                        }
                        st.close();
                    } catch (IOException e) {
                    }

                }
            }).start();
        } else if (inverse.equals(ViewerAttributeRegistry.INVERSE_SEARCH_STD)) {
            new Thread(new ViewerOutputScanner(project, in)).start();
        }
    }
}
