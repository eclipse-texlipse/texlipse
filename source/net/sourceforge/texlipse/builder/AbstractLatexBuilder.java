package net.sourceforge.texlipse.builder;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import net.sourceforge.texlipse.auxparser.AuxFileParser;
import net.sourceforge.texlipse.builder.factory.BuilderDescription;
import net.sourceforge.texlipse.model.PackageContainer;
import net.sourceforge.texlipse.model.ReferenceContainer;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;


/**
 * Abstract class which provides some common methods for Latex builders.
 *
 * @author Matthias Erll
 */
public abstract class AbstractLatexBuilder extends AbstractBuilder {

    private static final String PACKAGE_FILE_EXT = ".sty";
    private static final String CLASS_FILE_EXT = ".cls";

    protected ProgramRunner latex;

    /**
     * Extracts a file name from the given path, using any available path
     * delimiter regardless of the platform. A path without any delimiter
     * is considered invalid, and returns <code>null</code>.
     *
     * @param path path string
     * @return the file name, which is the string segment after the last
     *  path delimiter; if no path delimiter is found, <code>null</code>
     *  is returned.
     */
    private static String extractFileName(final String path) {
        int pathDelim = path.lastIndexOf('/');
        if (pathDelim == -1) {
            pathDelim = path.lastIndexOf('\\');
        }
        if (pathDelim > -1 && pathDelim + 1 < path.length() - 1) {
            return path.substring(pathDelim + 1);
        }
        else {
            return null;
        }
    }

    /**
     * Extracts all labels defined in the aux-file and adds them to the
     * label container
     * 
     * @param afp the <code>AuxFileParser</code> used to extract the labels
     */
    private static void extractLabels(AuxFileParser afp) {
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
     * Extracts the document class and package names from the set of input and external
     * files, which have been detected during a latex run.
     *
     * @param project current project
     */
    private static void extractPackages(IProject project) {
        Set<String> externalNames = getExternalFiles(project);
        Set<IPath> inputFiles = getInputFiles(project);
        final Set<String> packages = new HashSet<String>();
        for (String path : externalNames) {
            if (path.endsWith(PACKAGE_FILE_EXT)) {
                final String name = extractFileName(path);
                if (name != null) {
                    packages.add(OutputFileManager.stripFileExt(name, PACKAGE_FILE_EXT));
                }
            }
            else if (path.endsWith(CLASS_FILE_EXT)) {
                final String name = extractFileName(path);
                if (name != null) {
                    TexlipseProperties.setSessionProperty(project,
                            TexlipseProperties.SESSION_LATEX_DOC_CLASS,
                            OutputFileManager.stripFileExt(name, CLASS_FILE_EXT));
                }
            }
        }
        for (IPath path : inputFiles) {
            final String name = path.lastSegment();
            if (name.endsWith(PACKAGE_FILE_EXT)) {
                packages.add(OutputFileManager.stripFileExt(name, PACKAGE_FILE_EXT));
            }
            else if (name.endsWith(CLASS_FILE_EXT)) {
                TexlipseProperties.setSessionProperty(project,
                        TexlipseProperties.SESSION_LATEX_DOC_CLASS,
                        OutputFileManager.stripFileExt(name, CLASS_FILE_EXT));
            }
        }
    
        Object containerObject = TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.PACKAGECONTAINER_PROPERTY);
        if (containerObject != null) {
            PackageContainer packageContainer = (PackageContainer) containerObject;
            packageContainer.updatePackages(packages);
        }
    }

    /**
     * Retrieves the set of input file as stored by the most recent latex run.
     *
     * @param project current project
     * @return set of project relative file paths
     */
    @SuppressWarnings("unchecked")
    protected static Set<IPath> getInputFiles(IProject project) {
        final Object inputSet = TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_LATEX_INPUTFILE_SET);
        if (inputSet != null) {
            return (Set<IPath>) inputSet;
        }
        else {
            return new HashSet<IPath>();
        }
    }

    /**
     * Retrieves the set of external input files as stored by the most recent latex run.
     *
     * @param project current project
     * @return set of file paths
     */
    @SuppressWarnings("unchecked")
    protected static Set<String> getExternalFiles(IProject project) {
        final Object externalSet = TexlipseProperties.getSessionProperty(project,
                TexlipseProperties.SESSION_LATEX_EXTERNALINPUT_SET);
        if (externalSet != null) {
            return (Set<String>) externalSet;
        }
        else {
            return new HashSet<String>();
        }
    }

    /**
     * Determines the name of the root aux-file to be used by the 
     * <code>AuxFileParser</code>.
     *
     * @param buildResource resource which is currently being built
     * @return potential name of the aux file
     */
    protected static String getAuxFileName(IResource buildResource) {
        final String buildFileName = buildResource.getProjectRelativePath().toPortableString();
        return OutputFileManager.stripFileExt(buildFileName, ".tex").concat(".aux");
    }

    protected static void updateContainers(IResource resource, AuxFileParser afp) {
        final IProject project = resource.getProject();
        if (afp != null) {
            // add the labels defined in the .aux-file to the label container
            extractLabels(afp);
        }
        extractPackages(project);
    }

    /**
     * Clears errors and warnings from the problem view. If LaTeX runs more than once, this
     * makes sure, the view only shows the messages of the last run, which are still valid.
     *
     * @param project the project
     */
    protected static void clearMarkers(IProject project) {
        try {
            project.deleteMarkers(TexlipseBuilder.MARKER_TYPE, false, IResource.DEPTH_INFINITE);
            project.deleteMarkers(TexlipseBuilder.LAYOUT_WARNING_TYPE, false, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
        }
    }

    /**
     * Creates a new Latex builder.
     */
    public AbstractLatexBuilder(BuilderDescription description) {
        super(description);
        this.latex = BuilderRegistry.getRunner(description.getRunnerId());
    }

    @Override
    public boolean isValid() {
        if (latex == null || !latex.isValid()) {
            latex = BuilderRegistry.getRunner(description.getRunnerId());
        }
        return latex != null && latex.isValid();
    }

    @Override
    public abstract void buildResource(IResource resource) throws CoreException;

}
