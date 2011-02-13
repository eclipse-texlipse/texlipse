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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    
    //Put this in your document to prevent it from building
    private static final String NO_PARTIAL_BUILD = "%##noBuild";

    /**
     * Build the project.
     * 
	 * @see IncrementalProjectBuilder.build
	 */
    @Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
        
        BuilderRegistry.clearConsole();

        if (isUpToDate(getProject()))
            return null;

        Object s = TexlipseProperties.getProjectProperty(getProject(), TexlipseProperties.PARTIAL_BUILD_PROPERTY);
		if (s != null) {
			partialBuild(monitor);
		} else {
			buildFile(null, monitor);
		}
        
		return null;
	}

    /**
     * Clean the temporary files.
     * 
     * @see IncrementalProjectBuilder.clean
     */
    @Override
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
        
        String[] ext = TexlipsePlugin.getPreferenceArray(TexlipseProperties.TEMP_FILE_EXTS);

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
        this.recursiveTempClean(dir, ext, format, monitor);
    }
    
    /**
     * recursively delete the temp-directory
     * 
     * @param container
     * @param ext
     * @param format
     * @param monitor
     * @throws CoreException
     */
    private void recursiveTempClean(IContainer container, String[] ext,
			String format, IProgressMonitor monitor) throws CoreException {
		if (container == null || !container.exists())
			return;

		IResource files[] = container.members();
		IResource current;

		for (int i = 0; i < files.length; i++) {
			current = files[i];

			if (current instanceof IFolder) {
				IFolder folder = (IFolder)current;
				
				// recursively delete folder
				recursiveTempClean(folder, ext, format, monitor);

				// remove the folder if it's empty (no non-tempfiles left)
				if (folder.members().length == 0) {
					folder.delete(true, monitor);
				}
			} else {
				// current is a file; TODO: check for file?
				if (hasTempFileExtension(current.getName(), ext, format)) {
					current.delete(true, monitor);
				}
			}
			
			monitor.worked(1);
		}
	}
    
    /**
     * Check whether the given file is a temp file.
     * 
     * @param name file name
     * @param ext temp. file extensions
     * @param format build output format
     * @return true, if file is a temporary file created by Latex
     */
    private static boolean hasTempFileExtension(String name, String[] ext, String format) {
        
        for (String e : ext) {
            if (name.endsWith(e)) {
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
        String content = doc.get();
        if (content.indexOf(NO_PARTIAL_BUILD) >= 0) {
            //Do not build this file or anything else
            return;
        }
        else if (resourceName.equals(TexlipseProperties.getProjectProperty(project, TexlipseProperties.MAINFILE_PROPERTY))
                || (!ext.equals("tex") && !ext.equals("ltx"))) {

            // main file can't be built partially
            // also, bib file changes need full build
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, null);
        	buildFile(null, monitor);
        	return;
        } else if (LatexParserUtils.findCommand(content, "\\documentclass", 0) != -1
                || LatexParserUtils.findCommand(content, "\\documentstyle", 0) != -1
                || LatexParserUtils.findBeginEnvironment(content, "document", 0) != null) {
            // A complete tex file (just build it)
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, file);
            buildFile(file, monitor);
            return;
        }
        String tempFileContents = getTempFileContents(file, project, monitor);
        if (tempFileContents == null) {
            //Can not create a valid tmp file
            return;
        }
        //The temp file should be in the main folder
        IContainer folder = TexlipseProperties.getProjectSourceDir(project);
        
        IFile tmpFile = folder.getFile(new Path("tempPartial00000.tex"));
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
        buildFile(tmpFile, monitor);
    }
    
    /**
     * Generate temp file contents. Therefore it includes 
     * the full preamble + \include{file} + bibtex settings
     * 
     * @param file
     * @param project
     * @param monitor
     * @return The content of the tmp file or null if no preamble was found
     * @throws CoreException
     */
    private String getTempFileContents(IFile file, IProject project, final IProgressMonitor monitor) throws CoreException {
        
        // get information from the main file
        String preamble = (String) TexlipseProperties.getSessionProperty(project, TexlipseProperties.PREAMBLE_PROPERTY);
        if (preamble == null) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderNoPreambleFound"));
            return null;
        }
        String bibsty = (String) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBSTYLE_PROPERTY);
        String[] bibli = (String[]) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBFILE_PROPERTY);

        // generate the file contents
        //StringBuffer sb = readFile(file.getContents(), monitor);
        StringBuilder sb = new StringBuilder ("\\input{");
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
            else if (c == ' ') {
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
     * Builds a document 
     * 
     * @param resource the file to build, if <code>null</code> build main document
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void buildFile(IFile resource, IProgressMonitor monitor) throws CoreException {
        
        IProject project = getProject();

        //load settings, if changed on disk
        if (TexlipseProperties.isProjectPropertiesFileChanged(project)) {
            TexlipseProperties.loadProjectProperties(project);
        }
        
        Builder builder = null;
        try {
            builder = checkBuilderSettings(project);
        } catch (CoreException e) {
            // can't get builder, so can't build. error reported to the console
            return;
        }
                
        if (resource == null) {
            //Full build
            try {
                // check settings
                resource = (IFile)checkFileSettings(project, monitor);
            } catch (CoreException e) {}
            if (resource == null) {
                // silent fail. the file doesn't contain enough tags yet
                return; 
            }
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
        sourceDir.refreshLocal(IProject.DEPTH_INFINITE, monitor);
        
        //Get directory that contains the build file 
        final IContainer resSubDir = resource.getParent();
        
        // mark output files as derived
        markOutFile(project, resSubDir);
        try { // possibly move output & temp files away from the source dir
            moveOutput(project, resSubDir, monitor);
        } catch (CoreException e) {
            throw new BuilderCoreException(TexlipsePlugin.stat("Could not write to output file. Please close the output document in your viewer and rebuild."));
        }
        moveTempFiles(project, monitor);
        
        monitor.done();
//-------------------------        

        // build finished successfully, so refresh the directory
        //IContainer sourceDir = resource.getParent();
        //sourceDir.refreshLocal(IProject.DEPTH_INFINITE, monitor);
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
                
        if (resource.getRawLocation().toFile().length() < validDocumentLimit) {
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
     * Mark output file used by Latex program as "derived" to hide them from
     * version control systems. 
     * 
     * @param project the current project
     * @param srcDir the directory where the output file is (where the source file was built)
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void markOutFile(IProject project, IContainer sourceDir) throws CoreException {
        
        String mark = TexlipseProperties.getProjectProperty(project, TexlipseProperties.MARK_DERIVED_PROPERTY);
        if (!"true".equals(mark)) {
            return;
        }
        
        String outputFileName = TexlipseProperties.getOutputFileName(project);
        IResource r = sourceDir.findMember(outputFileName);
        if (r != null) r.setDerived(true);
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
        moveFile(project, sourceDir, outputDir, outputFileName, monitor);
        
        
        //Move the synctex files also, this will not throw an exception, if synctex is not enabled
        String synctexFileName = new Path(outputFileName).removeFileExtension().addFileExtension("synctex.gz").toString();
        moveFile(project, sourceDir, outputDir, synctexFileName, monitor);
        synctexFileName = new Path(outputFileName).removeFileExtension().addFileExtension("synctex").toString();
        moveFile(project, sourceDir, outputDir, synctexFileName, monitor);
        
        // refresh the directories whose contents changed
        sourceDir.refreshLocal(IProject.DEPTH_INFINITE, monitor);
        monitor.worked(1);
        if (outputDir != null) {
            outputDir.refreshLocal(IProject.DEPTH_ONE, monitor);
        } else {
            project.refreshLocal(IProject.DEPTH_ONE, monitor);
        }
        monitor.worked(1);
    }

    
    /**
     * Moves a file to the output directory.
     * 
     * @param project the current project
     * @param srcDir the directory where the output file is (where the source file was built)
     * @param outputDir the destination directory of the file
     * @param fileName the filename of the file to be moved
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
	private void moveFile(IProject project, IContainer sourceDir,
			IFolder outputDir, String fileName,
			IProgressMonitor monitor) throws CoreException {
		
		IResource outputFile = sourceDir.findMember(fileName);
		if (outputFile != null && outputFile.exists()) {
            //Check if the output dir exists, if not create it
            if (outputDir != null && !outputDir.exists()) {
                outputDir.create(true, true, null);
            }

            IResource dest = null;
            if (outputDir != null) {
                dest = outputDir.getFile(fileName);
            } else {
                dest = project.getFile(fileName);
            }
            if (dest == null) return;
            
            if (dest.exists()) {
                File outFile = new File(outputFile.getLocationURI());
                File destFile = new File(dest.getLocationURI());
                try {
                    //Try to move the content instead of deleting the old file
                    //and replace it by the new one. This is better for some viewers like Sumatrapdf
                    FileOutputStream out = new FileOutputStream(destFile);
                    out.getChannel().tryLock();
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(outFile));

                    byte[] buf = new byte[4096];
                    int l;
                    while ((l = in.read(buf)) != -1) {
                        out.write(buf, 0, l);
                    }
                    in.close();
                    out.close();
                    outputFile.delete(true, monitor);
                } catch (IOException e) {
                    // try to delete and move the file
                    dest.delete(true, monitor);
                    outputFile.move(dest.getFullPath(), true, monitor);
                }
            }
            else {
                // move the file
                outputFile.move(dest.getFullPath(), true, monitor);
            }
            monitor.worked(1);
        }
	}

    /**
     * Move temporary files to temp directory and mark them as derived if needed
     * 
     * @param project the current project
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void moveTempFiles(final IProject project, IProgressMonitor monitor) throws CoreException {
        
        final IFolder tempDir = TexlipseProperties.getProjectTempDir(project);
        if (tempDir != null) {
            //Create temp-Folder if it was deleted
            if (!tempDir.exists()) tempDir.create(true, true, monitor);
            
            final IContainer sourceDir = TexlipseProperties.getProjectSourceDir(project);
            if (!sourceDir.exists()) {
                return;
            }
            
            if (tempDir.getFullPath().equals(sourceDir.getFullPath())) {
                return;
            }
            IPath destinationPath = tempDir.getProjectRelativePath().append(sourceDir.getProjectRelativePath());
            final IFolder destination = project.getFolder(destinationPath);
            if (!destination.exists()) destination.create(true, true, monitor);
            
            final String[] tempExts = TexlipsePlugin.getPreferenceArray(TexlipseProperties.TEMP_FILE_EXTS);
            if (tempExts == null || tempExts.length == 0) {
                return;
            }
            project.getWorkspace().run(new IWorkspaceRunnable() {
                public void run(IProgressMonitor monitor) throws CoreException{
		            String format = TexlipseProperties.getProjectProperty(project, TexlipseProperties.OUTPUT_FORMAT);
		            recursiveTempMove(sourceDir, destination, true, tempExts, format, monitor);
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
     * Recursively move all resources of the source container to the 
     * destination container.
     * 
     * @param source
     * @param destination
     * @throws CoreException 
     */
    private static void recursiveTempMove(IContainer source, IContainer destination, boolean createFolders, String[] ext, String format, IProgressMonitor monitor) throws CoreException {
		if (source == null || destination == null)
			return;

		if (source.getFullPath().equals(destination.getFullPath()))
			return;

		if (!source.exists())
			return;

        boolean markAsDerived = false;
		String mark = TexlipseProperties.getProjectProperty(source.getProject(), TexlipseProperties.MARK_DERIVED_PROPERTY);
        if ("true".equals(mark)) {
            markAsDerived = true;
        }

		if (destination instanceof IFolder) {
			if (!destination.exists()){
				if(!createFolders)
					return;
				
				((IFolder) destination).create(true, true, monitor);

				/*
				 * Set destination derived so it will be ignored by version
				 * control system. We can set it here because we will never
				 * create a directory in the sourcefolder. Only in the temp
				 * directory.
				 */
				if (markAsDerived) destination.setDerived(markAsDerived);
			}
		}

		// source and destination are valid.
		// start move
		IResource[] res = source.members();

		for (int i = 0; i < res.length; i++) {
		    IResource current = res[i];

			if (current instanceof IFolder) {
				// We are moving a directory
				IFolder srcFolder = (IFolder) current;
				if (!current.equals(destination)) {
				    //Do not copy tmp folder
				    IPath destinationPath = destination.getFullPath().append(srcFolder.getName());
				    destinationPath = destinationPath.removeFirstSegments(destinationPath.segmentCount() - 1);
				    IFolder destFolder = destination.getFolder(destinationPath);

				    recursiveTempMove(srcFolder, destFolder, createFolders, ext, format, monitor);
				}
			} else {
				// We are moving a file
                if (createFolders == false) {
                    //Move file from tmp back to src
                    IPath newPath = destination.getFullPath().addTrailingSeparator().append(current.getName());
                    if (source.getWorkspace().getRoot().getFile(newPath).exists()) {
                    	//Delete old file first
                    	source.getWorkspace().getRoot().getFile(newPath).delete(true, monitor);
                    }
                    current.move(newPath, true, monitor);
                }			    
                else if (hasTempFileExtension(current.getName(), ext, format)) {
                    //File from src to tmp 
                    //Check if there is a tex file with that name exists in the folder
                    String cc = current.getName().substring(0, current.getName().length()-current.getFileExtension().length());
                    for (IResource r : res) {
                        String name = r.getName();
                        if (name.equals(cc+"tex") || name.equals(cc+"sty") || name.equals(cc+"cls") || name.equals(cc+"ltx")) {
                            
                            if (markAsDerived) current.setDerived(true);

                            IPath newPath = destination.getFullPath().addTrailingSeparator().append(current.getName());
                            current.move(newPath, true, monitor);
                            break;
                        }
                    }
                }
			}
			monitor.worked(1);
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
            tempDir.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            sourceDir.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            
        	//Get tmp dir that corresponds to current src directory
            final IPath sourceTempPath = tempDir.getProjectRelativePath().append(sourceDir.getProjectRelativePath());
            final IFolder sourceTempDir = project.getFolder(sourceTempPath);
            
            if (sourceTempDir.getFullPath().equals(sourceDir.getFullPath())) {
                return;
            }
        	            
            final String[] ext = TexlipsePlugin.getPreferenceArray(TexlipseProperties.TEMP_FILE_EXTS);
            if (ext == null || ext.length == 0) {
                return;
            }
			project.getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					String format = TexlipseProperties.getProjectProperty(
							project, TexlipseProperties.OUTPUT_FORMAT);
							recursiveTempMove(sourceTempDir, sourceDir, false, ext, format, monitor);
				}
				// no need to refresh, because Eclipse API is not going to read
				// these temp files
			}, monitor);
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
     * Delete old build errors and layout markers from project
     * @param project
     * @throws CoreException
     */
    protected void deleteMarkers (IProject project) throws CoreException{
        project.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_INFINITE);
        project.deleteMarkers(LAYOUT_WARNING_TYPE, false, IResource.DEPTH_INFINITE);
    }
}
