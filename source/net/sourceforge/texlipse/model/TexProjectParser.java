package net.sourceforge.texlipse.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TexDocumentParseException;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.TexParser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public class TexProjectParser {

    private IProject currentProject;
    private int position;
    private IFile mainFile;
    private IFile changedFile;
    private String changedInput;

    private IFile file;
    
    private TexParser parser;
    
    private ReferenceContainer labels;
    private ReferenceContainer bibs;
    
    private static String TEX_FILE_ENDING = ".tex";

    
    public TexProjectParser(IProject currentProject, 
            ReferenceContainer labels, ReferenceContainer bibs) {
        this.currentProject = currentProject;
        this.labels = labels;
        this.bibs = bibs;
    }

    /**
     * Parses the document. Parses the complete project with its inputs recursively.
     * At the first round the complete project is parsed. Then only the changed
     * parts will be parsed again and the outlineTree will be generated.
     * 
     * @param labels the label container.
     * @param bibs the bib container.
     */
    private void parseDocument(String input) throws TexDocumentParseException {
        if (this.parser == null) {
            this.parser = new TexParser(null);
        }
        try {
            //String input = readFile(mainFile);
            this.parser.parseDocument(labels, bibs, input);
        } catch (IOException e) {
            TexlipsePlugin.log("Can't read file.", e);
            throw new TexDocumentParseException(e);
        }
    }

    public IFile findIFile(String fileName, IFile referringFile) {

        // Append default ending
        if (fileName.indexOf('.') == -1) { 
            fileName += TEX_FILE_ENDING;
        }
        
        IPath path = referringFile.getFullPath();
        path = path.removeFirstSegments(1).removeLastSegments(1).append(fileName);
        //IResource res = currentProject.findMember(path);
        //res.exists();
        file = currentProject.getFile(path);
        //boolean f = file.exists();
        
        /*
        String dir = TexlipseProperties.getProjectProperty(currentProject,
                TexlipseProperties.SOURCE_DIR_PROPERTY);
        if (dir != null && dir.length() > 0) {
            file = currentProject.getFolder(dir).getFile(fileName);
        } else {
            file = currentProject.getFile(fileName);
        }
        */

        return (file.exists() ? file : null);
    }
    
    public List parseFile(IFile file) {
        this.file = file;
        return this.parseFile();
    }
    
    public List parseFile() {
        try {
            String inputContent = readFile(file);
            parseDocument(inputContent);
            // FIXME we need to add the file-info to the OutlineTree here
            return (parser.isFatalErrors() ? null : parser.getOutlineTree());
        } catch (Exception e) {

        }
        return null;
    }
    
    /**
     * Reads a file from the project.
     * @param file the file to be read.
     * @return The contents of the file as a String.
     * @throws CoreException
     * @throws IOException
     */
    private String readFile(IFile file) throws IOException {
        String inputContent = "";
        try {
            BufferedReader buf = new BufferedReader(new InputStreamReader(file
                    .getContents()));
        
            String tmp;
            // TODO efficiency
            while ((tmp = buf.readLine()) != null) {
                tmp = tmp.concat("\n");
                inputContent = inputContent.concat(tmp);
            }
            buf.close();
        
        } catch (CoreException e) {
            TexlipsePlugin.log("The file does not exist", e);
            throw new IOException(e.getMessage());
        }

        // TODO
        //return this.rmTrailingWhitespace(inputContent);
        return inputContent;
    }

    
}
