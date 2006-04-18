package net.sourceforge.texlipse.texparser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.ParseErrorMessage;
import net.sourceforge.texlipse.model.ReferenceContainer;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.lexer.LexerException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Parser front-end for parsing LaTeX files and extracting the information
 * needed for the Full Outline. It parses the project main file and the documents
 * inputs. Only the needed parts of the project are parsed. At the first time it
 * will parse the project main file and all the inputs recursively. When a document
 * changes it will only parse the changed document and its inputs.
 */
public class FullTexParser extends TexParser {
    
    private IProject currentProject;
    private ArrayList outlineTree;
    private OutlineNode parent;
    private int position;
    private IFile mainFile;
    private IFile changedFile;
    private String changedInput;
    
    private static String TEX_FILE_ENDING = ".tex";
    
    /**
     * 
     * @param currentProject the current project.
     * @param mainFile the project main file.
     */
    public FullTexParser(IProject currentProject, IFile mainFile) {
        super();
        this.lparser = new LatexParser();
        this.fatalErrors = false;
        this.mainFile = mainFile;
        this.currentProject = currentProject;
        this.position = -1;
    }
    
    /**
     * Parses the document. Parses the complete project with its inputs recursively.
     * At the first round the complete project is parsed. Then only the changed
     * parts will be parsed again and the outlineTree will be generated.
     * 
     * @param labels the label container.
     * @param bibs the bib container.
     */
    public void parseDocument(ReferenceContainer labels, ReferenceContainer bibs)
    throws IOException {
        //TODO what about the errors
        //Clear old errors
        this.errors = null;
        try {
            // parse partially
            //TODO make this working correct
            changedFile = null;   //<- remove this, to enable reparsing instead of fullparsing
            if (changedFile != null && changedInput != null
                    && !changedFile.equals(mainFile)) {
                // find the parent of the changed input
                findParent(this.outlineTree);
                if (parent != null) {
                    // remove the nodes and parse the changed input
                    for (int i=0; i < this.outlineTree.size(); i++){
                        OutlineNode on = (OutlineNode)this.outlineTree.get(i);
                        if (on.getIFile().equals(changedFile)){
                            this.outlineTree.remove(i);
                            i--;
                        }
                        else{
                            deleteNodes(on);
                        }
                    }
                    
                    LatexLexer lexer = new LatexLexer(new PushbackReader(
                            new StringReader(changedInput), 1024));
                    LatexParser inputParser = new LatexParser();
                    inputParser.parse(lexer, labels, bibs);
                    
                    // add the nodes to the tree
                    ArrayList newNodes = inputParser.getOutlineTree();
                    addFileNames(newNodes, changedFile);
                    addToTree(this.outlineTree, newNodes, this.parent, null);
                    //addToTree(parent.getChildren(), newNodes, null, null);
                    
                    // parse inputs recursively
                    if (inputParser.getInputs().size() > 0) {
                        processInputs(newNodes, inputParser.getInputs(),
                                changedFile, labels, bibs);
                    }
                    // remove temporary variables
                    cleanUp();
                }
                // complete parse
            } else {
                String input;
                // changed file is main file, so we don't have to read the file
                if (changedFile != null && changedInput != null
                        && changedFile.equals(mainFile)) {
                    input = this.rmTrailingWhitespace(changedInput);
                } else {
                    try {
                        input = readFile(mainFile);
                    } catch (CoreException e) {
                        TexlipsePlugin.log(
                                "Error reading from project main file", e);
                        return;
                    }
                }
                this.extractPreamble(input);
                // start the parse
                LatexLexer lexer = new LatexLexer(new PushbackReader(
                        new StringReader(input), 1024));
                if (this.preamble != null) {
                    OutlineNode on = new OutlineNode("Preamble",
                            OutlineNode.TYPE_PREAMBLE, 1, null);
                    lparser.parse(lexer, labels, bibs, on);
                } else {
                    lparser.parse(lexer, labels, bibs);
                }
                
                this.outlineTree = lparser.getOutlineTree();
                this.fatalErrors = lparser.isFatalErrors();
                
                // add the file references and parse the inputs recursively
                addFileNames(this.outlineTree, this.mainFile);
                if (lparser.getInputs().size() > 0) {
                    processInputs(outlineTree, lparser.getInputs(), mainFile,
                            labels, bibs);
                }
                // showTree(outlineTree, 5, "");
            }
        } catch (LexerException e) {
            // we must parse the lexer exception into a suitable format
            String msg = e.getMessage();
            int first = msg.indexOf('[');
            int last = msg.indexOf(']');
            String numseq = msg.substring(first + 1, last);
            String[] numbers = numseq.split(",");
            this.errors = new ArrayList(1);
            this.errors.add(new ParseErrorMessage(Integer.parseInt(numbers[0]),
                    Integer.parseInt(numbers[1]), 2, msg.substring(last + 2),
                    IMarker.SEVERITY_ERROR));
            this.fatalErrors = true;
        }
    }
    
    /**
     * Cleans up the the variables needed for the partial parse.
     */
    private void cleanUp() {
        this.parent = null;
        this.position = -1;
        this.changedFile = null;
        this.changedInput = null;
    }
    
    /**
     * Deletes the nodes of the document which changed from the outline tree.
     * @param parent the OutlineNode whose children are to be deleted.
     */
    private void deleteNodes(OutlineNode parent) {
        ArrayList children = parent.getChildren();
        if (children == null) return;
        for (int i = 0; i < children.size(); i++) {
            OutlineNode on = (OutlineNode) children.get(i);
            if (on.getIFile().equals(changedFile)) {
                children.remove(i);
                i--;
            }
            else {
                if (on.getChildren() != null)
                    deleteNodes(on);
            }
        }
    }
    
    /**
     * Searches the outlineTree for the OutlineNode and the position where the
     * changed input is added recursively.
     * @param children the children of a OutlineNode.
     */
    private void findParent(ArrayList children) {
        if (children == null)
            return;
        for (int i = 0; i < children.size(); i++) {
            OutlineNode on = (OutlineNode) children.get(i);
            if (on.getIFile().equals(changedFile)) {
                this.parent = on;
                this.position = i;
                return;
            } else {
                findParent(on.getChildren());
            }
            if (parent != null)
                return;
        }
    }
    
    /**
     * Reads a file from the project.
     * @param file the file to be read.
     * @return The contents of the file as a String.
     * @throws CoreException
     * @throws IOException
     */
    private String readFile(IFile file) throws CoreException, IOException {
        String inputContent = "";
        BufferedReader buf = new BufferedReader(new InputStreamReader(file
                .getContents()));
        String tmp;
        while ((tmp = buf.readLine()) != null) {
            tmp = tmp.concat("\n");
            inputContent = inputContent.concat(tmp);
        }
        buf.close();
        return this.rmTrailingWhitespace(inputContent);
    }
    
    /**
     * Processes the inputs and adds them to the outlineTree recursively.
     * @param tree The children of a node, where the input nodes should be added.
     * @param inputs the inputs to be added.
     * @param inputFile the file where the inputs came from.
     * @param labels the label container.
     * @param bibs the bib container
     * @return
     * @throws IOException
     * @throws LexerException
     */
    private ArrayList processInputs(ArrayList tree, ArrayList inputs,
            IFile inputFile, ReferenceContainer labels, ReferenceContainer bibs)
    throws IOException, LexerException {
        OutlineNode inputNode;
        for (int i = 0; i < inputs.size(); i++) {
            inputNode = (OutlineNode) inputs.get(i);
            String fileName = inputNode.getName();
            if (fileName.indexOf('.') == -1) 
                fileName += TEX_FILE_ENDING;
            IFile file = null;
            String dir = TexlipseProperties.getProjectProperty(currentProject, TexlipseProperties.SOURCE_DIR_PROPERTY);
            if (dir != null && dir.length() > 0) {
                file = currentProject.getFolder(dir).getFile(fileName);
            }
            else
                file = currentProject.getFile(fileName);

            //IFile file = currentProject.getFile(fileName);
            
            String inputContent;
            try {
                // read the input
                inputContent = readFile(file);
            } catch (CoreException e) {
                if (e.getMessage().endsWith("does not exist.")) {
                    // only add an error if the input is in the current editor
                    // input, because of the marker.
                    String editorInput = TexlipsePlugin
                    .getCurrentWorkbenchPage().getActiveEditor()
                    .getEditorInput().getName();
                    if (editorInput.equals(inputNode.getParent().getIFile()
                            .getName())) {
                        this.errors = new ArrayList(1);
                        //TODO Set marker to the right position (\include)
                        this.errors.add(new ParseErrorMessage(inputNode
                                .getBeginLine(), 7, inputNode.getName()
                                .length(), e.getMessage(),
                                IMarker.SEVERITY_ERROR));
                    }
                } else {
                    TexlipsePlugin.log("Error reading from file"
                            + file.getProjectRelativePath().toOSString(), e);
                }
                inputContent = null;
            }
            if (inputContent != null) {
                // parse the input
                LatexLexer lexer = new LatexLexer(new PushbackReader(
                        new StringReader(inputContent), 1024));
                LatexParser inputParser = new LatexParser();
                inputParser.parse(lexer, labels, bibs);
                
                // add the new nodes to the tree
                ArrayList newNodes = inputParser.getOutlineTree();
                if (newNodes.size() > 0) {
                    addFileNames(newNodes, file);
                    addToTree(tree, newNodes, inputNode, inputFile);
                    
                    // parse inputs recursively
                    if (inputParser.getInputs().size() > 0) {
                        processInputs(newNodes, inputParser.getInputs(), file,
                                labels, bibs);
                    }
                }
            }
        }
        return tree;
    }
    
    /**
     * This routine is a quick hack, could be buggy
     * @param tree
     * @param newNodes
     * @param inputNode
     * @param cIndex
     */
    private void addToTree(ArrayList tree, ArrayList newNodes,
            OutlineNode inputNode, int cIndex) {
        if (inputNode != null) {
            OutlineNode parent = inputNode;
            // find the correct position where to add the newNodes
            int biggest = -1;
            while (parent.getChildren() != null) {
                ArrayList children = parent.getChildren();
                for (int i=0; i<children.size(); i++){
                    OutlineNode child = (OutlineNode) children.get(i);
                    if (child.getType()>=biggest) {
                        biggest = child.getType();
                        parent = child;
                    }
                }
            }
            int index = cIndex;
            // add the nodes to the tree
            OutlineNode on;
            //ArrayList later = new ArrayList();
            int fullIndex = 0;
            for (int k = 0; k < newNodes.size(); k++) {
                on = (OutlineNode) newNodes.get(k);
                while (parent != null && parent.getType() >= on.getType()){
                    OutlineNode newParent = parent.getParent();
                    if (newParent != null) {
                        ArrayList children = newParent.getChildren();
                        int newindex = findPosition (children, parent);
                        index = newindex + 1;
                    } 
                    parent = newParent;
                }
                if (parent == null) {
                    tree.add(fullIndex++, on);
                } else {
                    on.setParent(parent);
                    parent.addChild(on, index++);
                }
            }
        }
    }

    /**
     * Adds the nodes to the tree.
     * @param tree the part of the tree, where the nodes should be added.
     * @param newNodes the nodes to be added
     * @param inputNode the nodes where the input came from.
     * @param file the file where the input came from.
     */
    private void addToTree(ArrayList tree, ArrayList newNodes,
            OutlineNode inputNode, IFile file) {
        // if the new Nodes have a parent node add them at this node
        if (inputNode != null) {
            OutlineNode parent = inputNode.getParent();
            // find the right position where to add thenew nodes at the children
            int index = 0;
            if (parent != null && parent.getChildren() != null) {
                index = findPosition(parent.getChildren(), inputNode, file);
            }
            // add the nodes to the tree
            OutlineNode on = null;
            ArrayList later = new ArrayList();
            //TODO How to determine fullIndex if reparsing
            int fullIndex = tree.size();
            for (int k = 0; k < newNodes.size(); k++) {
                on = (OutlineNode) newNodes.get(k);
                //Check if the current nodes type is smaller then the parents type 
                while (parent != null && parent.getType() >= on.getType()){
                    ArrayList children = parent.getChildren();
                    if (children != null) {
                    	for (int i = index; i < children.size(); i++) {
                    		OutlineNode child = (OutlineNode) children.get(i);
                    		//Put all remaining childs into an arraylist which we append later
                    		later.add(child);
                    		parent.deleteChild(child);
                    	}
                    }
                    OutlineNode newParent = parent.getParent();
                    if (newParent != null) {
                        //Get the correct position
                        children = newParent.getChildren();
                        int newindex = findPosition (children, parent);
                        index = newindex + 1;
                    } 
                    parent = newParent;
                }
                if (parent == null) {
                    tree.add(fullIndex++, on);
                } else {
                    on.setParent(parent);
                    parent.addChild(on, index++);
                }
            }
            if (later.size() > 0)
                addToTree(tree, later, on, 0);
        } 
    }
    
    /**
     * Finds the position of inputNode in the ArrayList.
     * @param tree
     * @param inputNode
     * @return the index of the inputNode
     */
    private int findPosition (ArrayList tree, OutlineNode inputNode) {
        OutlineNode on = inputNode;
        do {
        for (int i = 0; i < tree.size(); i++) {
            OutlineNode element = (OutlineNode) tree.get(i);
            if (element == on)
                return i;
        }
        } while ((on=on.getParent()) != null);
        return -1;
    }
    
    /**
     * Gets the position where the nodes should be added. Either searches for the
     * position or returns it when the position is already available(in case of
     * partial parsing). 
     * @param tree the part of the tree to be searched.
     * @param inputNode the node whose position shall be found.
     * @param file the file, where the inputNode is at.
     * @return the position as an Integer.
     */
    private int findPosition(ArrayList tree, OutlineNode inputNode, IFile file) {
        // in case of the partial parse the position is already known
        // (only at the changed file)
        if (this.position != -1)
            return position;
        
        // if empty add them to the front
        if (tree == null || tree.size() == 0)
            return 0;
        
        // input is at the front of the document
        OutlineNode beginNode = (OutlineNode) tree.get(0);
        if (beginNode.getIFile().equals(file)
                && inputNode.getBeginLine() < beginNode.getBeginLine()) {
            return 0;
        }
        
        // search the list for the correct position.
        // take care only of the nodes, which are from the file the input belongs to.
        OutlineNode pre = null;
        OutlineNode after = null;
        for (int k = 1; k < tree.size(); k++) {
            OutlineNode preTemp = (OutlineNode) tree.get(k - 1);
            OutlineNode afterTemp = (OutlineNode) tree.get(k);
            if (file.equals(preTemp.getIFile()))
                pre = preTemp;
            if (file.equals(afterTemp.getIFile()))
                after = afterTemp;
            if (pre != null && after != null) {
                if (inputNode.getBeginLine() > pre.getBeginLine()
                        && inputNode.getBeginLine() < after.getBeginLine()) {
                    return k;
                }
            }
        }
        // when no position is found, the input must be at the end.
        return tree.size();
    }
    
    /**
     * Adds the file references to the tree.
     * @param tree the tree to add the file references.
     * @param file the file reference.
     */
    private void addFileNames(ArrayList tree, IFile file) {
        if (tree == null)
            return;
        for (int i = 0; i < tree.size(); i++) {
            OutlineNode on = (OutlineNode) tree.get(i);
            on.setIFile(file);
            addFileNames(on.getChildren(), file);
        }
    }
    
    /**
     * Prints out the tree to the given depth recursively. 
     * Only for debugging purposes.
     * @param tree the tree.
     * @param depth the depth.
     * @param identation the identitaion to be concatenated to the beginning.
     */
    private void showTree(ArrayList tree, int depth, String identation) {
        if (depth == 0 || tree == null)
            return;
        for (int j = 0; j < tree.size(); j++) {
            OutlineNode outnode = (OutlineNode) tree.get(j);
            if (outnode.getParent() != null)
                System.out.println(identation + outnode.getName() + "   :   "
                        + "  :  " + outnode.getType() + "  :  "
                        + outnode.getIFile().toString() + "  :  "
                        + outnode.getParent().getName());
            else
                System.out.println(identation + outnode.getName() + "   :   "
                        + "  :  " + outnode.getType() + "  :  "
                        + outnode.getIFile().toString() + "  :  No parent");
            showTree(outnode.getChildren(), depth--, identation.concat("    "));
        }
    }
    
    /**
     * @return the outlineTree.
     */
    public ArrayList getOutlineTree() {
        return this.outlineTree;
    }
    
    /**
     * 
     * @param changedFile the file reference to the file which changed.
     */
    public void setChangedFile(IFile changedFile) {
        this.changedFile = changedFile;
    }
    
    /**
     * 
     * @param changedInput the changed input.
     */
    public void setChangedInput(String changedInput) {
        this.changedInput = changedInput;
    }
}
