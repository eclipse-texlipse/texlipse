/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.KpsewhichRunner;
import net.sourceforge.texlipse.editor.TexDocumentParseException;
import net.sourceforge.texlipse.texparser.TexParser;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A parser interface for finding and parsing files in a LaTeX-project.
 * 
 * @author Oskar Ojala
 * @author Boris von Loesch
 */
public class TexProjectParser {

    private IProject currentProject;

    private IFile file;
    
    private TexParser parser;
        
    private static final String TEX_FILE_ENDING = ".tex";

    /**
     * Creates a new project parser
     * 
     * @param currentProject The project this parser should belong to
     * @param labels The label container for this project
     * @param bibs The BibTeX container for this project
     */
    public TexProjectParser(IProject currentProject) {
        this.currentProject = currentProject;
    }

    /**
     * Finds the given file from the project and returns it or null
     * if such a file wasn't found. If the file was found in a path outside
     * the project, a link to the file is created.
     * 
     * @param fileName The name of the file to look for
     * @param referringFile The file referring to this file (used for paths)
     * @return The found file or null if it wasn't found
     */
    public IFile findIFile(String fileName, IFile referringFile) {
        return findIFile(fileName, referringFile, currentProject);
    }
    
    /**
     * Finds the given file from the project and returns it or null
     * if such a file wasn't found. If the file was found in a path outside
     * the project, a link to the file is created.
     * 
     * @param fileName The name of the file to look for
     * @param referringFile The file referring to this file (used for paths)
     * @param currentProject
     * @return The found file or null if it wasn't found
     */
    public static IFile findIFile(String fileName, IFile referringFile, IProject currentProject) {

        // Append default ending
        if (fileName.indexOf('.') == -1
                || fileName.lastIndexOf('/') > fileName.lastIndexOf('.')) {
            fileName += TEX_FILE_ENDING;
        }
        IPath path = referringFile.getFullPath();
        path = path.removeFirstSegments(1).removeLastSegments(1).append(fileName);
        IFile file = currentProject.getFile(path);
        if (!file.exists()) {
            //Try Kpsewhich
            KpsewhichRunner filesearch = new KpsewhichRunner();
            try {
                String fName = filesearch.getFile(currentProject, fileName, "latex");
                if (fName.length() > 0) {
                    //Create a link
                    IPath p = new Path(fName);
                    file.createLink(p, IResource.NONE, null);
                }
            } catch (CoreException e) {
                TexlipsePlugin.log("Can't run Kpathsea", e);
            }
        }
        return file.exists() ? file : null;
    }
    
    /**
     * Parses the given file
     * 
     * @param file The file to parse
     * @return Outline tree
     * @throws IOException if the file was not readable
     * @throws TexDocumentParseException if the parsing ended in fatal errors
     */
    public List<OutlineNode> parseFile(IFile file) throws IOException {
        this.file = file;
        return this.parseFile();
    }
    
    /**
     * Parses a file that has been previously found with 
     * <code>findIFile</code>. Note that if the find was not done or
     * it completed unsuccessfully, then the behaviour of this method
     * is undefined.
     * 
     * @return Outline tree or null if parsing was unsuccessful.
     * @throws IOException if the file was not readable
     * @throws TexDocumentParseException if the parsing ended in fatal errors
     */
    private List<OutlineNode> parseFile() throws IOException {
        String inputContent = readFile(file);
        parseDocument(inputContent);
        if (parser.isFatalErrors()) {
            throw new IOException("Unable to parse document successfully");
        }
        return parser.getOutlineTree();
    }
    
    /**
     * Parses the document. Parses the complete project with its inputs recursively.
     * At the first round the complete project is parsed. Then only the changed
     * parts will be parsed again and the outlineTree will be generated.
     * 
     * @param labels the label container.
     * @param bibs the bib container.
     */
    private void parseDocument(String input) throws IOException {
        if (this.parser == null) {
            this.parser = new TexParser(null);
        }
        this.parser.parseDocument(input, false);
    }

    /**
     * Reads a file from the project.
     * 
     * @param file the file to be read.
     * @return The contents of the file as a String.
     * @throws IOException
     */
    private String readFile(IFile file) throws IOException {
        StringBuilder inputContent = new StringBuilder("");
        try {
            BufferedReader buf = new BufferedReader(
                    new InputStreamReader(file.getContents()));
            
            final int length = 10000;
            int read = length;
            char[] fileData = new char[length];
            while (read == length) {
                read = buf.read(fileData, 0, length);
                if (read > 0) {
                    inputContent.append(fileData, 0, read);
                }
            }
            buf.close();
        } catch (CoreException e) {
            // This should be very rare...
            throw new IOException(e.getMessage());
        }
        // TODO
        //return this.rmTrailingWhitespace(inputContent);
        return inputContent.toString();
    }
    
}
