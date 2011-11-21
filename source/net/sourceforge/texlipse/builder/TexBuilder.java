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

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.text.StyledEditorKit.BoldAction;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.auxparser.AuxFileParser;
import net.sourceforge.texlipse.model.ReferenceContainer;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.viewer.ViewerManager;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Shell;


/**
 * Build tex-file(s) using latex or pslatex or pdflatex.
 * 
 * @author Kimmo Karlsson
 * @author Tor Arne Vestbø
 * @author Boris von Loesch
 */
public class TexBuilder extends AbstractBuilder implements AdaptableBuilder {

    private boolean biblatexMode;
    private String biblatexBackend;
    private ProgramRunner latex;
    private ProgramRunner bibtex;
    private ProgramRunner makeIndex;
    private ProgramRunner makeIndexNomencl;
    private String output;
    private boolean stopped;
    private int alternative;
    
    public TexBuilder(int i, String outputFormat, int alt) {
        super(i);
        biblatexMode = false;
        biblatexBackend = null;
        output = outputFormat;
        latex = null;
        bibtex = null;
        makeIndex = null;
        alternative = alt;
        isValid();
    }
    
    /**
     * Check if the needed program runners are operational.
     * Update runners from registry if necessary.
     * @return true, if this builder is ready for operation, false otherwise
     */
    public boolean isValid() {
        if (latex == null || !latex.isValid()) {
            latex = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_TEX, output, alternative);
        }
        if (bibtex == null || !bibtex.isValid()) {
            if (!biblatexMode || biblatexBackend == null || "bibtex".equals(biblatexBackend)) {
                bibtex = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_BIB, TexlipseProperties.OUTPUT_FORMAT_AUX, 0);
            }
            else if (biblatexMode && "biber".equals(biblatexBackend)) {
                bibtex = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_BCF, TexlipseProperties.OUTPUT_FORMAT_BBL, 0);
            }
        }
        if (makeIndex == null || !makeIndex.isValid()) {
            makeIndex = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_IDX, TexlipseProperties.OUTPUT_FORMAT_IDX, 0);
        }
        if (makeIndexNomencl == null || !makeIndexNomencl.isValid()) {
            makeIndexNomencl = BuilderRegistry.getRunner(TexlipseProperties.INPUT_FORMAT_NOMENCL, TexlipseProperties.OUTPUT_FORMAT_NOMENCL, 0);
        }
        return latex != null && latex.isValid()
            && bibtex != null && bibtex.isValid()
            && makeIndex != null && makeIndex.isValid();
    }
    
    /**
     * @return output format of the latex processor
     */
    public String getOutputFormat() {
        return latex.getOutputFormat();
    }
    
    /**
     * @return sequence
     */
    public String getSequence() {
        return latex.getProgramName();
    }
    
    public void stopRunners() {
        latex.stop();
        bibtex.stop();
        makeIndex.stop();
        makeIndexNomencl.stop();
        stopped = true;
    }
    
    /**
     * Opens a messabox where the user is asked wether he wants to continue the build
     * @param project
     * @return
     * @throws CoreException
     */
    private boolean askUserForContinue(IProject project) throws CoreException{
        Object c = project.getSessionProperty(new QualifiedName(null, "AlwaysContinueBuilding"));
        if (c == null) {
            // ask the user if he wants to continue
            final Shell shell = TexlipsePlugin.getCurrentWorkbenchPage().getActiveEditor().getSite().getShell();
            final StringBuffer toggle = new StringBuffer();
            final StringBuffer returnCode = new StringBuffer();

            shell.getDisplay().syncExec(new Runnable() {
               public void run() {
                   final MessageDialogWithToggle mess = MessageDialogWithToggle.openOkCancelConfirm(
                           TexlipsePlugin.getCurrentWorkbenchPage().getActiveEditor().getSite().getShell(), 
                           TexlipsePlugin.getResourceString("builderErrorDuringBuildTitle"), 
                           TexlipsePlugin.getResourceString("builderErrorDuringBuild"), 
                           TexlipsePlugin.getResourceString("builderErrorDuringBuildToggle"), false, null, null);
                   if (mess.getToggleState()) {
                       toggle.append(true);
                   }
                   if (mess.getReturnCode() == MessageDialogWithToggle.CANCEL)
                       returnCode.append(false);
                } 
            });
            
            if (toggle.length() > 0) {
                if (returnCode.length() > 0) {
                    project.setSessionProperty(new QualifiedName(null, "AlwaysContinueBuilding"), new Boolean(false));
                } else {
                    project.setSessionProperty(new QualifiedName(null, "AlwaysContinueBuilding"), new Boolean(true));
                }
            }
            if (returnCode.length() > 0) {
                return false;
            }
        } else {
            //we have a saved state
            Boolean b = (Boolean) c;
            if (b.booleanValue() == true)
                return true;
            else
                return false;
        }
        return true;
    }
    
    /**
     * Calculates the name of the root aux-file to be used by the 
     * <code>AuxFileParser</code>.
     * 
     * @param project
     * @return
     */
    private String getAuxFileName(IProject project) {
        // evaluate the .aux file
        String auxFileName = TexlipseProperties.getProjectProperty(project, TexlipseProperties.MAINFILE_PROPERTY);
        //Check for partial build
        Object s = TexlipseProperties.getProjectProperty(project, TexlipseProperties.PARTIAL_BUILD_PROPERTY);
        if (s != null) {
            IFile tmpFile = (IFile)TexlipseProperties.getSessionProperty(project, TexlipseProperties.PARTIAL_BUILD_FILE);
            if (tmpFile != null) {
                auxFileName = tmpFile.getProjectRelativePath().toPortableString();
            }
        }
        auxFileName = auxFileName.replaceFirst("\\.tex$", "\\.aux");
        return auxFileName;
    }
    
    /**
     * Extracts all labels defined in the aux-file and adds them to the
     * label container
     * 
     * @param afp the <code>AuxFileParser</code> used to extract the labels
     */
	private void extractLabels(AuxFileParser afp) {
		ReferenceContainer labelC = (ReferenceContainer) TexlipseProperties
				.getSessionProperty(afp.getProject(),
						TexlipseProperties.LABELCONTAINER_PROPERTY);
		if (labelC != null) {
			// Add temp path to aux-File
			String tempPath = TexlipseProperties.getProjectProperty(afp.getProject(),
					TexlipseProperties.TEMP_DIR_PROPERTY);
			String correctedAuxFileName = tempPath + File.separator
					+ afp.getRootAuxFile();

			// First remove the labels
			labelC.addRefSource(correctedAuxFileName,
					new LinkedList<ReferenceEntry>());
			// and reorganize
			labelC.organize();
			// now add them
			labelC.updateRefSource(correctedAuxFileName, afp.getLabels());
		}
    }

	/**
	 * Clears errors and warnings from the problem view. If LaTeX runs more than once, this
	 * makes sure, the view only shows the messages of the last run, which are still valid.
	 *
	 * @param project the project
	 */
	private void clearMarkers(IProject project) {
        try {
            project.deleteMarkers(TexlipseBuilder.MARKER_TYPE, false, IResource.DEPTH_INFINITE);
            project.deleteMarkers(TexlipseBuilder.LAYOUT_WARNING_TYPE, false, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
        }
	}
    
    /**
     * Run latex and optionally bibtex to produce a dvi file.
     * @throws CoreException if the build fails at any point
     */
    public void buildResource(IResource resource) throws CoreException {
        //boolean error = false;
		stopped = false;
        // Make sure we close the output document first 
    	// (using DDE on Win32)
    	if (Platform.getOS().equals(Platform.OS_WIN32)) {
    		monitor.subTask("Closing output document");    	
    		ViewerManager.closeOutputDocument();
    		monitor.worked(5);    		
    	}
    	
    	IProject project = resource.getProject();
    	boolean parseAuxFiles = TexlipsePlugin.getDefault().getPreferenceStore().getBoolean(TexlipseProperties.BUILDER_PARSE_AUX_FILES);
    	String auxFileName = getAuxFileName(project);
		IResource auxFile = project.getFile(auxFileName);
    	List<String> oldCitations = null;
    
		if (!biblatexMode && parseAuxFiles && auxFile.exists()) {
			// read all citations from the aux-files and save them for later
			AuxFileParser afp = new AuxFileParser(project, auxFileName);
			oldCitations = afp.getCitations();
		}		
    	
    	monitor.subTask("Building document");
        try {
            latex.run(resource);
        } catch (BuilderCoreException ex) {
            //Don't stop here, we will ask the user later
            //TODO: Error managment
            //error = true;
        }
        monitor.worked(10);
        if (stopped)
            return;
        
        String runBib = (String) TexlipseProperties.getSessionProperty(project, TexlipseProperties.SESSION_BIBTEX_RERUN);
        Boolean bibChange = (Boolean) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBFILES_CHANGED);
        IResource runIdx = findIndex(project, resource);
        IResource runNomencl = findNomencl(project, resource);
        
        // if bibtex is not used, maybe the references need to be updated in the main document
        String rerun = (String) TexlipseProperties.getSessionProperty(resource.getProject(), TexlipseProperties.SESSION_LATEX_RERUN);
        
		if (parseAuxFiles && auxFile.exists()) {
			AuxFileParser afp = new AuxFileParser(project, auxFileName);

			if (!biblatexMode) {
    			// check whether a new bibtex run is required
    			List<String> newCitations = afp.getCitations();
    			if (!newCitations.equals(oldCitations))
    				bibChange = new Boolean(true);
			}

			// add the labels defined in the .aux-file to the label container
			extractLabels(afp);
		}
        
        // if bibtex is used, the bibliography might be changed
        String[] bibs = (String[]) TexlipseProperties.getSessionProperty(project, TexlipseProperties.BIBFILE_PROPERTY);
        
        if (bibs != null && bibs.length > 0 && (runBib != null || bibChange != null)) {
            
/*            if (error) {
                if (askUserForContinue(project) == false) {
                    throw new BuilderCoreException(TexlipsePlugin.stat("Errors during build. See the problems dialog."));
                }
            }*/
            
            bibtex.run(resource);
            if (stopped)
                return;
            monitor.worked(10);
            
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.SESSION_BIBTEX_RERUN, null);
            TexlipseProperties.setSessionProperty(project, TexlipseProperties.BIBFILES_CHANGED, null);
            
            if (runIdx != null) {
                makeIndex.run(resource);
                if (stopped)
                    return;
                monitor.worked(10);
            }
            
            if (runNomencl != null)
            {
                // Running makeindex to build nomenclature index
                // when %input.nlo file is detected
                makeIndexNomencl.run(resource);
                if (stopped)
                    return;
                monitor.worked(10);
            }
              
            try {
                latex.run(resource);
            } catch (BuilderCoreException ex) {
                //if (!error)
                //    throw ex;
            }
            if (stopped)
                return;
            monitor.worked(10);
            clearMarkers(project);
            try {
                latex.run(resource);
            } catch (BuilderCoreException ex) {
                //if (!error)
                //    throw ex;
            }
            if (stopped)
                return;
            monitor.worked(10);
            
        } else if (rerun != null || runIdx != null || runNomencl != null) {

/*            if (error) {
                if (askUserForContinue(project) == false) {
                    throw new BuilderCoreException(TexlipsePlugin.stat("Errors during build. See the problems dialog."));
                }
            }*/
            
            if (runIdx != null) {
                makeIndex.run(resource);
                if (stopped)
                    return;
                monitor.worked(10);
            }
            
            if (runNomencl != null)
            {
                // Running makeindex to build nomenclature index
                // when %input.nlo file is detected
                makeIndexNomencl.run(resource);
                if (stopped)
                    return;
                monitor.worked(10);
            }
            
            try {
                latex.run(resource);
            } catch (BuilderCoreException ex) {
                //if (!error)
                    //throw ex;
            }
            if (stopped)
                return;
            monitor.worked(10);
            
            TexlipseProperties.setSessionProperty(resource.getProject(), TexlipseProperties.SESSION_LATEX_RERUN, null);
        }
    }

    public void updateBuilder(IProject project) {
        // Check if runners need to be updated due to changes in BibTeX / BibLaTeX settings
        Boolean newBiblatexMode = (Boolean) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_BIBLATEXMODE_PROPERTY);
        String newBiblatexBackend = (String) TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_BIBLATEXBACKEND_PROPERTY);
        boolean blModeVal = newBiblatexMode != null;
        String blBEVal = newBiblatexBackend != null ? newBiblatexBackend : ""; 
        if (blModeVal != biblatexMode || (biblatexMode && !blBEVal.equals(biblatexBackend))) {
            bibtex = null;
            // isValid will later re-assign the runners
        }
        biblatexMode = blModeVal;
        biblatexBackend = newBiblatexBackend;
    }

    /**
     * Find a handle to the index file of this project.
     * @param project the current project
     * @param source buildable resource inside project 
     * @return handle to index file or null if not found.
     *         Returns null also if the index file is older than the current output file
     */
    private IResource findIndex(IProject project, IResource source) {
        
        IContainer srcDir = TexlipseProperties.getProjectSourceDir(project);
        if (srcDir == null) {
            srcDir = project;
        }
        
        String name = source.getName();
        String idxName = name.substring(0, name.length() - source.getFileExtension().length()) + TexlipseProperties.INPUT_FORMAT_IDX;
        IResource idxFile = srcDir.findMember(idxName);
        if (idxFile == null) {
            return null;
        }
        
        IResource outFile = TexlipseProperties.getProjectOutputFile(project);
        if (outFile.getLocalTimeStamp() > idxFile.getLocalTimeStamp()) {
            return null;
        }
        
        return idxFile;
    }
    
    /**
     * Find a handle to the index file of this project.
     * @param project the current project
     * @param source buildable resource inside project 
     * @return handle to index file or null if not found.
     *         Returns null also if the index file is older than the current output file
     */
    private IResource findNomencl(IProject project, IResource source) {
        
        IContainer srcDir = TexlipseProperties.getProjectSourceDir(project);
        if (srcDir == null) {
            srcDir = project;
        }

        String name = source.getName();
        String nomenclName = name.substring(0, name.length() - source.getFileExtension().length()) + TexlipseProperties.INPUT_FORMAT_NOMENCL;
        IResource idxFile = srcDir.findMember(nomenclName);
    
        if (idxFile == null) {
            return null;
        }
        
        IResource outFile = TexlipseProperties.getProjectOutputFile(project);
        if (outFile.getLocalTimeStamp() > idxFile.getLocalTimeStamp()) {
            return null;
        }
        
        return idxFile;
    }
}
