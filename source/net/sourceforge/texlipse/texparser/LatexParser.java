/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.texparser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.sourceforge.texlipse.model.CommandEntry;
import net.sourceforge.texlipse.model.DocumentReference;
import net.sourceforge.texlipse.model.OutlineNode;
import net.sourceforge.texlipse.model.ParseErrorMessage;
import net.sourceforge.texlipse.model.ReferenceContainer;
import net.sourceforge.texlipse.model.ReferenceEntry;
import net.sourceforge.texlipse.texparser.lexer.Lexer;
import net.sourceforge.texlipse.texparser.lexer.LexerException;
import net.sourceforge.texlipse.texparser.node.EOF;
import net.sourceforge.texlipse.texparser.node.TArgument;
import net.sourceforge.texlipse.texparser.node.TCbegin;
import net.sourceforge.texlipse.texparser.node.TCbib;
import net.sourceforge.texlipse.texparser.node.TCbibstyle;
import net.sourceforge.texlipse.texparser.node.TCchapter;
import net.sourceforge.texlipse.texparser.node.TCcite;
import net.sourceforge.texlipse.texparser.node.TCend;
import net.sourceforge.texlipse.texparser.node.TCinput;
import net.sourceforge.texlipse.texparser.node.TClabel;
import net.sourceforge.texlipse.texparser.node.TCnew;
import net.sourceforge.texlipse.texparser.node.TCommentline;
import net.sourceforge.texlipse.texparser.node.TCparagraph;
import net.sourceforge.texlipse.texparser.node.TCpart;
import net.sourceforge.texlipse.texparser.node.TCpindex;
import net.sourceforge.texlipse.texparser.node.TCref;
import net.sourceforge.texlipse.texparser.node.TCsection;
import net.sourceforge.texlipse.texparser.node.TCssection;
import net.sourceforge.texlipse.texparser.node.TCsssection;
import net.sourceforge.texlipse.texparser.node.TCword;
import net.sourceforge.texlipse.texparser.node.TOptargument;
import net.sourceforge.texlipse.texparser.node.TStar;
import net.sourceforge.texlipse.texparser.node.TWhitespace;
import net.sourceforge.texlipse.texparser.node.Token;

import org.eclipse.core.resources.IMarker;


/**
 * Simple parser for LaTeX: does very basic structure checking and
 * extracts useful data.
 * 
 * @author Oskar Ojala
 */
public class LatexParser {

    /**
     * Defines a new stack implementation, which is unsynchronized and
     * tuned for the needs of the parser, making it much faster than 
     * java.util.Stack
     * 
     * @author Oskar Ojala
     */
    private final class Stack2 {
        
        private static final int initialSize = 10;
        private static final int growthFactor = 2;
        private int capacity;
        private int size;
        private Object[] stack;
        
        /**
         * Creates a new stack.
         */
        public Stack2() {
            stack = new Object[initialSize];
            size = 0;
            capacity = initialSize;
        }
        
        /**
         * @return True if the stack is empty, false if it contains items
         */
        public boolean empty() {
            return (size == 0);
        }
        
        /**
         * @return The item at the top of the stack
         */
        public Object peek() {
            return stack[size];
        }
        
        /**
         * Removes the item at the stop of the stack.
         * 
         * @return The item at the top of the stack
         */
        public Object pop() {
            Object top = stack[size];
            stack[size] = null;
            size--;
            return top;
        }
        
        /**
         * Pushes an item to the top of the stack.
         * 
         * @param item The item to push on the stack
         */
        public void push(Object item) {
            size++;
            if (size >= capacity) {
                capacity *= growthFactor;
                Object[] newStack = new Object[capacity];
                System.arraycopy(stack, 0, newStack, 0, stack.length);
                stack = newStack;
            }
            stack[size] = item;
        }
        
        /**
         * Clears the stack; removes all entries.
         */
        public void clear() {
            for (; size >= 0; size--) {
                stack[size] = null;
            }
            size = 0;
        }
    }

    private ArrayList labels; //type: ReferenceEntry
    private ArrayList cites; //type: DocumentReference
    private ArrayList refs; //type: DocumentReference
    private ArrayList commands; //type: CommandEntry
    
    private String[] bibs;
    private String bibstyle;
    
    private ArrayList inputs; //type: String
    
    private ArrayList outlineTree; //type: OutlineNode
    
    private ArrayList errors; //type: ParseErrorMessage
    
    private OutlineNode documentEnv;
    
    private boolean index;
    private boolean fatalErrors;
    
    /**
     * Creates new LaTeX parser.
     */
    public LatexParser() {
    }
    
    /**
     * Initializes the internal datastructures that are exported after parsing.
     */
    private void initializeDatastructs() {
        this.labels = new ArrayList();
        this.cites = new ArrayList();
        this.refs = new ArrayList();
        this.commands = new ArrayList();
        this.inputs = new ArrayList();
        
        this.outlineTree = new ArrayList();
        this.errors = new ArrayList();
        
        this.index = false;
        this.fatalErrors = false;
    }
        
    /**
     * Parses a LaTeX document. Uses the given lexer's <code>next()</code>
     * method to receive tokens that are processed.
     * 
     * @param lex The lexer to use for extracting the document tokens
     * @param definedLabels Labels that are defined, used to check for references to
     * nonexistant labels
     * @param definedBibs Defined bibliography entries, used to check for references to
     * nonexistant bibliography entries
     * @throws LexerException If the given lexer cannot tokenize the document
     * @throws IOException If the document is unreadable
     */
    public void parse(Lexer lex, ReferenceContainer definedLabels, ReferenceContainer definedBibs) throws LexerException, IOException {
        parse(lex, definedLabels, definedBibs, null);
    }

    /**
     * Parses a LaTeX document. Uses the given lexer's <code>next()</code>
     * method to receive tokens that are processed.
     * 
     * @param lexer The lexer to use for extracting the document tokens
     * @param definedLabels Labels that are defined, used to check for references to
     * nonexistant labels
     * @param definedBibs Defined bibliography entries, used to check for references to
     * nonexistant bibliography entries
     * @param preamble An <code>OutlineNode</code> containing the preamble, null if there is no preamble
     * @throws LexerException If the given lexer cannot tokenize the document
     * @throws IOException If the document is unreadable
     */

    public void parse(Lexer lexer, ReferenceContainer definedLabels, ReferenceContainer definedBibs, OutlineNode preamble) throws LexerException, IOException {
        initializeDatastructs();
        Stack2 blocks = new Stack2();
        
        boolean expectArg = false;
        boolean expectArg2 = false;
        Token prevToken = null;
        
        CommandEntry currentCommand = null;
        int argCount = 0;
        Integer nodeType;
        
        HashMap sectioning = new HashMap();
        
        if (preamble != null) {
            outlineTree.add(preamble);
            blocks.push(preamble);
        }
        
        // newcommand would need to check for the valid format
        // duplicate labels?
        // change order of ifs to optimize performance?
        
        
        int accumulatedLength = 0;
        Token t = lexer.next();
        for (; !(t instanceof EOF); t = lexer.next()) { 
            if (expectArg) {
                if (t instanceof TArgument) {
                    if (prevToken instanceof TClabel) {
                        this.labels.add(new ReferenceEntry(t.getText()));
                        
                    } else if (prevToken instanceof TCref) {
                        // if it's not certain that it exists, add it (this could lead to erros if the corresponding
                        // label was also removed)

                        if (!definedLabels.binTest(t.getText())) {
                            this.refs.add(new DocumentReference(t.getText(),
                                    t.getLine(),
                                    t.getPos(),
                                    t.getText().length()));
                            //  + accumulatedLength + t.getText().length())
                        }
                    } else if (prevToken instanceof TCcite) {
                        if (!"*".equals(t.getText())) {
                            String[] cs = t.getText().replaceAll("\\s","").split(",");
                            for (int i=0; i < cs.length; i++) {
                                // this is certain to be an error, since the BibTeX -keys are always up to date
                                if (!definedBibs.binTest(cs[i])) {
                                    this.cites.add(new DocumentReference(cs[i],
                                            t.getLine(), t.getPos(), t.getText().length()));
                                }
                            }
                        }
                        
                    } else if (prevToken instanceof TCbegin) { // \begin{...}
                        OutlineNode on = new OutlineNode(t.getText(),
                                OutlineNode.TYPE_ENVIRONMENT,
                                t.getLine(), null);
                        
                        if (preamble != null && "document".equals(t.getText())) {
                            preamble.setEndLine(t.getLine());
                            blocks.clear();
                            documentEnv = on;
                        } else {
                            if (!blocks.empty()) {
                                OutlineNode prev = (OutlineNode) blocks.peek();
                                prev.addChild(on);
                                on.setParent(prev);
                            } else {
                                outlineTree.add(on);
                            }
                            blocks.push(on);
                        }
                        
                    } else if (prevToken instanceof TCend) { // \end{...}
                        int endLine = t.getLine();
                        OutlineNode prev = null;

                        // check if the document ends
                        if (preamble != null && "document".equals(t.getText())) {
                            documentEnv.setEndLine(endLine + 1);
                            
                            // terminate open blocks here; check for errors
                            while (!blocks.empty()) {
                                prev = (OutlineNode) blocks.pop();
                                prev.setEndLine(endLine);
                                if (prev.getType() == OutlineNode.TYPE_ENVIRONMENT) {
                                    errors.add(new ParseErrorMessage(prevToken.getLine(),
                                            prevToken.getPos(),
                                            prevToken.getText().length() + accumulatedLength + t.getText().length(),
                                            "\\end{" + prev.getName() + "} expected, but \\end{document} found; at least one unbalanced begin-end",
                                            IMarker.SEVERITY_ERROR));
                                    fatalErrors = true;
                                }
                            }
                        } else {
                            // the "normal" case
                            boolean traversing = true;
                            if (!blocks.empty()) {
                                while (traversing && !blocks.empty()) {
                                    prev = (OutlineNode) blocks.peek();
                                    switch (prev.getType()) {
                                    case OutlineNode.TYPE_ENVIRONMENT:
                                        prev.setEndLine(endLine + 1);
                                    blocks.pop();
                                    traversing = false;
                                    break;
                                    default:
                                        prev.setEndLine(endLine);
                                    blocks.pop();
                                    break;
                                    }
                                }
                            }
                            if (blocks.empty() && traversing) {
                                fatalErrors = true;
                                errors.add(new ParseErrorMessage(prevToken.getLine(),
                                        prevToken.getPos(),
                                        prevToken.getText().length() + accumulatedLength + t.getText().length(),
                                        "\\end{" + t.getText() + "} found with no preceding \\begin",
                                        IMarker.SEVERITY_ERROR));
                            } else if (!prev.getName().equals(t.getText())) {
                                fatalErrors = true;
                                errors.add(new ParseErrorMessage(prevToken.getLine(),
                                        prevToken.getPos(),
                                        prevToken.getText().length() + accumulatedLength + t.getText().length(),
                                        "\\end{" + prev.getName() + "} expected, but \\end{" + t.getText() + "} found; unbalanced begin-end",
                                        IMarker.SEVERITY_ERROR));                            
                            }
                        }
                    } else if (prevToken instanceof TCpart) {
                        int startLine = prevToken.getLine();
                        OutlineNode on = new OutlineNode(t.getText(),
                                OutlineNode.TYPE_PART,
                                startLine,
                                null);
                        
                        if (!blocks.empty()) {
                            boolean traversing = true;
                            while (traversing && !blocks.empty()) {
                                OutlineNode prev = (OutlineNode) blocks.peek();
                                switch (prev.getType()) {
                                case OutlineNode.TYPE_ENVIRONMENT:
                                    prev.addChild(on);
                                on.setParent(prev);
                                traversing = false;
                                break;
                                default:
                                    prev.setEndLine(startLine);
                                blocks.pop();
                                break;
                                }
                            }
                        }
                        if (blocks.empty())
                            outlineTree.add(on);
                        blocks.push(on);
                        
                    } else if (prevToken instanceof TCchapter) {
                        int startLine = prevToken.getLine();
                        OutlineNode on = new OutlineNode(t.getText(),
                                OutlineNode.TYPE_CHAPTER,
                                startLine,
                                null);
                        
                        if (!blocks.empty()) {
                            boolean traversing = true;
                            while (traversing && !blocks.empty()) {
                                OutlineNode prev = (OutlineNode) blocks.peek();
                                switch (prev.getType()) {
                                case OutlineNode.TYPE_PART:
                                case OutlineNode.TYPE_ENVIRONMENT:
                                    prev.addChild(on);
                                on.setParent(prev);
                                traversing = false;
                                break;
                                default:
                                    prev.setEndLine(startLine);
                                blocks.pop();
                                break;
                                }
                            }
                        }
                        // add directly to tree if no parent was found
                        if (blocks.empty())
                            outlineTree.add(on);
                        
                        blocks.push(on);
                    } else if (prevToken instanceof TCsection) {
                        int startLine = prevToken.getLine();
                        OutlineNode on = new OutlineNode(t.getText(),
                                OutlineNode.TYPE_SECTION,
                                startLine,
                                null);
                        
                        if (!blocks.empty()) {
                            boolean traversing = true;
                            while (traversing && !blocks.empty()) {
                                OutlineNode prev = (OutlineNode) blocks.peek();
                                switch (prev.getType()) {
                                case OutlineNode.TYPE_PART:
                                case OutlineNode.TYPE_CHAPTER:
                                case OutlineNode.TYPE_ENVIRONMENT:
                                    prev.addChild(on);
                                on.setParent(prev);
                                traversing = false;
                                break;
                                default:
                                    prev.setEndLine(startLine);
                                blocks.pop();
                                break;
                                }
                            }
                        }
                        // add directly to tree if no parent was found
                        if (blocks.empty())
                            outlineTree.add(on);
                        
                        blocks.push(on);
                    } else if (prevToken instanceof TCssection) {
                        int startLine = prevToken.getLine();
                        OutlineNode on = new OutlineNode(t.getText(),
                                OutlineNode.TYPE_SUBSECTION,
                                startLine,
                                null);
                        
                        boolean foundSection = false;
                        if (!blocks.empty()) {
                            boolean traversing = true;
                            while (traversing && !blocks.empty()) {
                                OutlineNode prev = (OutlineNode) blocks.peek();
                                switch (prev.getType()) {
                                case OutlineNode.TYPE_ENVIRONMENT:
                                case OutlineNode.TYPE_SECTION:
                                    foundSection = true;                                
                                case OutlineNode.TYPE_PART:
                                case OutlineNode.TYPE_CHAPTER:
                                    prev.addChild(on);
                                on.setParent(prev);
                                traversing = false;
                                break;
                                default:
                                    prev.setEndLine(startLine);
                                blocks.pop();
                                break;
                                }
                            }
                        }
                        // add directly to tree if no parent was found
                        if (blocks.empty())
                            outlineTree.add(on);
                        
                        if (!foundSection) {
                            errors.add(new ParseErrorMessage(prevToken.getLine(),
                                    prevToken.getPos(),
                                    prevToken.getText().length() + accumulatedLength + t.getText().length(),
                                    "Subsection " + prevToken.getText() + " has no preceding section",
                                    IMarker.SEVERITY_WARNING));
                        }
                        blocks.push(on);
                    } else if (prevToken instanceof TCsssection) {
                        int startLine = prevToken.getLine();
                        OutlineNode on = new OutlineNode(t.getText(),
                                OutlineNode.TYPE_SUBSUBSECTION,
                                prevToken.getLine(),
                                null);
                        
                        boolean foundSsection = false;
                        if (!blocks.empty()) {
                            boolean traversing = true;
                            while (traversing && !blocks.empty()) {
                                OutlineNode prev = (OutlineNode) blocks.peek();
                                switch (prev.getType()) {
                                case OutlineNode.TYPE_ENVIRONMENT:
                                case OutlineNode.TYPE_SUBSECTION:
                                    foundSsection = true;                                
                                case OutlineNode.TYPE_PART:
                                case OutlineNode.TYPE_CHAPTER:
                                case OutlineNode.TYPE_SECTION:
                                    prev.addChild(on);
                                on.setParent(prev);
                                traversing = false;
                                break;
                                default:
                                    prev.setEndLine(startLine);
                                blocks.pop();
                                break;
                                }
                            }
                        }
                        // add directly to tree if no parent was found
                        if (blocks.empty())
                            outlineTree.add(on);
                        
                        if (!foundSsection) {
                            errors.add(new ParseErrorMessage(prevToken.getLine(),
                                    prevToken.getPos(),
                                    prevToken.getText().length() + accumulatedLength + t.getText().length(),
                                    "Subsubsection " + prevToken.getText() + " has no preceding subsection",
                                    IMarker.SEVERITY_WARNING));
                        }
                        
                        blocks.push(on);
                        
                    } else if (prevToken instanceof TCparagraph) {
                        int startLine = prevToken.getLine();
                        OutlineNode on = new OutlineNode(t.getText(),
                                OutlineNode.TYPE_PARAGRAPH,
                                prevToken.getLine(),
                                null);
                        
                        boolean foundSssection = false;
                        if (!blocks.empty()) {
                            boolean traversing = true;
                            while (traversing && !blocks.empty()) {
                                OutlineNode prev = (OutlineNode) blocks.peek();
                                switch (prev.getType()) {
                                case OutlineNode.TYPE_ENVIRONMENT:
                                case OutlineNode.TYPE_SUBSUBSECTION:
                                    foundSssection = true;
                                case OutlineNode.TYPE_PART:
                                case OutlineNode.TYPE_CHAPTER:
                                case OutlineNode.TYPE_SECTION:
                                case OutlineNode.TYPE_SUBSECTION:
                                    prev.addChild(on);
                                on.setParent(prev);
                                traversing = false;
                                break;
                                default:
                                    prev.setEndLine(startLine);
                                blocks.pop();
                                break;
                                }
                            }
                        }
                        // add directly to tree if no parent was found
                        if (blocks.empty())
                            outlineTree.add(on);
                        
                        if (!foundSssection) {
                            errors.add(new ParseErrorMessage(prevToken.getLine(),
                                    prevToken.getPos(),
                                    prevToken.getText().length() + accumulatedLength + t.getText().length(),
                                    "Paragraph " + prevToken.getText() + " has no preceding subsubsection",
                                    IMarker.SEVERITY_WARNING));
                        }
                        blocks.push(on);

                    } else if (prevToken instanceof TCbib) {
                        bibs = t.getText().split(",");
                        int startLine = prevToken.getLine();
                        while (!blocks.empty()) {
                            OutlineNode prev = (OutlineNode) blocks.pop();
                            if (prev.getType() == OutlineNode.TYPE_ENVIRONMENT) { // this is an error...
                                blocks.push(prev);
                                break;
                            }
                            prev.setEndLine(startLine);
                        }
                    } else if (prevToken instanceof TCbibstyle) {
                        this.bibstyle = t.getText();
                        int startLine = prevToken.getLine();
                        while (!blocks.empty()) {
                            OutlineNode prev = (OutlineNode) blocks.pop();
                            if (prev.getType() == OutlineNode.TYPE_ENVIRONMENT) { // this is an error...
                                blocks.push(prev);
                                break;
                            }
                            prev.setEndLine(startLine);
                        }
                    } else if (prevToken instanceof TCinput) {
                        inputs.add(t.getText());
                    
                    } else if (prevToken instanceof TCnew) {
                        currentCommand = new CommandEntry(t.getText().substring(1)); 
                        expectArg2 = true;
                    }
                    
                    // reset state to normal scanning
                    accumulatedLength = 0;
                    prevToken = null;
                    expectArg = false;
                    
                } else if ((t instanceof TCword) && (prevToken instanceof TCnew)) {
                    // this handles the \newcommand\comx{...} -format
                    currentCommand = new CommandEntry(t.getText().substring(1)); 
                    expectArg2 = true;
                    accumulatedLength = 0;
                    prevToken = null;
                    expectArg = false;

                } else if (!(t instanceof TOptargument) && !(t instanceof TWhitespace)
                        && !(t instanceof TStar) && !(t instanceof TCommentline)) {
                    
                    // if we didn't get the mandatory argument we were expecting...
                    //fatalErrors = true;
                    errors.add(new ParseErrorMessage(prevToken.getLine(),
                            prevToken.getPos(),
                            prevToken.getText().length() + accumulatedLength + t.getText().length(),
                            "No argument following " + prevToken.getText(),
                            IMarker.SEVERITY_WARNING));
                    
                    accumulatedLength = 0;
                    prevToken = null;
                    expectArg = false;
                } else {
                    accumulatedLength += t.getText().length();
                }
            } else if (expectArg2) {
                // we are capturing the second argument of a command with two arguments
                // the only one of those that interests us is newcommand
                if (t instanceof TArgument) {
                    currentCommand.info = t.getText();
                    commands.add(currentCommand);
                    if (currentCommand.info.indexOf("\\part") != -1)
                        sectioning.put("\\" + currentCommand.key, new Integer(OutlineNode.TYPE_PART));
                    else if (currentCommand.info.indexOf("\\chapter") != -1)
                        sectioning.put("\\" + currentCommand.key, new Integer(OutlineNode.TYPE_CHAPTER));
                    else if (currentCommand.info.indexOf("\\section") != -1)
                        sectioning.put("\\" + currentCommand.key, new Integer(OutlineNode.TYPE_SECTION));
                    else if (currentCommand.info.indexOf("\\subsection") != -1)
                        sectioning.put("\\" + currentCommand.key, new Integer(OutlineNode.TYPE_SUBSECTION));
                    else if (currentCommand.info.indexOf("\\subsubsection") != -1)
                        sectioning.put("\\" + currentCommand.key, new Integer(OutlineNode.TYPE_SUBSUBSECTION));
                    else if (currentCommand.info.indexOf("\\paragraph") != -1)
                        sectioning.put("\\" + currentCommand.key, new Integer(OutlineNode.TYPE_PARAGRAPH));

                    argCount = 0;
                    expectArg2 = false;
                } else if (t instanceof TOptargument) {
                    if (argCount == 0) {
                        try {
                            currentCommand.arguments = Integer.parseInt(t.getText());
                        } catch (NumberFormatException nfe) {
                            errors.add(new ParseErrorMessage(prevToken.getLine(),
                                    t.getPos(),
                                    t.getText().length(),
                                    "The first optional argument of newcommand must only contain the number of arguments",
                                    IMarker.SEVERITY_ERROR));
                            expectArg2 = false;
                        }
                    }
                    argCount++;
                } else if (!(t instanceof TWhitespace) && !(t instanceof TCommentline)) {
                    // if we didn't get the mandatory argument we were expecting...
                    errors.add(new ParseErrorMessage(t.getLine(), t.getPos(), t.getText().length(),
                            "No 2nd argument following newcommand",
                            IMarker.SEVERITY_WARNING));
                    argCount = 0;
                    expectArg2 = false;
                }
            } else {
                if (t instanceof TClabel || t instanceof TCref || t instanceof TCcite
                        || t instanceof TCbib || t instanceof TCbibstyle 
                        || t instanceof TCbegin || t instanceof TCend || t instanceof TCinput
                        || t instanceof TCpart || t instanceof TCchapter 
                        || t instanceof TCsection || t instanceof TCssection 
                        || t instanceof TCsssection || t instanceof TCparagraph
                        || t instanceof TCnew) {
                    prevToken = t;
                    expectArg = true;
                } else if (t instanceof TCword) {
                    if ((nodeType = (Integer) sectioning.get(t.getText())) != null) {
                        switch (nodeType.intValue()) {
                        case OutlineNode.TYPE_PART:
                            prevToken = new TCpart(t.getLine(), t.getPos());
                            break;
                        case OutlineNode.TYPE_CHAPTER:
                            prevToken = new TCchapter(t.getLine(), t.getPos());
                            break;
                        case OutlineNode.TYPE_SECTION:
                            prevToken = new TCsection(t.getLine(), t.getPos());
                            break;
                        case OutlineNode.TYPE_SUBSECTION:
                            prevToken = new TCssection(t.getLine(), t.getPos());
                            break;
                        case OutlineNode.TYPE_SUBSUBSECTION:
                            prevToken = new TCsssection(t.getLine(), t.getPos());
                            break;
                        case OutlineNode.TYPE_PARAGRAPH:
                            prevToken = new TCparagraph(t.getLine(), t.getPos());
                            break;
                        default:
                            break;
                        }
                        expectArg = true;
                    }
                } else if (t instanceof TCpindex)
                    this.index = true;
            }
        }
                
        int endLine = t.getLine() + 1; //endline is exclusive
        while (!blocks.empty()) {
            OutlineNode prev = (OutlineNode) blocks.pop();
            prev.setEndLine(endLine);
            if (prev.getType() == OutlineNode.TYPE_ENVIRONMENT) {
                fatalErrors = true;
                errors.add(new ParseErrorMessage(prev.getBeginLine(),
                        0,
                        prev.getName().length(),
                        "\\begin{" + prev.getName() + "} does not have matching end; at least one unbalanced begin-end",
                        IMarker.SEVERITY_ERROR));
            }
        }
    }
    
    /**
     * @return The labels defined in this document
     */
    public ArrayList getLabels() {
        return this.labels;
    }
    
    /**
     * @return The BibTeX citations which weren't defined
     */
    public ArrayList getCites() {
        return this.cites;
    }
    
    /**
     * @return The refencing commands for which no label was found
     */
    public ArrayList getRefs() {
        return this.refs;
    }
    
    /**
     * @return The bibliography files to use.
     */
    public String[] getBibs() {
        return this.bibs;
    }
    
    /**
     * @return The bibliography style.
     */
    public String getBibstyle() {
        return bibstyle;
    }
    
    /**
     * @return The input commands in this document
     */
    public ArrayList getInputs() {
        return this.inputs;
    }
    
    /**
     * @return The outline tree of the document (OutlineNode objects).
     */
    public ArrayList getOutlineTree() {
        return this.outlineTree;
    }
    
    /**
     * @return The list of errors (ParseErrorMessage objects) in the document
     */
    public ArrayList getErrors() {
        return this.errors;
    }
    
    /**
     * @return Returns whether makeindex is to be used or not
     */
    public boolean isIndex() {
        return index;
    }
    /**
     * @return Returns the documentEnv.
     */
    public OutlineNode getDocumentEnv() {
        return documentEnv;
    }
    
    /**
     * @return Returns whether there are fatal errors in the document
     */
    public boolean isFatalErrors() {
        return fatalErrors;
    }
    
    /**
     * @return Returns the commands.
     */
    public ArrayList getCommands() {
        return commands;
    }
}
