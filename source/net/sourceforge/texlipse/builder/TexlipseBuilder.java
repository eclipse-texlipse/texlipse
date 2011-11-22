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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.TexDocumentModel;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.LatexParserUtils;
import net.sourceforge.texlipse.viewer.ViewerManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
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
	protected IProject[] build(int kind, Map args,
	        IProgressMonitor monitor) throws CoreException {

        final IProject project = getProject();
        final ProjectFileTracking fileTracking = new ProjectFileTracking(project);
        final OutputFileManager fileManager = new OutputFileManager(project, fileTracking);

        Object rebuild = TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.FORCED_REBUILD);

        // Wait for all scheduled parser jobs, since they could change relevant
        // session properties
        for (Job parser : Job.getJobManager().find(TexDocumentModel.PARSER_FAMILY)) {
            int state = parser.getState();
            if (state == Job.WAITING || state == Job.SLEEPING) {
                // If waiting, run immediately
                parser.cancel();
                parser.schedule();
            }
            try {
                parser.join();
            } catch (InterruptedException e) {
            }
        }

        if (rebuild == null && fileManager.isUpToDate()) {
            return null;
        }

        BuilderRegistry.clearConsole();

        Object s = TexlipseProperties.getProjectProperty(project,
                TexlipseProperties.PARTIAL_BUILD_PROPERTY);
		if (s != null) {
			partialBuild(project, fileManager, monitor);
		} else {
			buildFile(project, null, fileManager, monitor);
		}

		TexlipseProperties.setSessionProperty(project,
		        TexlipseProperties.FORCED_REBUILD, null);

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
        final ProjectFileTracking fileTracking = new ProjectFileTracking(project);
        final OutputFileManager fileManager = new OutputFileManager(project, fileTracking);

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

        fileManager.cleanTempFiles(monitor);
        fileManager.cleanOutputFile(monitor);

        monitor.subTask(TexlipsePlugin.getResourceString("builderSubTaskCleanMarkers"));

        this.deleteMarkers(project);
        project.refreshLocal(IProject.DEPTH_INFINITE, monitor);
		monitor.done();
	}

    /**
     * Perform a partial build.
     *
     * @param project current project
     * @param fileManager output file manager instance
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void partialBuild(final IProject project,
            final OutputFileManager fileManager,
            final IProgressMonitor monitor) throws CoreException {

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
        	buildFile(project, null, fileManager, monitor);
        	return;
        } else if (LatexParserUtils.findCommand(content, "\\documentclass", 0) != -1
                || LatexParserUtils.findCommand(content, "\\documentstyle", 0) != -1
                || LatexParserUtils.findBeginEnvironment(content, "document", 0) != null) {
            // A complete tex file (just build it)
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE, file);
            buildFile(project, file, fileManager, monitor);
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
        buildFile(project, tmpFile, fileManager, monitor);
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
        Boolean biblatexMode = (Boolean) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_BIBLATEXMODE_PROPERTY);
        Boolean localBib = (Boolean) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_BIBLATEXLOCALBIB_PROPERTY);

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
        if (biblatexMode == null) {
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
        }
        else {
            // Only add bibliography if it is not already included in the current file.
            if (localBib == null) {
                sb.append("\\printbibliography");
            }
        }
        sb.append("\n\\end{document}\n");
        
        return preamble + '\n' + sb.toString();
    }

    /**
     * Builds a document 
     *
     * @param project the current project
     * @param resource the file to build, if <code>null</code> build main document
     * @param fileManager output file manager instance
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    private void buildFile(final IProject project, IFile resource,
            final OutputFileManager fileManager, IProgressMonitor monitor)
                    throws CoreException {

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
                resource = (IFile) checkFileSettings(project, monitor);
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

        // run file processes before build (e.g. moving temp files in)
        fileManager.setCurrentSourceFile(resource);
        fileManager.performBeforeBuild(monitor);

        // start the build
        try {
            builder.build(resource);
        } catch (BuilderCoreException e) {
        }

        // run file processes after build (e.g. moving files out)
        fileManager.performAfterBuild(monitor);

        monitor.done();
    }


    /**
     * Check that the filename settings are correct.
     * 
     * @param project the current project
     * @param monitor progress monitor
     * @return project's main file
     * @throws CoreException if some setting is not correct
     */
    private IResource checkFileSettings(final IProject project,
            IProgressMonitor monitor) throws CoreException {

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
     * Update builder, if necessary, and check that the builder settings are correct.
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
        if (builder instanceof AdaptableBuilder) {
            ((AdaptableBuilder) builder).updateBuilder(project);
        }
        if (builder == null) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorBuilderNumberNotSet").replaceAll("%s", project.getName()).replaceAll("%f", format).replaceAll("%i", number+""));
            throw new CoreException(TexlipsePlugin.stat("Builder (#"
                    + number + ") for " + format + " output format not configured."));
        }
        else if (!builder.isValid()) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("builderErrorBuilderNumberInvalid").replaceAll("%s", project.getName()).replaceAll("%f", format).replaceAll("%i", number+""));
            throw new CoreException(TexlipsePlugin.stat("Builder (#"
                    + number + ") for " + format + " output format has an invalid configuration. Please check"
                    + "if paths to builder programs are set up correctly."));
        }
        
        return builder;
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
