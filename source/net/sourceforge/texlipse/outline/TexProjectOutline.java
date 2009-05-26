/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.outline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.MarkerHandler;
import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.TexProjectParser;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

/**
 * Container for an outline representing the entire project
 * 
 * @author Oskar Ojala
 */
public class TexProjectOutline {

    private IProject currentProject;
    private List<OutlineNode> topLevelNodes;
    private OutlineNode virtualTopNode;
    private TexProjectParser fileParser;
    private Map<String, List<OutlineNode>> outlines = new HashMap<String, List<OutlineNode>>();
    private Set<String> included = new HashSet<String>();
    
    /**
     * Creates a new project outline
     * 
     * @param currentProject The projet the outline represents
     * @param labels The labels of the project
     * @param bibs The bibliography of the project
     */
    public TexProjectOutline(IProject currentProject) {
        this.currentProject = currentProject;
        this.fileParser = new TexProjectParser(currentProject);
    }

    /**
     * Adds an outline into the project (full document) outline
     * 
     * @param nodes The outline tree top
     * @param fileName The path of the source file relative to the
     *                 project's base directory
     */
    public void addOutline(List<OutlineNode> nodes, String fileName) {
        outlines.put(fileName, nodes);
        
        IFile mainFile = TexlipseProperties.getProjectSourceFile(currentProject);
        String str = mainFile.getFullPath().removeFirstSegments(1).toString();
        
        if (fileName.equals(str)) {
            this.topLevelNodes = nodes;
        }
    }
    
    /**
     * Returns the complete outline starting with the main file
     * and displaying all the files that are included from the main
     * file.
     * 
     * Note that this clears the problem markers from the main file
     * and each included file. 
     * 
     * @return List containing <code>outlineNode</code>s
     */
    public List<OutlineNode> getFullOutline() {
        included.clear();
        virtualTopNode = new OutlineNode("Entire document", OutlineNode.TYPE_DOCUMENT, 0, null);
        
        IFile currentTexFile = TexlipseProperties.getProjectSourceFile(currentProject);
        MarkerHandler marker = MarkerHandler.getInstance();
        marker.clearProblemMarkers(currentTexFile);
        String fullName = getProjectRelativeName(currentTexFile);
        if (topLevelNodes == null) {
            try {
                topLevelNodes = fileParser.parseFile(currentTexFile);
                outlines.put(fullName, topLevelNodes);
            } catch (IOException ioe) {
                TexlipsePlugin.log("Unable to create full document outline; main file is not parsable", ioe);
                return new ArrayList<OutlineNode>();
            }
        }
        included.add(fullName);
        addChildren(virtualTopNode, topLevelNodes, currentTexFile);

        List<OutlineNode> outlineTop = virtualTopNode.getChildren();
        for (Iterator<OutlineNode> iter = outlineTop.iterator(); iter.hasNext();) {
            OutlineNode node = iter.next();
            node.setParent(null);
        }
        return outlineTop;
    }
    
    /**
     * Replaces an input node with the outline that the referred file contains.
     * 
     * @param parent The parent node to add the input to
     * @param insertList The top level nodes of the outline to insert
     * @param texFile The file that contains the nodes in <code>insertList</code>
     */
    private void replaceInput(OutlineNode parent, List<OutlineNode> insertList, IFile texFile) {
        // An input node should never have any children
        // We need to raise the level depending on the type of the 1st node in the new outline
        
        if (insertList.size() == 0) {
            return;
        }
        for (Iterator<OutlineNode> iter2 = insertList.iterator(); iter2.hasNext();) {
            OutlineNode oldNode2 = iter2.next();
            
            if (oldNode2.getType() == OutlineNode.TYPE_INPUT) {
                // replace node with tree
                IFile includedFile = resolveFile(oldNode2.getName(), texFile, oldNode2.getBeginLine());
                if (includedFile != null) {
                    List<OutlineNode> nodes = loadInput(includedFile, texFile, oldNode2.getBeginLine());
                    replaceInput(parent, nodes, includedFile);
                    included.remove(getProjectRelativeName(includedFile));
                }
            } else {
                // TODO do a real comparison method here instead, this doesn't work always
                while (oldNode2.getType() <= parent.getType()) {
                    parent = parent.getParent();
                }
                
                OutlineNode newNode = oldNode2.copy(texFile);
                parent.addChild(newNode);
                newNode.setParent(parent);
                
                List<OutlineNode> oldChildren = oldNode2.getChildren();
                if (oldChildren != null) {
                    // TODO do we need to check parent level?
                    addChildren(newNode, oldChildren, texFile);
                }
            }
        }
    }
    
    /**
     * Adds OutlineNodes in <code>children</code> under the 
     * node <code>main</code> copying the child nodes in the
     * process.
     * 
     * @param main The parent node
     * @param children The child nodes to add to the parent node
     * @param texFile The file that contains the nodes in <code>insertList</code>
     */
    private boolean addChildren(OutlineNode main, List<OutlineNode> children, IFile texFile) {
        boolean insert = false;
        for (Iterator<OutlineNode> iter = children.iterator(); iter.hasNext();) {
            OutlineNode node = iter.next();

            // The tree shape might have changed...
            if (insert) {
                OutlineNode newMain = getParentLevel(virtualTopNode.getChildren(),
                        OutlineNode.getSmallerType(node.getType()));
                main = newMain == null ? virtualTopNode : newMain;
            }
            
            if (node.getType() == OutlineNode.TYPE_INPUT) {
                // replace node with tree
                IFile includedFile = resolveFile(node.getName(), texFile, node.getBeginLine());
                if (includedFile != null) {
                    List<OutlineNode> nodes = loadInput(includedFile, texFile, node.getBeginLine());
                    replaceInput(main, nodes, includedFile);
                    included.remove(getProjectRelativeName(includedFile));
                    insert = true;
                }
            } else {
                OutlineNode newNode = node.copy(texFile);
                main.addChild(newNode);
                newNode.setParent(main);
                List<OutlineNode> oldChildren = node.getChildren();
                if (oldChildren != null) {
                    if (addChildren(newNode, oldChildren, texFile)) {
                        main = getParentLevel(virtualTopNode.getChildren(), main.getType());
                    }
                }
            }
        }
        return insert;
    }

    /**
     * Gets the appropriate parent node for the given level.
     * Typically, the children of the top level node are given
     * as the parameter so that the whole tree is searched. This
     * method is useful for determining the node to insert new nodes
     * under after an include ahs taken place.
     * 
     * @param children The children to start searching from
     * @param level The level (lower limit) to find
     * @return Last node of the given level or the highest level that is
     *         lower than the given level
     */
    private OutlineNode getParentLevel(List<OutlineNode> children, int level) {
        if (children == null || children.size() == 0) {
            return null;
        }
        OutlineNode lastNode = (OutlineNode) children.get(children.size() - 1);
        if (lastNode.getType() == level) {
            return lastNode;
        } else if (lastNode.getType() > level) {
            return level == OutlineNode.TYPE_DOCUMENT ? lastNode.getParent() : null;
        }
        // reverse iteration
        /*
        for (ListIterator li = children.listIterator(children.size() - 1);
             li.hasPrevious();) {
            List nodeChildren = ((OutlineNode) li.previous()).getChildren();
            if (nodeChildren != null) {
                OutlineNode found = getParentLevel(nodeChildren, level);
                if (found != null) {
                    return found;
                }
          
            }
        }*/
        List<OutlineNode> nodeChildren = lastNode.getChildren();
        if (nodeChildren != null) {
            OutlineNode found = getParentLevel(nodeChildren, level);
            if (found != null) {
                return found;
            }
        }
        // Now return the "best match", i.e. the smallest one that's larger
        return lastNode;
    }
    
    /**
     * Resolves from a textual representation the IFile that corresponds
     * to that file in the current project.
     * 
     * @param name The name of the file
     * @param referringFile The file referring to (i.e. including) this file
     * @param lineNumber The line number of the inclusion command
     * @return The corresponding IFile or null if no file was found
     */
    private IFile resolveFile(String name, IFile referringFile, int lineNumber) {
        MarkerHandler marker = MarkerHandler.getInstance();
        //Inclusions are always relative to the main file
        IFile currentTexFile = TexlipseProperties.getProjectSourceFile(currentProject);
        
        IFile newTexFile = fileParser.findIFile(name, currentTexFile);
        if (newTexFile == null) {
/*            marker.createErrorMarker(referringFile,
                    "Could not find file " + name,
                    lineNumber);*/
            return null;
        }
        // TODO check that this doesn't get messed up if the same file is included sevral times
        marker.clearProblemMarkers(newTexFile);
        return newTexFile;
    }
    
    /**
     * Loads the outline from the given file. 
     * 
     * @param newTexFile The file to parse
     * @param referringFile The file referring to (i.e. including) this file
     * @param lineNumber The line number of the inclusion command
     * @return The top level nodes of the parsed file or an empty list if parsing
     * failed
     */
    private List<OutlineNode> loadInput(IFile newTexFile, IFile referringFile, int lineNumber) {
        MarkerHandler marker = MarkerHandler.getInstance();
        
        String fullName = getProjectRelativeName(newTexFile);         
        List<OutlineNode> nodes = outlines.get(fullName);
        if (nodes == null) {
            try {
                nodes = fileParser.parseFile(newTexFile);
                outlines.put(fullName, nodes);
            } catch (IOException ioe) {
                marker.createErrorMarker(referringFile,
                        "Could not parse file " + fullName + ", reason: " + ioe.getMessage(),
                        lineNumber);
                return new ArrayList<OutlineNode>();
            }
        }
        if (!included.add(fullName)) {
            marker.createErrorMarker(referringFile,
                    "Circular include of " + fullName,
                    lineNumber);
            return new ArrayList<OutlineNode>();            
        }
        return nodes;
    }
    
    /**
     * Takes an IFile and returns the path to the file and filename
     * relative to the project root directory.
     * 
     * @param file The file
     * @return Path with filename relative to project
     */
    private String getProjectRelativeName(IFile file) {
        return file.getFullPath().removeFirstSegments(1).toString();
    }
}
