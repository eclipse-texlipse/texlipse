/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.texparser;

import java.io.PushbackReader;
import java.util.HashSet;

import org.eclipse.texlipse.texparser.lexer.Lexer;
import org.eclipse.texlipse.texparser.lexer.LexerException;
import org.eclipse.texlipse.texparser.node.EOF;
import org.eclipse.texlipse.texparser.node.TArgument;
import org.eclipse.texlipse.texparser.node.TBverbatim;
import org.eclipse.texlipse.texparser.node.TCnew;
import org.eclipse.texlipse.texparser.node.TCword;
import org.eclipse.texlipse.texparser.node.TEverbatim;
import org.eclipse.texlipse.texparser.node.TLBrace;
import org.eclipse.texlipse.texparser.node.TOptargument;
import org.eclipse.texlipse.texparser.node.TRBrace;
import org.eclipse.texlipse.texparser.node.TRBracket;
import org.eclipse.texlipse.texparser.node.TVtext;
import org.eclipse.texlipse.texparser.node.TWhitespace;
import org.eclipse.texlipse.texparser.node.Token;


/**
 * Lexer for LaTeX -files. Implements tokenizing curly brace-enclosed
 * areas and verb and verbatim environments.
 * 
 * @author Oskar Ojala
 */
public class LatexLexer extends Lexer {
    
    /**
     * Counter for braces
     */
    private int count;
    
    private Token argStart;
    //private Token verbStart;
    private StringBuffer text;
    
    /**
     * Terminator char for \verb
     */
    private char startChar;
    
    private int vline, vpos;
    
    private HashSet<String> defCommands;
    private boolean commandDef;
    
    /**
     * Creates a new lexer.
     * 
     * @param in The reader to read the character stream from
     */
    public LatexLexer(PushbackReader in) {
        super(in);
        defCommands = new HashSet<String>();
        commandDef = false;
    }
    
    /**
     * We define a filter that recognizes braced strings and verbatims
     */
    protected void filter() throws LexerException {
        
        if (state.equals(State.COMCAPT)) {
//            if (token instanceof TCword) {
//                System.out.println(token.getText().substring(1));
//                System.out.println(defCommands.contains(token.getText().substring(1)));
//            }
            if (token instanceof TCnew) {
                commandDef = true;
            } else if (token instanceof TCword && !commandDef 
                    && !defCommands.contains(token.getText().substring(1))) {
                state = State.NORMAL;
                return;
            }

        // if we're to capture a brace-block
        } else if (state.equals(State.BLOCKCAPT)) {
            
            // if we are just entering this state
            if (argStart == null) {                
                argStart = token;
                text = new StringBuffer("");
                count = 1;
                token = null; // continue to scan the input.
            } else {
                if (token instanceof TLBrace)
                    count++;
                else if (token instanceof TRBrace)
                    count--;
                else if (token instanceof EOF) {
                    throw new LexerException("[" + argStart.getLine() + 
                            "," + (argStart.getPos() - 1) + "] There's a } missing: unexpected end of file");
                }
                if (count != 0) {
                    // accumulate the string and continue to scan the input.
                    if (token instanceof TWhitespace)
                        text.append(" ");
                    else
                        text.append(token.getText());
                    token = null;
                } else {
                    TArgument targ = new TArgument(text.toString(),
                            argStart.getLine(),
                            argStart.getPos());
                    // emit the string
                    token = targ;
                    state = State.COMCAPT;
                    argStart = null;
                    commandDef = false;
                }
            }
            // Capture optional argument
        } else if (state.equals(State.OPTCAPT)) {
            if (argStart == null) {
                argStart = token;
                text = new StringBuffer("");
                count = 0;
                token = null; // continue to scan the input.
            } else {
                if (token instanceof TLBrace)
                    count++;
                else if (token instanceof TRBrace)
                    count--;
                else if (token instanceof EOF) {
                    throw new LexerException("[" + argStart.getLine() + 
                            "," + argStart.getPos() + "] There's a } or a ] missing: unexpected end of file");
                }
                
                if (count != 0 || !(token instanceof TRBracket)) {
                    // accumulate the string and continue to scan the input.
                    if (token instanceof TWhitespace)
                        text.append(" ");
                    else
                        text.append(token.getText());
                    token = null;
                } else {
                    TOptargument tsl = new TOptargument(text.toString(),
                            argStart.getLine(),
                            argStart.getPos());
                    // emit the string
                    token = tsl;
                    state = State.COMCAPT;
                    argStart = null;
                    commandDef = false;
                }
            }
        } else if (state.equals(State.VERBATIM)) {
            // we store some contents to be able to code fold
            if (token instanceof TBverbatim) {
                argStart = token;
                text = new StringBuffer(token.getText());
                vline = token.getLine();
                vpos = token.getPos();
            } else if (token instanceof TVtext || token instanceof TWhitespace) {
                text.append(token.getText());
                token = null;
            } else if (token instanceof EOF) {
                throw new LexerException("[" + vline + "," + vpos 
                        + "] The verbatim environment isn't closed: unexpected end of file");
            }
        } else if (state.equals(State.VERB)) {
            if (token instanceof TVtext) {
                if (argStart == null) {
                    argStart = token;
                    startChar = token.getText().charAt(0);
                } else {
                    if (startChar == token.getText().charAt(0)) {
                        state = State.NORMAL;
                        startChar = '\0';
                        argStart = null;
                    }
                }
                token = null;
            } else if (token instanceof EOF) {
                throw new LexerException("[" + argStart.getLine() + 
                        "," + argStart.getPos() + "] The verb-command isn't closed: unexpected end of file");
            }
        } else if (state.equals(State.NORMAL)) {
            if (token instanceof TEverbatim) {
                String startCommand = argStart.getText().substring(argStart.getText().indexOf("{"));
                String endCommand = token.getText().substring(token.getText().indexOf("{"));
                if (!startCommand.equals(endCommand)) {
                    throw new LexerException("[" + vline + "," + vpos 
                            + "] The verbatim environment isn't closed with the correct command");                    
                }
                text.append(token.getText());
                token = new TVtext(text.toString(), vline, vpos);
                argStart = null;
            }
        }
    }
    
    public void registerCommand(String command) {
//        System.out.println("---------------------");
//        System.out.println(command);
//        System.out.println("---------------------");
        defCommands.add(command);
    }
}
