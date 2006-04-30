package net.sourceforge.texlipse.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.ParseErrorMessage;
import net.sourceforge.texlipse.model.ReferenceContainer;
import net.sourceforge.texlipse.model.TexProjectParser;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;

public class TexProjectOutline {

    private IProject currentProject;
    private List topLevelNodes;
    private Map outlines = new HashMap();
    private IFile currentTexFile;
    private OutlineNode virtualTopNode;
    private List errors;
    private TexProjectParser fileParser;
    
    public TexProjectOutline(IProject currentProject,
            ReferenceContainer labels, ReferenceContainer bibs) {
        // TODO
        this.currentProject = currentProject;
        fileParser = new TexProjectParser(currentProject, labels, bibs);
        this.errors = new ArrayList();
    }

    /**
     * @param nodes The outline tree top
     * @param fileName The path of the source file relative to the
     *                 project's base directory
     */
    public void addOutline(List nodes, String fileName) {
        outlines.put(fileName, nodes);
        
        IFile mainFile = TexlipseProperties.getProjectSourceFile(currentProject);
        
        if (fileName.equals(mainFile.getName())) {
            this.topLevelNodes = nodes;
        }
    }
    

    
    public List getErrors() {
        return errors;
    }

    /**
     * Returns the complete outline starting with the main file
     * and displaying all the files that are included from the main
     * file
     * 
     * @return List containing <code>outlineNode</code>s
     */
    public List getFullOutline() {
        errors.clear();

        virtualTopNode = new OutlineNode("Entire document", OutlineNode.TYPE_DOCUMENT, 0, null);
        
        currentTexFile = TexlipseProperties.getProjectSourceFile(currentProject);
        if (topLevelNodes == null) {
            topLevelNodes = fileParser.parseFile(currentTexFile);
            String fullName = currentTexFile.getFullPath().removeFirstSegments(1).toString();
            outlines.put(fullName, topLevelNodes);
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
     * @param parent
     * @param insertList
     * @param texFile
     * @return The last of the topmost inserted nodes
     */
    private OutlineNode replaceInput(OutlineNode parent, List insertList, IFile texFile) {
        // An input node should never have any children
        // We need to raise the level depending on the type of the 1st node in the new outline
        
        if (insertList.size() == 0) {
            return parent;
        }
        /*
        OutlineNode firstNode = (OutlineNode) insertList.get(0);
        // FIXME do a real comparison method here instead, this doesn't work always
        while (firstNode.getType() <= parent.getType()) {
            parent = parent.getParent();
        }
        */
        for (Iterator iter2 = insertList.iterator(); iter2.hasNext();) {
            OutlineNode oldNode2 = (OutlineNode) iter2.next();
            
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
        List newChildren = parent.getChildren();
        return (OutlineNode) newChildren.get(newChildren.size()-1);
    }
    
    /**
     * Adds OutlineNodes in children to main copying the children in the
     * process.
     * 
     * @param main
     * @param children
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
                List nodes = loadInput(node.getName());
                replaceInput(main, nodes, currentTexFile);
//                OutlineNode newMain = getParentLevel(virtualTopNode.getChildren(), main.getType());
//                main = newMain == null ? main : newMain;
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
     * Loads input from the given file name
     * 
     * @param name
     * @return
     */
    private List loadInput(String name) {
        currentTexFile = fileParser.findIFile(name, currentTexFile);
        if (currentTexFile == null) {
            errors.add(new ParseErrorMessage(1,
                    0,
                    0,
                    "File " + name + " was not found",
                    IMarker.SEVERITY_ERROR));
            return new ArrayList();
        }
        String fullName = currentTexFile.getFullPath().removeFirstSegments(1).toString();
        List nodes = (List) outlines.get(fullName);
        if (nodes == null) {
            nodes = fileParser.parseFile();
            outlines.put(fullName, nodes);
        }
        if (nodes == null) {
            errors.add(new ParseErrorMessage(1,
                    0,
                    0,
                    "Unable to parse " + name + "",
                    IMarker.SEVERITY_ERROR));
            return new ArrayList();            
        }
        return nodes;
    }
}
