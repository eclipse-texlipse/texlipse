/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;


/**
 * Handles creation and deletion of document markers for parsing errors.
 * 
 * @author Oskar Ojala
 */
public class MarkerHandler {

    private static MarkerHandler theInstance;
    
    private MarkerHandler() {
    }

    /**
     * Returns the sole instance of the MarkerHandler
     * 
     * @return The MarkerHandler
     */
    public static MarkerHandler getInstance() {
        if (theInstance == null) {
            theInstance = new MarkerHandler();
        }
        return theInstance;
    }

    /**
     * Create error markers from the given <code>ParseErrorMessage</code>s.
     * 
     * @param editor The editor to add the errors to
     * @param errors The errors to add as instances of <code>ParseErrorMessage</code>
     */
    public void createErrorMarkers(ITextEditor editor, List<ParseErrorMessage> errors) {
        createMarkers(editor, errors, IMarker.PROBLEM);
    }

    /**
     * Create task markers from the given <code>ParseErrorMessage</code>s.
     * 
     * @param editor The editor to add the errors to
     * @param tasks The tasks to add as instances of <code>ParseErrorMessage</code>
     */
    public void createTaskMarkers(ITextEditor editor, List<ParseErrorMessage> tasks) {
        createMarkers(editor, tasks, IMarker.TASK);
    }
    
    /**
     * Creates markers from a given list of <code>ParseErrorMessage</code>s.
     * 
     * @param editor The editor to add the errors to
     * @param markers The markers to add as instances of <code>ParseErrorMessage</code>
     * @param markerType The type of the markers as <code>IMarker</code> types
     */
    private void createMarkers(ITextEditor editor, List<ParseErrorMessage> markers, final String markerType) {
        IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        if (resource == null) return;
        //IResource resource = ((FileEditorInput)editor.getEditorInput()).getFile();
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        
        for (ParseErrorMessage msg : markers) {
            try {
                int beginOffset = document.getLineOffset(msg.getLine() - 1) + msg.getPos();
                
                Map<String, ? super Object> map = new HashMap<String, Object>();
                map.put(IMarker.LINE_NUMBER, Integer.valueOf(msg.getLine()));
                map.put(IMarker.CHAR_START, Integer.valueOf(beginOffset));
                map.put(IMarker.CHAR_END, Integer.valueOf(beginOffset + msg.getLength()));
                map.put(IMarker.MESSAGE, msg.getMsg());
                
                // we can do this since we're referring to a static field
                if (IMarker.PROBLEM == markerType)
                    map.put(IMarker.SEVERITY, Integer.valueOf(msg.getSeverity()));
                
                if (IMarker.TASK == markerType)
                    map.put(IMarker.PRIORITY, Integer.valueOf(msg.getSeverity()));
                
                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (CoreException ce) {
                TexlipsePlugin.log("Creating marker", ce);
            } catch (BadLocationException ble) {
                TexlipsePlugin.log("Creating marker", ble);
            }
        }
    }

    /**
     * Creates warning markers for undefined references. 
     * 
     * @param editor The editor to add the errors to
     * @param errors The errors to add as instances of <code>DocumentReference</code>
     */
    public void createReferencingErrorMarkers(ITextEditor editor, List<DocumentReference> errors) {
        
        IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        if (resource == null) return;
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        
        for (DocumentReference msg : errors) {
            try {
                int beginOffset = document.getLineOffset(msg.getLine() - 1) + msg.getPos();
                
                Map<String, ? super Object> map = new HashMap<String, Object>();
                map.put(IMarker.LINE_NUMBER, Integer.valueOf(msg.getLine()));
                map.put(IMarker.CHAR_START, Integer.valueOf(beginOffset));
                map.put(IMarker.CHAR_END, Integer.valueOf(beginOffset + msg.getLength()));
                map.put(IMarker.MESSAGE, "Key " + msg.getKey() + " is undefined");
                map.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_WARNING));
                
                MarkerUtilities.createMarker(resource, map, IMarker.PROBLEM);
            } catch (CoreException ce) {
                TexlipsePlugin.log("Creating marker", ce);
            } catch (BadLocationException ble) {
                TexlipsePlugin.log("Creating marker", ble);
            }
        }
    }
    
    /**
     * Adds a fatal error to the problem log.
     * 
     * @param editor The editor to add the errors to
     * @param error The error message 
     */
    public void addFatalError(ITextEditor editor, String error) {
        IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        if (resource == null) return;
        //IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        try {
            Map<String, ? super Object> map = new HashMap<String, Object>();
            map.put(IMarker.MESSAGE, error);
            map.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_ERROR));
            
            MarkerUtilities.createMarker(resource, map, IMarker.PROBLEM);
        } catch (CoreException ce) {
            TexlipsePlugin.log("Creating marker", ce);
        }
    }

    /**
     * Delete all error markers from the currently open file.
     * 
     * @param editor The editor to clear the markers from
     */
    public void clearErrorMarkers(ITextEditor editor) {
    	// talk about ugly code...
        // TODO if this case occurs, then the user has probably not correctly created a project
        // -> we should somehow inform the user
        IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        if (resource == null) return;

        try {
            // TODO what should we clear and when?
            // regular problems == parsing errors
            resource.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
            // builder markers == build problems (don't clean them)
            //resource.deleteMarkers(TexlipseBuilder.MARKER_TYPE, false, IResource.DEPTH_INFINITE);
            // don't clear spelling errors
        } catch (CoreException e) {
            TexlipsePlugin.log("Deleting error markers", e);
        }
    }

    /**
     * Clears the problem markers (such as parsing errors)
     * 
     * @param resource The resource whose markers to clear
     */
    public void clearProblemMarkers(IResource resource) {
        try {
            resource.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            TexlipsePlugin.log("Deleting error markers", e);
        }
    }
    
    /**
     * Clears the task markers
     * 
     * @param editor The editor to clear the markers from
     */
    public void clearTaskMarkers(ITextEditor editor) {
        IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
        if (resource == null) return;
        try {
            resource.deleteMarkers(IMarker.TASK, false, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            TexlipsePlugin.log("Deleting task markers", e);
        }
    }

    /**
     * Creates an error marker on the given line
     * 
     * @param resource The resource to create the error to
     * @param message The message for the marker
     * @param lineNumber The line number to create the error on
     */
    public void createErrorMarker(IResource resource, String message, int lineNumber) {
        try {
            Map<String, ? super Object> map = new HashMap<String, Object>();
            map.put(IMarker.LINE_NUMBER, Integer.valueOf(lineNumber));
            map.put(IMarker.MESSAGE, message);
            
            map.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_ERROR));
            
            MarkerUtilities.createMarker(resource, map, IMarker.PROBLEM);
        } catch (CoreException ce) {
            TexlipsePlugin.log("Creating marker", ce);
        }
    }
    
}
