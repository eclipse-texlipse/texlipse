/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.viewer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;



/**
 * A server that takes in filename-linenumber commands
 * and passes these commands as events to its listener.
 * 
 * The server will listen to the specified socket until it gets
 * a command "QUIT". 
 * 
 * @author Esa Seuranen
 * @author Kimmo Karlsson
 */
public class FileLocationServer implements Runnable {

    // the singleton instance
    private static FileLocationServer instance = new FileLocationServer();

    // the socket where the server is currently listening
    private ServerSocket ssocket;
    
    // the listener, which will be notified every time a file location command occurs
    private FileLocationListener listener;


    /**
     * Create a new Server.
     */
    private FileLocationServer() {
    }
    
    /**
     * @return the shared instance
     */
    public static FileLocationServer getInstance() {
        return instance;
    }

    /**
     * Check to see if the server is already running.
     * @return true, if the server is running
     */
    public boolean isRunning() {
        return ssocket != null;
    }
    
    /**
     * Stop the server.
     */
    public void stop() {
        if (ssocket == null) {
            return;
        }
        
        try {
            Socket s = new Socket("localhost", ssocket.getLocalPort());
            s.getOutputStream().write("QUIT\n".getBytes());
            s.close();
        } catch (UnknownHostException e) {
            //shouldn't happen
        } catch (IOException e) {
            TexlipsePlugin.log("Stopping FileLocation server: ", e);
        }
    }

    /**
     * Set the file location listener.
     * @param listener the new file location listener.
     */
    public void setListener(FileLocationListener listener) {
        this.listener = listener;
    }
    
    /**
     * Run the socket listener.
     *
     */
    public void run() {

    	// Try to open a server socket on port 
    	// Note that we can't choose a port less than 1023 if we are not
    	// privileged users (root)

        ssocket = null;
    	try {
    		ssocket = new ServerSocket(TexlipsePlugin.getDefault().getPreferenceStore().getInt(TexlipseProperties.FILE_LOCATION_PORT));
    	} catch (IOException e) {
    		TexlipsePlugin.log("Starting server", e);
    	}   

    	// Create a socket object from the ServerSocket to listen and accept connections.
    	// Open input and output streams

    	try {
    		while (ssocket != null) {

    		    // Wait for connection
                Socket csocket = ssocket.accept();
    	    	BufferedReader is = new BufferedReader(new InputStreamReader(csocket.getInputStream()));

                String line = null;
    	    	while ((line = is.readLine()) != null) {
                    
                    parseLine(line);
    	    	}
                
    		}
    	} catch (IOException e) {
            ssocket = null;
    		TexlipsePlugin.log("Server error: ", e);
    	}    
    }

    /**
     * Try to parse a "filename:linenumber" -combination
     * and notify listener if parsing is successful.
     * 
     * @param line the line of characters to parse
     */
    private void parseLine(String line) {
        
        int index = line.lastIndexOf(':');
        if (index > 0) {
            
            String file = line.substring(0, index);

            int i = index+1;
            while (i < line.length()
                    && Character.isDigit(line.charAt(i))) {
                i++;
            }
            String num = line.substring(index+1, i);
            
            int lineNumber = -1;
            try {
                lineNumber = Integer.parseInt(num);
            } catch (NumberFormatException e) {
            }
    
            if (listener != null) {
                listener.showLineOfFile(file, lineNumber);
            }
        } else {
            if (line.startsWith("QUIT")) {
                ssocket = null;
            }
        }
    }
}
