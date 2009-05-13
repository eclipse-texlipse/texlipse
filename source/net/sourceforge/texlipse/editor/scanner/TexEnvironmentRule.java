/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor.scanner;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * This class implements a rule which detects latex environments, i.e
 * \begin{environment} ... \end{environment} LaTeX permits whitespaces or line
 * feeds between \begin an {environment} and also this class.
 * 
 * @author Boris von Loesch
 */
public class TexEnvironmentRule implements IPredicateRule {

    /** The token to be returned on success */
    protected IToken fToken;
    /** The pattern's start sequence */
    protected char[] fStartSequence;
    /** The pattern's end sequence */
    protected char[] fEndSequence;
    /** The name of the environment */
    protected char[][] fEnvName;
    protected boolean fStar;
    protected boolean fLastStar;
    protected int fLastEnv;

    public TexEnvironmentRule(String envName, IToken token) {
        this (envName, false, token);
    }
    
    public TexEnvironmentRule(String[] envNames, boolean star, IToken token) {
        fStartSequence = ("\\begin").toCharArray();
        fEndSequence = ("\\end").toCharArray();
        fToken = token;
        fEnvName = new char[envNames.length][];
        for (int i = 0; i < envNames.length; i++) {
            fEnvName[i] = envNames[i].toCharArray();            
        }
        fStar = star;
    }
    
    /**
     * 
     * @param envName Name of the environment
     * @param star if true, this environment also detects the stared version of the environment
     * @param token
     */
    public TexEnvironmentRule(String envName, boolean star, IToken token) {
        fStartSequence = ("\\begin").toCharArray();
        fEndSequence = ("\\end").toCharArray();
        fToken = token;
        fEnvName = new char[1][];
        fEnvName[0] = envName.toCharArray();
        fStar = star;
    }

    /**
     * Evaluates this rules without considering any column constraints. Resumes
     * detection, i.e. look sonly for the end sequence required by this rule if
     * the <code>resume</code> flag is set.
     * 
     * @param scanner the character scanner to be used
     * @param resume <code>true</code> if detection should be resumed,
     *            <code>false</code> otherwise
     * @return the token resulting from this evaluation
     * @since 2.0
     */
    protected IToken doEvaluate(ICharacterScanner scanner, boolean resume) {
        fLastStar = true;
        fLastEnv = -1;
        if (resume) {
            //HACK, just read all available characters from the scanner. 
            //This works very good in Eclipse 3.2 - 3.3 but maybe not in other versions. 
            //Sadly there is no easy workaround that works as good as this
            int c;
            while ((c = scanner.read()) != ICharacterScanner.EOF) ;
/*            if (endSequenceDetected(scanner))
                return fToken;*/
        } else {
            int c = scanner.read();
            if (c == fStartSequence[0]) {
                if (sequenceDetected(scanner, fStartSequence)) {
                    if (endSequenceDetected(scanner))
                        return fToken;
                }
            }
            scanner.unread();
        }

        return Token.UNDEFINED;
    }

    /*
     * @see IRule#evaluate(ICharacterScanner)
     */
    public IToken evaluate(ICharacterScanner scanner) {
        return evaluate(scanner, false);
    }

    /**
     * Returns whether the end sequence was detected. *
     * 
     * @param scanner the character scanner to be used
     * @return <code>true</code> if the end sequence has been detected
     */
    protected boolean endSequenceDetected(ICharacterScanner scanner) {
        int c;
        int readChar = 1;
        while ((c = scanner.read()) != ICharacterScanner.EOF) {
            readChar++;
            if (fEndSequence.length > 0 && c == fEndSequence[0]) {
                // Check if the specified end sequence has been found.
                if (sequenceDetected(scanner, fEndSequence))
                    return true;
            }
        }
        unReadScanner(scanner, readChar);
        return false;
    }

    /**
     * Unreads a certain amount of charakters from the scanner
     * 
     * @param scanner the charakter scanner
     * @param readChar the number of charakters to unread
     * @return false
     */
    private boolean unReadScanner(ICharacterScanner scanner, int readChar) {
        for (int j = 0; j < readChar; j++)
            scanner.unread();
        return false;
    }

    /**
     * Returns whether the next characters to be read by the character scanner
     * are an exact match of sequence + whitespaces + {envName}
     * 
     * @param scanner the character scanner to be used
     * @param sequence either \begin or \end
     * @return <code>true</code> if the given sequence has been detected
     */
    protected boolean sequenceDetected(ICharacterScanner scanner, char[] sequence) {
        int readChar = 0;
        for (int i = 1; i < sequence.length; i++) {
            int c = scanner.read();
            readChar++;
            if (c != sequence[i]) {
                return unReadScanner(scanner, readChar);
            }
        }
        // Whitespaces
        int c;
        do {
            c = scanner.read();
            readChar++;
        } while (isWhiteSpace((char) c));
        // Now: {environment name}
        if (c != '{') {
            return unReadScanner(scanner, readChar);
        }
        if (fLastEnv == -1) {
            //Test if one of the environments fits
            boolean found = false;
            for (int j = 0; j < fEnvName.length; j++) {
                int readChar2 = 0;
                for (int i = 0; i < fEnvName[j].length; i++) {
                    c = scanner.read();
                    readChar2++;
                    if (c != fEnvName[j][i]) {
                        unReadScanner(scanner, readChar2);
                        break;
                    }
                    if (i == fEnvName[j].length - 1) found = true;
                }
                if (found) {
                    fLastEnv = j;
                    readChar += readChar2;
                    break;
                }
            }
            if (!found) {
                return unReadScanner(scanner, readChar);
            }
        } else {
            //Test for environment fLastEnv
            for (int i = 0; i < fEnvName[fLastEnv].length; i++) {
                c = scanner.read();
                readChar++;
                if (c != fEnvName[fLastEnv][i]) {
                    return unReadScanner(scanner, readChar);
                }
            }            
        }
        c = scanner.read();
        readChar++;
        if (fStar && fLastStar && c == '*') {
            //Stared environment detected
            fLastStar = true;
            c = scanner.read();
            readChar++;
        } else {
            fLastStar = false;
        }

        if (c != '}') {
            return unReadScanner(scanner, readChar);
        }

        return true;
    }

    /**
     * Returns true if c is either a whitespace, tab or linefeed
     * 
     * @param c
     * @return
     */
    private boolean isWhiteSpace(char c) {
        if (c == ' ' || c == '\n' || c == '\r' || c == '\t')
            return true;
        return false;
    }

    /*
     * @see IPredicateRule#evaluate(ICharacterScanner, boolean)
     * @since 2.0
     */
    public IToken evaluate(ICharacterScanner scanner, boolean resume) {
        return doEvaluate(scanner, resume);
    }

    /*
     * @see IPredicateRule#getSuccessToken()
     * @since 2.0
     */
    public IToken getSuccessToken() {
        return fToken;
    }
}
