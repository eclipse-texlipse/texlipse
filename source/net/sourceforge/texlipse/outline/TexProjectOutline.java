package net.sourceforge.texlipse.outline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.MarkerHandler;
import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.ReferenceContainer;
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
    private List topLevelNodes;
    private Map outlines = new HashMap();
    private IFile currentTexFile;
    private OutlineNode virtualTopNode;
    private TexProjectParser fileParser;
    
    public TexProjectOutline(IProject currentProject,
            ReferenceContainer labels, ReferenceContainer bibs) {
        this.currentProject = currentProject;
        fileParser = new TexProjectParser(currentProject, labels, bibs);
    }

    /**
     * Adds an outline into the project (full document) outline
     * 
     * @param nodes The outline tree top
     * @param fileName The path of the source file relative to the
     *                 project's base directory
     */
    public void addOutline(List nodes, String fileName) {
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
    public List getFullOutline() {

        virtualTopNode = new OutlineNode("Entire document", OutlineNode.TYPE_DOCUMENT, 0, null);
        
        currentTexFile = TexlipseProperties.getProjectSourceFile(currentProject);
        MarkerHandler marker = MarkerHandler.getInstance();
        marker.clearProblemMarkers(currentTexFile);
        if (topLevelNodes == null) {
            try {
                topLevelNodes = fileParser.parseFile(currentTexFile);
                String fullName = currentTexFile.getFullPath().removeFirstSegments(1).toString();
                outlines.put(fullName, topLevelNodes);
            } catch (IOException ioe) {
                TexlipsePlugin.log("Unable to create full document outline; main file is not parsable", ioe);
                return new ArrayList();
            }
        }
        addChildren(virtualTopNode, topLevelNodes, currentTexFile);

        List outlineTop = virtualTopNode.getChildren();
        for (Iterator iter = outlineTop.iterator(); iter.hasNext();) {
            OutlineNode node = (OutlineNode) iter.next();
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
    private void replaceInput(OutlineNode parent, List insertList, IFile texFile) {
        // An input node should never have any children
        // We need to raise the level depending on the type of the 1st node in the new outline
        
        if (insertList.size() == 0) {
            return;
        }
        for (Iterator iter2 = insertList.iterator(); iter2.hasNext();) {
            OutlineNode oldNode2 = (OutlineNode) iter2.next();
            
            if (oldNode2.getType() == OutlineNode.TYPE_INPUT) {
                List nodes = loadInput(oldNode2.getName(), texFile, oldNode2.getBeginLine());
                replaceInput(parent, nodes, currentTexFile);
                continue;
            }

            // TODO do a real comparison method here instead, this doesn't work always
            while (oldNode2.getType() <= parent.getType()) {
                parent = parent.getParent();
            }
            
            OutlineNode newNode = oldNode2.copy(texFile);
            parent.addChild(newNode);
            newNode.setParent(parent);
            
            List oldChildren = oldNode2.getChildren();
            if (oldChildren != null) {
                // TODO do we need to check parent level?
                addChildren(newNode, oldChildren, texFile);
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
    private boolean addChildren(OutlineNode main, List children, IFile texFile) {
        boolean insert = false;
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            OutlineNode node = (OutlineNode) iter.next();

            // The tree shape might have changed...
            if (insert) {
                OutlineNode newMain = getParentLevel(virtualTopNode.getChildren(),
                        OutlineNode.getSmallerType(node.getType()));
                main = newMain == null ? virtualTopNode : newMain;
            }
            
            if (node.getType() == OutlineNode.TYPE_INPUT) {
                // replace node with tree
                List nodes = loadInput(node.getName(), texFile, node.getBeginLine());
                replaceInput(main, nodes, currentTexFile);
                insert = true;
            } else {
                OutlineNode newNode = node.copy(texFile);
                main.addChild(newNode);
                newNode.setParent(main);
                List oldChildren = node.getChildren();
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
    private OutlineNode getParentLevel(List children, int level) {
        if (children.size() == 0) {
            return null;
        }
        OutlineNode lastNode = (OutlineNode) children.get(children.size() - 1);
        if (lastNode.getType() == level) {
            return lastNode;
        } else if (lastNode.getType() > level) {
            return null;
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
        List nodeChildren = lastNode.getChildren();
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
     * Loads the outline from the given file. 
     * 
     * @param name The name of the file
     * @param referringFile The file referring to (i.e. including) this file
     * @param lineNumber The line number of the inclusion command
     * @return The top level nodes of the parsed file or an empty list if parsing
     * failed
     */
    private List loadInput(String name, IFile referringFile, int lineNumber) {
        MarkerHandler marker = MarkerHandler.getInstance();
        marker.clearProblemMarkers(referringFile);
        
        IFile newTexFile = fileParser.findIFile(name, referringFile);
        if (newTexFile == null) {
            marker.createErrorMarker(referringFile,
                    "Could not find file " + name,
                    lineNumber);
            return new ArrayList();
        }
        String fullName = newTexFile.getFullPath().removeFirstSegments(1).toString();
        List nodes = (List) outlines.get(fullName);
        if (nodes == null) {
            try {
                nodes = fileParser.parseFile();
                outlines.put(fullName, nodes);
            } catch (IOException ioe) {
                marker.createErrorMarker(referringFile,
                        "Could not parse file " + name + ", reason: " + ioe.getMessage(),
                        lineNumber);
                return new ArrayList();
            }
        }
        // TODO impact analysis + should this always be set
        currentTexFile = newTexFile;
        return nodes;
    }
}
