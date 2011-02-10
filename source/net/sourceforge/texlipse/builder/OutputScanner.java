/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.InputQueryDialog;

import org.eclipse.swt.widgets.Display;


/**
 * Scans the input stream for the given trigger strings and produces a query dialog if sees one.
 * 
 * @author Kimmo Karlsson
 */
public class OutputScanner {

    // input stream to scan
    private BufferedInputStream in;
    
    // output stream to write user's responses
    private OutputStream out;
    
    // the trigger strings to scan for
    private String[] triggerString;
    
    // true, if the user pressed ok in the dialog
    private boolean okPressed;
    
    // the text that the user wrote to the dialog
    private String query;
    
    // the scanned input
    private StringBuilder sb;
    
    // the length of the occured trigger string
    protected int currentTriggerStringLength;

    // output messages to console
    private String consoleOutput;

    /**
     * Create new OutputProducer.
     * 
     * @param in
     * @param out
     * @param trig
     * @param console
     */
    public OutputScanner(InputStream in, OutputStream out, String[] trig, String console) {
        this.in = new BufferedInputStream(in);
        this.out = out;
        this.triggerString = trig;
        this.okPressed = false;
        this.query = null;
        this.sb = new StringBuilder();
        this.consoleOutput = console;
    }

    /**
     * @return the contents of the buffer
     */
    public String getText() {
        return sb.toString();
    }

    /**
     * Read output from the stream and save it into a buffer.
     * If the trigger string occurs in the stream, a question dialog will be popped up.
     * 
     * Reading is done byte-by-byte because we don't know when the stream is
     * going to pause. Reading by buffers would mean that we could end up waiting
     * input from the program when the program is waiting input from us, i.e. a deadlock.
     * 
     * @return true if the output was read successfully into the buffer
     */
    public boolean scanOutput() {
        try {
            // this was the index we had parsed the output to
            // when the user pressed ok on our dialog
            int startOfLine = 0;
            int okIndex = 0;
            int maxLength = 0;
            if (triggerString != null) {
                //determine max length of a triggerString
                for (int i = 0; i < triggerString.length; i++) {
                    if (maxLength < triggerString[i].length()) 
                        maxLength = triggerString[i].length();
                }
            }
            int avail = 0;
            while (true) {
                
                int nextByte = in.read();
                if (nextByte == -1) break;
                sb.append((char)nextByte);
                
                avail--;                
                if (avail <= 0) avail = in.available();
                
                //TriggerStrings can only occur if the program is waiting for input => in.available() == 0
                if (triggerString != null && avail == 0) {
                    for (int i = 0; i < triggerString.length; i++) {
                        if (sb.length() > maxLength)
                            okIndex = sb.length() - maxLength;
                        int foundIndex = sb.indexOf(triggerString[i], okIndex);
                        if (foundIndex >= 0) {
                            currentTriggerStringLength = triggerString[i].length();
                            
                            boolean retry = askUserInput();
                            if (!retry) {
                                return false;
                            } else {
                                okIndex = foundIndex+1;
                                break;
                            }
                        }
                    }
                }
                
                if (consoleOutput != null && (char)nextByte == '\n') {
                    
                    int lf = 1;
                    if (sb.charAt(sb.length()-2) == '\r') { // fix for windows linefeeds
                        lf++;
                    }
                    // don't print the whole buffer as the printToConsole() outputs a linefeed
                    BuilderRegistry.printToConsole(consoleOutput + "> " + sb.substring(startOfLine, sb.length()-lf));
                    startOfLine = sb.length();
                }
                
            }
        } catch (IOException e) {
        }
        return true;
    }
    
    /**
     * Create the build error input query dialog.
     * @param message
     * @return
     */
    private static InputQueryDialog createQueryDialog(String message) {
        return InputQueryDialog.createQuery("Question from an external program",
            "External program has a question:\n\n" + message,
            "Enter text", "Cancel build");
    }

    /**
     * Display a question dialog with two buttons.
     * 
     * @return true, if the user pressed the "enter text" -button
     */
    private boolean askUserInput() {
        
        Display display = TexlipsePlugin.getDefault().getWorkbench().getDisplay();
        
        display.syncExec(new Runnable() {
            public void run() {
                int i = sb.lastIndexOf("!");
                if (i < 0) {
                    i = currentTriggerStringLength;
                } else {
                    i+=2;
                }
                InputQueryDialog dlg = createQueryDialog(sb.substring(i));
                okPressed = (dlg.open() == 0);
                query = dlg.getInput();
            }});
        
        if (okPressed) {
            try {
                
                if (query == null) {
                    query = "";
                }
                
                query += System.getProperty("line.separator");
                out.write(query.getBytes());
                out.flush();
                
            } catch (IOException e) {
                return false;
            }
            
            return true;
        }
        return false;
    }
}
