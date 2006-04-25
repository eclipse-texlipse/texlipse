package net.sourceforge.texlipse.outline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.ReferenceContainer;
import net.sourceforge.texlipse.model.TexProjectParser;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

public class TexProjectOutline {

    private IProject currentProject;
    private List topLevelNodes;
    private Map outlines = new HashMap();
    
    private TexProjectParser fileParser;
    
    public TexProjectOutline(IProject currentProject,
            ReferenceContainer labels, ReferenceContainer bibs) {
        // TODO
        this.currentProject = currentProject;
        fileParser = new TexProjectParser(currentProject, labels, bibs);
    }

    public void addOutline(List nodes, String fileName) {
        // FIXME see loadInput() (we use full file name here, but without leading paths)
        // -> should be 'path_from_srcdir/name.tex'
        outlines.put(fileName, nodes);
        
        IFile mainFile = TexlipseProperties.getProjectSourceFile(currentProject);
        
        if (fileName.equals(mainFile.getName())) {
            this.topLevelNodes = nodes;
        }
    }
    
    public List getFullOutline() {

        OutlineNode virtualTopNode = new OutlineNode("Entire document", OutlineNode.TYPE_DOCUMENT, 0, null);
        
        // TODO Fetch topLevelNodes -- check that this works
        if (topLevelNodes == null) {
            IFile mainFile = TexlipseProperties.getProjectSourceFile(currentProject);
            topLevelNodes = fileParser.parseFile(mainFile.getName());
            outlines.put(mainFile.getName(), topLevelNodes);
        }
        addChildren(virtualTopNode, topLevelNodes);

        List outlineTop = virtualTopNode.getChildren();
        for (Iterator iter = outlineTop.iterator(); iter.hasNext();) {
            OutlineNode node = (OutlineNode) iter.next();
            node.setParent(null);
        }
        return outlineTop;
    }
    
    private void replaceInput(OutlineNode parent, List insertList) {
        // An input node should never have any children
        // We need to raise the level depending on the type of the 1st node in the new outline
        if (insertList.size() == 0) {
            return;
        }
        OutlineNode firstNode = (OutlineNode) insertList.get(0);
        // FIXME do a real comparison method here instead, this doesn't work always
        while (firstNode.getType() <= parent.getType()) {
            parent = parent.getParent();
        }
        
        for (Iterator iter2 = insertList.iterator(); iter2.hasNext();) {
            OutlineNode oldNode2 = (OutlineNode) iter2.next();
            OutlineNode newNode = oldNode2.copy();
            parent.addChild(newNode);
            newNode.setParent(parent);
            
            List oldChildren = oldNode2.getChildren();
            if (oldChildren != null) {
                addChildren(newNode, oldChildren);
            }
        }
    }
    
    /**
     * Adds OutlineNodes in children to main copying the children in the
     * process.
     * 
     * @param main
     * @param children
     */
    private void addChildren(OutlineNode main, List children) {
        for (Iterator iter = children.iterator(); iter.hasNext();) {
            OutlineNode node = (OutlineNode) iter.next();
            if (node.getType() == OutlineNode.TYPE_INPUT) {
                // replace node with tree
                List nodes = loadInput(node.getName());
                replaceInput(main, nodes);
            } else {
                OutlineNode newNode = node.copy();
                main.addChild(newNode);
                newNode.setParent(main);
                List oldChildren = node.getChildren();
                if (oldChildren != null) {
                    addChildren(newNode, oldChildren);
                }
            }
        }
    }
    
    private List loadInput(String name) {
        // FIXME here we use the basename, while the insert uses the full filename
        List nodes = (List) outlines.get(name);
        if (nodes == null) {
            // FIXME we need to return something sensible here, not just null
            nodes = fileParser.parseFile(name);
            outlines.put(name, nodes);
        }
        return nodes;
    }
}
