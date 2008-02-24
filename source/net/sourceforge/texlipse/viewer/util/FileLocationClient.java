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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


/**
 * A client program that connects to a server and
 * writes the given filename and line number to the given port.
 * 
 * @author Esa Seuranen
 * @author Kimmo Karlsson
 */
public class FileLocationClient {

    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final int DEFAULT_PORTNUMBER = 55000;
    
    private String hostName;
    private int portNumber;

    private String fileName;
    private int lineNumber;
    
    /**
     * Create a new File Location client.
     * 
     * @param h host name to connect to. Can be null to mean localhost.
     * @param p port number to connect to. Can be -1 to mean default port.
     * @param f file name that contains the line to show. Can't be null.
     * @param l line number to show. Has to be positive.
     */
    public FileLocationClient(String h, int p, String f, int l) {
        
        if (h == null || h.length() == 0) {
            hostName = DEFAULT_HOSTNAME;
        } else {
            hostName = h;
        }
        
        if (p == -1) {
            portNumber = DEFAULT_PORTNUMBER;
        } else {
            portNumber = p;
        }
        
        fileName = f;
        lineNumber = l;
    }
    
    /**
     * Write the filename and line number to the server socket. 
     * Format: "fileName:lineNumber".
     */
    public void writeToSocket() {
        try {
            
            Socket socket = new Socket(hostName, portNumber);
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            
            out.println(fileName + ':' + lineNumber);
            
            out.flush();
            out.close();
            socket.close();
            
        } catch (UnknownHostException e) {
            System.out.println("Don't know about host: " + hostName);
        } catch (IOException e) {
            System.out.println("Couldn't get I/O for the connection to: "
                    + hostName);
        }
    }
    
    /**
     * Command line arguments:
     *  -p port (default: 55555)
     *  -h host (default: localhost)
     *  -f filename
     *  -l line number
     */
    public static FileLocationClient parseCommandLine(String[] args) {

        String host = null;
        String portNum = null;
        String file = null;
        String lineNum = null;

        // find values of possible parameters
        int i = 0;
        while (i + 1 < args.length) {
            if (args[i].equals("-p")) {
                portNum = args[i + 1];
                i += 2;
            } else if (args[i].equals("-h")) {
                host = args[i + 1];
                i += 2;
            } else if (args[i].equals("-f")) {
                file = args[i + 1];
                i += 2;
            } else if (args[i].equals("-l")) {
                lineNum = args[i + 1];
                i += 2;
            } else {
                System.out.println("Unknown argument "+args[i]);
                i++;
            }
        }
        
        if (file == null) {
            System.out.println("Empty filename");
            return null;
        }
        
        if (lineNum == null) {
            System.out.println("Empty lineNumber");
            return null;
        }

        int line = -1;
        try {
            line = Integer.parseInt(lineNum);
        } catch (NumberFormatException e) {
            System.out.println("Invalid lineNumber: " + lineNum);
            return null;
        }
        
        int port = -1;
        try {
            port = Integer.parseInt(portNum);
        } catch (NumberFormatException e) {
        }
        
        return new FileLocationClient(host, port, file, line);
    }

    /**
     * The main method.
     * @param args command line arguments
     */
    public static void main(String[] args) {

        FileLocationClient client = parseCommandLine(args);
        if (client != null) client.writeToSocket();
    }
}
