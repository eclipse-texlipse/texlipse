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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.LatexParserUtils;
import net.sourceforge.texlipse.viewer.ViewerManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Builder class interfacing with Eclipse API.
 * 
 * @author Kimmo Karlsson
 * @author Boris von Loesch
 */
public class TexlipseBuilder extends IncrementalProjectBuilder {

	// Use fully qualified id as the builder id.
    // Note: this requires the plugin to have the same name as the main java package.
    public static final String BUILDER_ID = TexlipseBuilder.class.getName();

    // marker type for builder problems
    public static final String MARKER_TYPE = TexlipseProperties.PACKAGE_NAME + ".builderproblem";

    // marker type for builder layout warnings
    public static final String LAYOUT_WARNING_TYPE = TexlipseProperties.PACKAGE_NAME + ".layoutproblem";

    // minimum number of characters that a valid latex document can have
    private static final int validDocumentLimit = 10;

    /**
     * Build the project.
     * 
	 * @see IncrementalProjectBuilder.build
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
        
        BuilderRegistry.clearConsole();
        
        Object s = TexlipseProperties.getProjectProperty(getProject(), TexlipseProperties.PARTIAL_BUILD_PROPERTY);
		if (s != null) {
            
			partialBuild(monitor);
		} else {
			fullBuild(monitor);
		}
        
		return null;
	}

    /**
     * Clean the temporary files.
     * 
     * @see IncrementalProjectBuilder.clean
     */
	protected void clean(IProgressMonitor monitor) throws CoreException {
        
        IProject project = getProject();
        BuilderRegistry.clearConsole();

        // reset session variables
        TexlipseProperties.setSessionProperty(project, TexlipseProperties.SESSION_LATEX_RERUN, null);
        TexlipseProperties.setSessionProperty(project, TexlipseProperties.SESSION_BIBTEX_RERUN, null);
        TexlipseProperties.setSessionProperty(project, TexlipseProperties.BIBFILES_CHANGED, null);

        // check main file
        String mainFile = TexlipseProperties.getProjectProperty(project, TexlipseProperties.MAINFILE_PROPERTY);
        if (mainFile == null || mainFile.length() == 0) {
            // main tex file not set -> nothing builded -> nothing to clean
            return;
        }

        cleanTempDir(monitor, project);
        cleanOutput(monitor, project);
        
        monitor.subTask(TexlipsePlugin.getResourceString("builderSubTaskCleanMarkers"));
        
        this.deleteMarkers(project);
        project.refreshLocal(IProject.DEPTH_INFINITE, monitor);
		monitor.done();
	}

    /**
     * Delete the output file.
     * 
     * @param monitor progress monitor
     * @param project current project
     * @throws CoreException if an error occurs
     */
    private void cleanOutput(IProgressMonitor monitor, IProject project) throws CoreException {

        monitor.subTask(TexlipsePlugin.getResourceString("builderSubTaskCleanOutput"));
            
        IResource outputFile = TexlipseProperties.getProjectOutputFile(project);
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete(true, monitor);
        }

        monitor.worked(1);
    }

    /**
     * Delete the temporary files.
     * 
     * @param monitor progress monitor
     * @param project current project
     * @throws CoreException if an error occurs
     */
    private void cleanTempDir(IProgressMonitor monitor, IProject project) throws CoreException {
        
        String tempExts = TexlipsePlugin.getPreference(TexlipseProperties.TEMP_FILE_EXTS);
        if (tempExts == null || tempExts.length() == 0) {
            return;
        }
        String[] ext = tempExts.split(",");

        String format = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT);
        
        IContainer dir = TexlipseProperties.getProjectTempDir(project);
        if (dir == null) {
            dir = project;
        } else if (!dir.exists()) {
            return;
        }
        
        IResource[] files = dir.members();
        
        monitor.beginTask(TexlipsePlugin.getResourceString("builderSubTaskClean"), files.length + 2);
        monitor.subTask(TexlipsePlugin.getResourceString("builderSubTaskCleanTemp"));
        
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            if (isLatexTempFile(name, ext, format)) {
                files[i].delete(true, monitor);
            }
            monitor.worked(1);
        }
    }
    
    /**
     * Check the given file ia a temp file.
     * 
     * @param name file name
     * @return true, if file is a temporary file created by Latex
     */
    private boolean isLatexTempFile(String name, String[] ext, String format) {
        
        for (int i = 0; i < ext.length; i++) {
            if (name.endsWith(ext[i])) {
                return true;
            }
        }
        
        // dvi and ps can also be temporary files at this point
        // pdf can not, because nothing is generated from pdfs
        return (name.endsWith(".dvi") && !"dvi".equals(format))
            || (name.endsWith(".ps") && !"ps".equals(format));
    }
    
    /**
     * Perform a partial build.
     * 
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void partialBuild(final IProgressMonitor monitor) throws CoreException {
        
        IProject project = getProject();
        if (isUpToDate(project))
            return;
        IEditorPart part = TexlipsePlugin.getCurrentWorkbenchPage().getActiveEditor();
        ITextEditor editor = null;
        if (part instanceof ITextEditor)
            editor = (ITextEditor) part;
        // find out the file that should be built partially
        IResource res = (IResource) part.getEditorInput().getAdapter(IResource.class);
        if (res == null || res.getType() != IResource.FILE || !res.getProject().equals(project)) {
            // No file is selected, so user must be browsing 
            // with the navigator. Don't build anything yet.
            return;            
        }
        String resourceName = res.getName();
        int extIndex = resourceName.lastIndexOf('.');
        String ext = resourceName.substring(extIndex+1);
        IDocument doc = editor.getDocumentProvider().getDocument(part.getEditorInput());
        //load settings, if changed on disk
        if (TexlipseProperties.isProjectPropertiesFileChanged(project)) {
            TexlipseProperties.loadProjectProperties(project);
        }

        IFile file = project.getFile(res.getProjectRelativePath());
        if (resourceName.equals(TexlipseProperties.getProjectProperty(project, TexlipseProperties.MAINFILE_PROPERTY))
                || (!ext.equals("tex") && !ext.equals("ltx"))) {

            // main file can't be built partially
            // also, bib file changes need full build
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, null);
        	fullBuild(monitor);
        	return;
        } else if (LatexParserUtils.findCommand(doc, "\\documentclass", 0) != -1
                || LatexParserUtils.findCommand(doc, "\\documentstyle", 0) != -1) {
            // A complete tex file (just build it)
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, file);
            buildPartialFile(file, monitor);
            return;
        }
        String tempFileContents = getTempFileContents(file, project, monitor);

        //The temp file should be in the main folder
        IContainer folder = TexlipseProperties.getProjectSourceDir(project);
        
        IFile tmpFile = createTempFileName(folder);
        TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, tmpFile);
        if (tmpFile == null) {
            throw new CoreException(TexlipsePlugin.stat("Can't create temp file"));
        }
        
        // write temp file
        ByteArrayInputStream bar = new ByteArrayInputStream(tempFileContents.getBytes());
        if (tmpFile.exists()) {
            tmpFile.setContents(bar, true, false, monitor);
        } else {
            tmpFile.create(bar, true, monitor);
        }
        tmpFile.setDerived(true);
        
        // build temp file
        buildPartialFile(tmpFile, monitor);
    }
    
    /**
     * Generate temp file contents. Therefore it includes 
     * the full preamble + \include{file} + bibtex settings
     * 
     * @param file
     * @param project
     * @param monitor
     * @return
     * @throws CoreException
     */
    private String getTempFileContents(IFile file, IProject project, final IProgressMonitor monitor) throws CoreException {
        
        // get information from the main file
        String preamble = (String) TexlipseProperties.getSessionProperty(project, TexlipseProperties.PREAMBLE_PROPERTY);
        String bibsty = (String) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBSTYLE_PROPERTY);
        String[] bibli = (String[]) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBFILE_PROPERTY);

        // generate the file contents
        //StringBuffer sb = readFile(file.getContents(), monitor);
        StringBuffer sb = new StringBuffer ("\\include{");
        String name = ViewerManager.resolveRelativePath(TexlipseProperties.getProjectSourceDir(project).getProjectRelativePath(), 
                file.getProjectRelativePath());
        name = name.substring(0, name.lastIndexOf('.') + 1);
        boolean ws = false;
        if (name.indexOf(' ') >= 0) {
            sb.append('"');
            ws = true;
        }
        //In windows convert the bs to slashes
        for (int i=0; i<name.length() - 1; i++){
            char c = name.charAt(i);
            if (c == File.separatorChar)
                sb.append('/');
            if (c == ' ') {
                sb.append("\\space ");
            }
            else
                sb.append(c);
        }
        //sb.append(name.substring(0, name.length() - file.getFileExtension().length() - 1));
        if (ws) {
            sb.append('"');
        }
        sb.append("}\n");
        if (bibsty != null) {
            sb.append("\\bibliographystyle{");
            sb.append(bibsty);
            sb.append("}\n");
        }
        if (bibli != null && bibli.length > 0) {
            sb.append("\\bibliography{");
            for (int i = 0; i < bibli.length-1; i++) {
                int ext = bibli[i].lastIndexOf('.');
                if (ext >= 0) 
                    sb.append(bibli[i].substring(0, ext));
                else
                    sb.append(bibli[i]);
                sb.append(',');
            }
            if (bibli.length > 1 || !bibli[0].equals(".bib")) { // parser bugfix
                int ext = bibli[bibli.length-1].lastIndexOf('.');
                if (ext >= 0) 
                    sb.append(bibli[bibli.length-1].substring(0, ext));
                else
                    sb.append(bibli[bibli.length-1]);
            }
            sb.append("}\n");
        }
        sb.append("\n\\end{document}\n");
        
        return preamble + '\n' + sb.toString();
    }

    /**
     * Build a partial document. This process does not move Latex temporary
     * files anywhere to save time.
     * 
     * @param resource the file
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void buildPartialFile(IFile resource, IProgressMonitor monitor) throws CoreException {
        
        IProject project = resource.getProject();
        Builder builder = checkBuilderSettings(project);

        // check changed files
        if (isUpToDate(project)) {
            //BuilderRegistry.printToConsole("Project (" + project.getName() + ") already up to date.");
            return;
        }
        
        // number 100 is just some kind of educated guess of how much work there is
        monitor.beginTask(TexlipsePlugin.getResourceString("builderSubTaskPartialBuild"), 100);

        this.deleteMarkers(project);
        // reset builder instance to startable state
        builder.reset(monitor);

        // use temp files from previous build
        moveBackTempFiles(project, monitor);
        
        // start the build
        try {
            builder.build(resource);
        } catch (BuilderCoreException e) {
        }

        // build finished successfully, so refresh the directory
        IContainer sourceDir = resource.getParent();
        sourceDir.refreshLocal(IProject.DEPTH_ONE, monitor);

        // mark temp and output files as derived
        markTempandOutFiles(project, sourceDir);
                
        // move the output file to correct place
        moveOutput(project, sourceDir, monitor);
        moveTempFiles(project, monitor);
        
        monitor.done();
    }

    /**
     * Create a temporary file.
     * @param dir the directory to create the file to
     * @return a non-existent file
     */
    private IFile createTempFileName(IContainer dir) {
        
        String base = "tempPartial";
        String ext = ".tex";
        StringBuffer sb = new StringBuffer("0000");
        
        int n = 0;
        int lim = 10;
        while (sb.length() > 0) {
            
            while (n < lim) {
                IFile f = dir.getFile(new Path(base + sb + n + ext));
//                if (!f.exists()) {
                    return f;
//                }
//                n++;
            }
            
            lim *= 10;
            sb.delete(sb.length()-1, sb.length());
        }
        return null;
    }

    /**
     * Read the entire contents of a file to a single StringBuffer.
     * @param stream input stream to the file
     * @return the contents of the file
     */
    private StringBuffer readFile(InputStream stream, final IProgressMonitor monitor) {

        monitor.subTask(TexlipsePlugin.getResourceString("builderSubTaskReadFile"));
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        String lineFeed = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer();

        try {
            String tmp = null;
            while ((tmp = br.readLine()) != null) {
                sb.append(tmp);
                sb.append(lineFeed);
                monitor.worked(1);
            }
        } catch (IOException e) {
        }

        try {
            br.close();
        } catch (IOException e) {
        }
        
        return sb;
    }

    /**
     * Check that the filename settings are correct.
     * 
     * @param project the current project
     * @param monitor progress monitor
     * @return project's main file
     * @throws CoreException if some setting is not correct
     */
    private IResource checkFileSettings(IProject project, IProgressMonitor monitor) throws CoreException {

        String mainFile = TexlipseProperties.getProjectProperty(project, TexlipseProperties.MAINFILE_PROPERTY);
        if (mainFile == null || mainFile.length() == 0) {
            // maybe not a good idea to report as error, at least when in java-project
            //throw new CoreException(TexlipsePlugin.stat("Main .tex -file name not set."));
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorMainFileNotSet").replaceAll("%s", project.getName()));
        }

        String outputFile = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUTFILE_PROPERTY);
        if (outputFile == null || outputFile.length() == 0) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorOutputFileNotSet").replaceAll("%s", project.getName()));
            throw new CoreException(TexlipsePlugin.stat("Project output file name not set."));
        }

        IFile resource = TexlipseProperties.getProjectSourceFile(project);
        if (resource == null || !resource.exists()) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorMainFileNotFound").replaceAll("%s", project.getName()));
            throw new CoreException(TexlipsePlugin.stat("Main .tex -file not found."));
        }
        
        StringBuffer sb = readFile(resource.getContents(), monitor);
        if (sb.toString().trim().length() < validDocumentLimit) {
            return null;
        }
        
        return resource;
    }

    /**
     * Check that the builder settings are correct.
     * 
     * @param project the current project
     * @return builder for this project
     * @throws CoreException if some setting is not correct
     */
    private Builder checkBuilderSettings(IProject project) throws CoreException {
        
        String format = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT);
        if (format == null || format.length() == 0) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorOutputFormatNotSet").replaceAll("%s", project.getName()));
            throw new CoreException(TexlipsePlugin.stat("Project output file format not set."));
        }
        
        String str = TexlipseProperties.getProjectProperty(project, TexlipseProperties.BUILDER_NUMBER);
        if (str == null) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorOutputBuilderNotSet").replaceAll("%s", project.getName()));
            throw new CoreException(TexlipsePlugin.stat("No builder selected."));
        }
        
        int number = 0;
        try {
            number = Integer.parseInt(str);
        } catch (NumberFormatException e) {
        }
        
        Builder builder = BuilderRegistry.get(number);
        if (builder == null || !builder.isValid()) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorBuilderNumberNotSet").replaceAll("%s", project.getName()).replaceAll("%f", format).replaceAll("%i", number+""));
            throw new CoreException(TexlipsePlugin.stat("Builder (#"
                    + number + ") for " + format + " output format not configured."));
        }
        
        return builder;
    }
    
	/**
	 * Perform a full build.
     * 
	 * @param monitor progress monitor
	 * @throws CoreException if an error occurs (e.g. builder not configured)
	 */
	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
        
        IProject project = getProject();
        
        //load settings, if changed on disk
        if (TexlipseProperties.isProjectPropertiesFileChanged(project)) {
            TexlipseProperties.loadProjectProperties(project);
        }
        
        // check settings
        IResource resource = null;
        try {
            resource = checkFileSettings(project, monitor);
        } catch (CoreException e) {}
        if (resource == null) {
            // silent fail. the file doesn't contain enough tags yet
            return; 
        }

        Builder builder = null;
        try {
            builder = checkBuilderSettings(project);
        } catch (CoreException e) {
            // can't get builder, so can't build. error reported to the console
            return;
        }
        
        // check changed files
        if (isUpToDate(project)) {
            //BuilderRegistry.printToConsole("Project \"" + project.getName() + "\" already up to date.");
            return;
        }
        
        // number 100 is just some kind of guess of how much work there is
        monitor.beginTask(TexlipsePlugin.getResourceString("builderSubTaskBuild"), 100);

        this.deleteMarkers(project);
        monitor.worked(1);

        // reset builder instance to startable state
        builder.reset(monitor);

        // use temp files from previous build
        moveBackTempFiles(project, monitor);
        
        // start the build
        try {
            builder.build(resource);
        } catch (BuilderCoreException e) {
        }

        // build finished successfully, so refresh the directory
        IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
        sourceDir.refreshLocal(IProject.DEPTH_ONE, monitor);
        
        // mark temp and output files as derived
        markTempandOutFiles(project, sourceDir);
        
        try { // possibly move output & temp files away from the source dir
            moveOutput(project, sourceDir, monitor);
        } catch (CoreException e) {
            throw new BuilderCoreException(TexlipsePlugin.stat("Could not write to output file. Please close the output document in your viewer and rebuild."));
        }
        moveTempFiles(project, monitor);
        
		monitor.done();
	}

    /**
     * Check if the given project needs a rebuild.
     * 
     * @param project the current project
     * @return true, if the project is up to date
     */
    private boolean isUpToDate(IProject project) {
        
        long lastBuildStamp = getOutputFileDate(project);
        
        IResource[] files = TexlipseProperties.getAllProjectFiles(project);
        for (int i = 0; i < files.length; i++) {
            long stamp = files[i].getLocalTimeStamp(); 
            if (stamp > lastBuildStamp) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Mark temporary and output files used by Latex program as "derived" to hide them from
     * version control systems. 
     * 
     * @param project the current project
     * @param srcDir the directory where the output file is (where the source file was built)
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void markTempandOutFiles(IProject project, IContainer sourceDir) throws CoreException {
        
        String mark = TexlipseProperties.getProjectProperty(project, TexlipseProperties.MARK_DERIVED_PROPERTY);
        if (!"true".equals(mark)) {
            return;
        }
        
        IResource[] files = sourceDir.members();
        if (files == null) {
            return;
        }
        
        String[] exts = TexlipsePlugin.getPreferenceArray(TexlipseProperties.TEMP_FILE_EXTS);
        String outputFileName = TexlipseProperties.getOutputFileName(project);

        for (IResource file : files) {
            
            String fileName = file.getName();
            if (fileName.equals(outputFileName)) {
                file.setDerived(true);
            }
            else {
                for (String ext : exts) {
                    if (fileName.endsWith(ext)) {
                        file.setDerived(true);
                    }
                }
            }
        }
    }
    
    /**
     * Move output file to output directory.
     * 
     * @param project the current project
     * @param srcDir the directory where the output file is (where the source file was built)
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void moveOutput(IProject project, IContainer sourceDir, IProgressMonitor monitor) throws CoreException {
        
        IFolder outputDir = TexlipseProperties.getProjectOutputDir(project);

        // get paths to the directories
        IPath sourceDirPath = sourceDir.getProjectRelativePath();
        IPath outputDirPath = null;
        if (outputDir != null) {
            outputDirPath = outputDir.getProjectRelativePath();
        } else {
            outputDirPath = project.getProjectRelativePath();
        }
        
        // check if the file is already in the correct place
        if (sourceDirPath.equals(outputDirPath)) {
            return;
        }
        
        String outputFileName = TexlipseProperties.getOutputFileName(project);
        IResource outputFile = sourceDir.findMember(outputFileName);
        if (outputFile != null && outputFile.exists()) {
            
            // delete the old file
            IResource dest = null;
            if (outputDir != null) {
                dest = outputDir.getFile(outputFileName);
            } else {
                dest = project.getFile(outputFileName);
            }
            if (dest != null && dest.exists()) {
                dest.delete(true, monitor);
            }
            
            //Check if the output dir exists, if not create it
            if (outputDir != null && !outputDir.exists()) {
                outputDir.create(true, true, null);
            }
            // move the file
            outputFile.move(dest.getFullPath(), true, monitor);
            monitor.worked(1);
            
            // refresh the directories whose contents changed
            sourceDir.refreshLocal(IProject.DEPTH_ONE, monitor);
            monitor.worked(1);
            if (outputDir != null) {
                outputDir.refreshLocal(IProject.DEPTH_ONE, monitor);
            } else {
                project.refreshLocal(IProject.DEPTH_ONE, monitor);
            }
            monitor.worked(1);
        }
    }

    /**
     * Move temporary files to temp directory.
     * 
     * @param project the current project
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void moveTempFiles(final IProject project, IProgressMonitor monitor) throws CoreException {
        
        final IFolder tempDir = TexlipseProperties.getProjectTempDir(project);
        if (tempDir != null) {
            
            final IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
            if (!sourceDir.exists()) {
                return;
            }
            
            if (tempDir.getFullPath().equals(sourceDir.getFullPath())) {
                return;
            }
            
            final String tempExts = TexlipsePlugin.getPreference(TexlipseProperties.TEMP_FILE_EXTS);
            if (tempExts == null || tempExts.length() == 0) {
                return;
            }
            project.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException{
                    IResource[] res = sourceDir.members();
                    String[] ext = tempExts.split(",");
                    String format = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT);
                    for (int i = 0; i < res.length; i++) {

                        String name = res[i].getName();
                        if (isLatexTempFile(name, ext, format) && res[i].exists()) {
                            if (!tempDir.exists()) {
                                tempDir.create(true, true, null);
                            }

                            IResource dest = tempDir.getFile(name);
                            if (dest != null && dest.exists()) {
                                dest.delete(true, monitor);
                            }

                            res[i].move(dest.getFullPath(), true, monitor);
                            monitor.worked(1);
                        }
                    }
                }
            }, monitor);
            // refresh to reflect the changes of the temp moves
/*            sourceDir.refreshLocal(IProject.DEPTH_ONE, monitor);
            monitor.worked(1);
            tempDir.refreshLocal(IProject.DEPTH_ONE, monitor);
            monitor.worked(1);*/
        }
    }

    /**
     * Move temporary files from temp directory back to source directory.
     * 
     * @param project the current project
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void moveBackTempFiles(final IProject project, IProgressMonitor monitor) throws CoreException {
        
        final IFolder tempDir = TexlipseProperties.getProjectTempDir(project);
        if (tempDir != null && tempDir.exists()) {
            
            final IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
            if (!sourceDir.exists()) {
                return;
            }
            tempDir.refreshLocal(IResource.DEPTH_ONE, monitor);
            sourceDir.refreshLocal(IResource.DEPTH_ONE, monitor);
            
            if (tempDir.getFullPath().equals(sourceDir.getFullPath())) {
                return;
            }
            
            final String[] ext = TexlipsePlugin.getPreferenceArray(TexlipseProperties.TEMP_FILE_EXTS);
            if (ext == null || ext.length == 0) {
                return;
            }

            project.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException{
                    String format = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT);
                    IResource[] res = tempDir.members();
                    for (int i = 0; i < res.length; i++) {

                        String name = res[i].getName();
                        if (isLatexTempFile(name, ext, format) && res[i].exists()) {
                            IPath path = res[i].getProjectRelativePath();
                            path = path.removeFirstSegments(path.segmentCount()-1);
                            IResource dest = sourceDir.getFile(path);
                            if (dest != null && dest.exists()) {
                                dest.delete(true, monitor);
                            }

                            res[i].move(dest.getFullPath(), true, monitor);
                        }
                        monitor.worked(1);
                    }
                }
            }, monitor);

            // no need to refresh, because Eclipse API is not going to read these temp files
        }
    }

    /**
     * Find the output file and get the local time stamp.
     * 
     * @param resource project's main file
     * @param output output format of this project
     * @return the "last modified" -timestamp of the project output file, or -1 if file not found
     */
    private static long getOutputFileDate(IProject project) {

        IResource of = null;
        try {
            of = ViewerManager.getOuputResource(project);
        } catch (CoreException e) {
        }
        //Check for partial build
        if (of != null && of.exists()) {
            return of.getLocalTimeStamp();
        }
        return -1;
    }

    /**
     * The launcher uses this method to check if the project is up-to-date.
     * @return true, if currently open project needs a rebuild
     */
    public static boolean needsRebuild() {
        
        IProject project = TexlipsePlugin.getCurrentProject();
        if (project == null) {
            return false;
        }
        
        IResource resource = TexlipseProperties.getProjectSourceFile(project);
        if (resource == null) {
            return false;
        }
        return resource.getLocalTimeStamp() > getOutputFileDate(project);
    }
    
    /**
     * Delete old build errors and layout markers from project
     * @param project
     * @throws CoreException
     */
    protected void deleteMarkers (IProject project) throws CoreException{
        project.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_INFINITE);
        project.deleteMarkers(LAYOUT_WARNING_TYPE, false, IResource.DEPTH_INFINITE);
    }
}
