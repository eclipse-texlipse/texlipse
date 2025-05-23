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

package org.eclipse.texlipse.viewer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.texlipse.builder.BuilderRegistry;


/**
 * A helper class to read the viewer error stream if an error occurs.
 * 
 * @author Kimmo Karlsson
 */
public class ViewerErrorScanner implements Runnable {

    // the process to monitor
    private Process process;

    public ViewerErrorScanner(Process process) {
        this.process = process;
    }
    
    /**
     * Wait for the program to exit and read the status.
     */
    public void run() {
        
        BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        
        ArrayList buffer = new ArrayList();
        String tmp = null;
        try {
            
            // read the error output stream lines before the program exits
            // the stream is not available anymore after the program has exit
            while ((tmp = in.readLine()) != null) {
                buffer.add(tmp);
            }
            in.close();
            
        } catch (IOException e) {
        }
        
        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
        }

        // if there was an error, the viewer exited with non-zero status
        if (exitCode != 0) {
            // print the error messages
            for (int i = 0; i < buffer.size(); i++) {
                tmp = (String) buffer.get(i);
                BuilderRegistry.printToConsole("viewer> " + tmp);
            }
        }
    }
}
