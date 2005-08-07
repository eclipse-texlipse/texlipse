/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.spelling;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;

import net.sourceforge.texlipse.PathUtils;
import net.sourceforge.texlipse.SelectedResourceManager;
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.BuilderRegistry;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * An abstraction to a spell checker program.
 * 
 * @author Kimmo Karlsson
 */
public class SpellChecker implements IPropertyChangeListener, IDocumentListener {

    // marker type for the spelling errors
    public static final String SPELLING_ERROR_MARKER_TYPE = TexlipseProperties.PACKAGE_NAME + ".spellingproblem";
        
    // preference constants
    public static final String SPELL_CHECKER_COMMAND = "spellCmd";
    public static final String SPELL_CHECKER_ARGUMENTS = "spellArgs";
    public static final String SPELL_CHECKER_ENV = "spellEnv";
    private static final String SPELL_CHECKER_ENCODING = "spellEnc";
    
    /**
     * Character buffer for external program output.
     */
    class ReaderBuffer implements Runnable {
        
        // the input reader
        private Reader reader;
        
        // buffer for the incoming data
        private StringBuffer buffer;
        
        /**
         * Create a new self-filling buffer.
         * @param r reader
         */
        public ReaderBuffer(Reader r) {
            reader = r;
            buffer = new StringBuffer();
        }
        
        /**
         * @return the current contents of the buffer and clear the buffer
         */
        public String getBuffer() {
            String str = null;
            synchronized (buffer) {
                str = buffer.toString();
                buffer.delete(0, buffer.length());
            }
            return str;
        }
        
        /**
         * Read characters form the input as long as there is input.
         */
        public void run() {
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            
            while (reader != null) {
                try {
                    int ch = reader.read();
                    if (ch == -1) {
                        reader = null;
                        break;
                    }
                    synchronized (buffer) {
                        buffer.append((char)ch);
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
    
    /**
     * A Spell-checker job.
     */
    class SpellCheckJob extends Job {

        // the document to check for spelling
        private IDocument document;
        
        // the file that contains the document
        private IFile file;

        /**
         * Create a new spell-checker job for the given document.
         * @param name document name
         * @param doc document
         */
        public SpellCheckJob(String name, IDocument doc, IFile file) {
            super(name);
            document = doc;
            this.file = file;
        }
        
        /**
         * Run the spell checker.
         */
        protected IStatus run(IProgressMonitor monitor) {
            SpellChecker.checkSpellingDirectly(document, file);
            return new Status(IStatus.OK, TexlipsePlugin.getPluginId(), IStatus.OK, "ok", null);
        }
    }
    
    // the shared instance
    private static SpellChecker instance = new SpellChecker();
    
    // the external spelling program
    private Process spellProgram;
    
    // the stream to the program
    private PrintWriter output;
    
    // the stream from the program
    private ReaderBuffer input;

    // spelling program command with arguments
    private String command;

    // environment variables for the program
    private String[] envp;
    
    // map of proposals so far
    private HashMap proposalMap;
    
    // the current language
    private String language;
    
    /**
     * Private constructor, because we want to keep this singleton.
     */
    private SpellChecker() {
        proposalMap = new HashMap();
        language = "en";
    }

    /**
     * Initialize the spell checker. This method must be called only once.
     * Preferably from PreferenceInitializer.
     * 
     * @param prefs the plugin preferences
     */
    public static void initializeDefaults(IPreferenceStore prefs) {
        
        String aspell = PathUtils.findEnvFile("aspell", "/usr/bin", "aspell.exe", "C:\\gnu\\aspell");
        prefs.setDefault(SPELL_CHECKER_COMMAND, aspell);
        // -a == ispell compatibility mode, -t == tex mode
        prefs.setDefault(SPELL_CHECKER_ARGUMENTS, "-a -t --lang=%language");
        prefs.setDefault(SPELL_CHECKER_ENV, "");
        prefs.setDefault(SPELL_CHECKER_ENCODING, "ISO-8859-1");
        prefs.addPropertyChangeListener(instance);
        instance.readSettings();
    }
    
    /**
     * Read settings from the preferences.
     */
    private void readSettings() {
        
        command = null;
        envp = null;
        
        String path = TexlipsePlugin.getPreference(SPELL_CHECKER_COMMAND);
        if (path == null || path.length() == 0) {
            return;
        }
        
        File f = new File(path);
        if (!f.exists() || f.isDirectory()) {
            return;
        }
        
        String args = TexlipsePlugin.getPreference(SPELL_CHECKER_ARGUMENTS);
        
        args = args.replaceAll("%encoding", TexlipsePlugin.getPreference(SPELL_CHECKER_ENCODING));
        args = args.replaceAll("%language", language);
        
        command = f.getAbsolutePath() + " " + args;
        envp = PathUtils.mergeEnvFromPrefs(PathUtils.getEnv(), SPELL_CHECKER_ENV);
    }

    /**
     * Check that the current language setting is correct.
     */
    private void checkLanguage(IFile file) {
        String pLang = null;
        IProject prj = file.getProject();
        if (prj != null) {
            pLang = TexlipseProperties.getProjectProperty(prj, TexlipseProperties.LANGUAGE_PROPERTY);
        }
        
        if (pLang != null && pLang.length() > 0) {
            if (!pLang.equals(language)) {
                // current project is different language than currently running process, so change
                language = pLang;
                stopProgram();
                readSettings();
            }
        }
    }

    /**
     * Check if the spelling program is still running.
     * Restart the program, if necessary.
     * @param file
     */
    protected void checkProgram(IFile file) {
        checkLanguage(file);
        if (spellProgram == null) {
            startProgram();
        } else {
            int exitCode = -1;
            try {
                exitCode = spellProgram.exitValue();
            } catch (IllegalThreadStateException e) {
                // program is still running, good
            }
            if (exitCode != -1) {
                // an exit code is defined, so program has ended
                spellProgram = null;
                startProgram();
            }
        }
    }
    
    /**
     * Restarts the spelling program.
     * Assumes that the program is not currently running.
     */
    private void startProgram() {
        
        BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("viewerRunning") + ' ' + command);
        try {
            spellProgram = Runtime.getRuntime().exec(command, envp);
            
        } catch (IOException e) {
            spellProgram = null;
            input = null;
            output = null;
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("spellProgramStartError"));
            return;
        }
        
        // get output stream
        output = new PrintWriter(spellProgram.getOutputStream());
        
        // get input stream
        InputStreamReader reader = new InputStreamReader(spellProgram.getInputStream());
        input = new ReaderBuffer(reader);
        new Thread(input).start();
        
        // get error stream
        ReaderBuffer errorStream = new ReaderBuffer(new InputStreamReader(spellProgram.getErrorStream()));
        new Thread(errorStream).start();
        
        // read error message
        String errors = errorStream.getBuffer();
        
        // read the version info
        String message = input.getBuffer();
        
        // just a hack to wait for the aspell program to wake up
        while (message.length() == 0 && errors.length() == 0) {
            Thread.yield();
            // aspell prints to either stdout...
            message = input.getBuffer();
            // ...or stderr, when it starts
            errors = errorStream.getBuffer();
        }
        
        // choose message to print
        if (message.length() == 0) {
            message = errors;
        }
        
        BuilderRegistry.printToConsole("aspell> " + message.trim());
    }

    /**
     * Stop running the spelling program.
     */
    private void stopProgram() {
        if (spellProgram != null) {
            spellProgram.destroy();
            spellProgram = null;
        }
    }

    /**
     * Set the spell checker encoding.
     * @param enc encoding to use
     */
    public static void setEncoding(String enc) {
        TexlipsePlugin.getDefault().getPreferenceStore().setValue(SPELL_CHECKER_ENCODING, enc);
        // don't stop program yet, because the given encoding might not be different from current setting
    }

    /**
     * The IPropertyChangeListener method.
     * Re-reads the settings from preferences.
     */
    public void propertyChange(PropertyChangeEvent event) {
        String prop = event.getProperty();
        if (prop.startsWith("spell")) {
            // encoding, program args or program path changed
            //BuilderRegistry.printToConsole("spelling property changed: " + prop);
            stopProgram();
            readSettings();
        }
    }
    
    /**
     * Check spelling of a single line.
     * 
     * @param line the line of text
     * @param offset start offset of the line in the document
     * @param file file
     * @return fix proposals, or empty array if all correct
     */
    public static void checkSpelling(String line, int offset, int lineNumber, IFile file) {
        instance.checkProgram(file);
        instance.checkLineSpelling(line, offset, lineNumber, file);
    }
    
    /**
     * Check spelling of the entire document.
     * This method returns after scheduling a spell-checker job.
     * @param document document from the editor
     * @param file
     */
    public static void checkSpelling(IDocument document, IFile file) {
        instance.startSpellCheck(document, file);
    }
    
    /**
     * Check spelling of the entire document.
     * This method returns after scheduling a spell-checker job.
     * @param document document from the editor
     */
    private void startSpellCheck(IDocument document, IFile file) {
        Job job = new SpellCheckJob("Spellchecker", document, file);
        job.setUser(true);
        job.schedule();
    }
    
    /**
     * Check spelling of the entire document.
     * This method actually checks the spelling.
     * @param document document from the editor
     */
    private static void checkSpellingDirectly(IDocument document, IFile file) {
        instance.checkProgram(file);
        instance.checkDocumentSpelling(document, file);
    }

    /**
     * Check spelling of the entire document.
     * 
     * @param doc the document
     * @param file
     */
    private void checkDocumentSpelling(IDocument doc, IFile file) {
        deleteOldProposals(file);
        doc.addDocumentListener(instance);
        try {
            int num = doc.getNumberOfLines();
            for (int i = 0; i < num; i++) {
                int offset = doc.getLineOffset(i);
                int length = doc.getLineLength(i);
                String line = doc.get(offset, length);
                checkLineSpelling(line, offset, i+1, file);
            }
        } catch (BadLocationException e) {
            TexlipsePlugin.log("Checking spelling on a line", e);
        }
    }

    /**
     * Check spelling of a single line.
     * This method parses ispell-style spelling error proposals.
     * 
     * @param line the line of text
     * @param offset start offset of the line in the document
     * @param file
     * @return fix proposals, or empty array if all correct
     */
    private void checkLineSpelling(String line, int offset, int lineNumber, IFile file) {
        
        // check that there is text for the checker
        if (line == null || line.length() == 0) {
            return;
        }
        
        line = line.replace('%', ' ');
        if (line.trim().length() == 0) {
            return;
        }
        
        // give the speller something to parse
        output.println(line);
        output.flush();
        Thread.yield();
        
        // read the ReaderBuffer contents
        StringBuffer sb = new StringBuffer();
        String tmp = input.getBuffer();
        
        // wait until there is input
        while (tmp.length() == 0) {
            Thread.yield();
            tmp = input.getBuffer();
        }
        
        // wait until the end of input
        while (tmp.length() > 0) {
            sb.append(tmp);
            Thread.yield();
            tmp = input.getBuffer();
        }
        
        // loop through the output lines
        String[] lines = sb.toString().split(System.getProperty("line.separator"));
        for (int i = 0; i < lines.length; i++) {
            
            if (lines[i] == null || lines[i].length() == 0) {
                continue;
            }
            tmp = lines[i];
            if (tmp.charAt(0) == '&') {
                
                // word starts in column 2 in the message
                int wordLength = tmp.indexOf(' ', 2) - 2;
                String word = tmp.substring(2, wordLength+2);
                
                // column, where the word starts in the line of text
                int column = line.indexOf(word);
                
                // list of proposals starts after the semicolon
                int index = tmp.indexOf(':');
                String[] proposals = tmp.substring(index+2).split(", ");
                
                createMarker(file, proposals, offset + column, wordLength, lineNumber);
            }
        }
    }

    /**
     * Adds a spelling error marker to the given file.
     * 
     * @param file the resource to add the marker to
     * @param proposals list of proposals for correcting the error
     * @param charBegin beginning offset in the file
     * @param wordLength length of the misspelled word
     */
    private void createMarker(IResource file, String[] proposals, int charBegin, int wordLength, int lineNumber) {
        
        HashMap attributes = new HashMap();
        attributes.put(IMarker.CHAR_START, new Integer(charBegin));
        attributes.put(IMarker.CHAR_END, new Integer(charBegin+wordLength));
        attributes.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
        attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
        attributes.put(IMarker.MESSAGE, TexlipsePlugin.getResourceString("spellMarkerMessage"));
        
        try {
            MarkerUtilities.createMarker(file, attributes, SPELLING_ERROR_MARKER_TYPE);
            addProposal(file, charBegin, charBegin+wordLength, proposals);
            
        } catch (CoreException e) {
            TexlipsePlugin.log("Adding spelling marker", e);
        }
    }

    /**
     * Adds a spelling error marker at the given offset in the file.
     * 
     * @param file file the was spell-checked
     * @param begin beginning offset of the misspelled word
     * @param end ending offset of the misspelled word
     * @param proposals correction proposals for the misspelled word
     * @throws CoreException
     */
    private void addProposal(IResource file, int begin, int end, String[] proposals) throws CoreException {
        
        IMarker[] markers = file.findMarkers(SPELLING_ERROR_MARKER_TYPE, false, IResource.DEPTH_ZERO);
        for (int i = 0; i < markers.length; i++) {
            
            int charStart = markers[i].getAttribute(IMarker.CHAR_START, -1);
            int charEnd = markers[i].getAttribute(IMarker.CHAR_END, -1);
            
            if (charStart == begin && charEnd == end) {
                proposalMap.put(markers[i], proposals);
                return;
            }
        }
    }

    /**
     * Clear all spelling error markers.
     */
    public static void clearMarkers(IResource resource) {
        instance.deleteOldProposals(resource);
    }

    /**
     * Deletes all the error markers of the previous check.
     * This has to be done to avoid duplicates.
     * Also, old markers are probably not anymore in the correct positions.
     */
    private void deleteOldProposals(IResource res) {
        
        // delete all markers with proposals, because there might be something in the other files
        Iterator iter = proposalMap.keySet().iterator();
        while (iter.hasNext()) {
            IMarker marker = (IMarker) iter.next();
            try {
                marker.delete();
            } catch (CoreException e) {
                TexlipsePlugin.log("Deleting marker", e);
            }
        }
        
        // just in case delete all markers from this file
        try {
            res.deleteMarkers(SPELLING_ERROR_MARKER_TYPE, false, IResource.DEPTH_ONE);
        } catch (CoreException e) {
            TexlipsePlugin.log("Deleting markers", e);
        }
        
        // clear the old proposals
        proposalMap.clear();
    }

    /**
     * Returns the spelling correction proposal words for the given marker.
     * 
     * @param marker a marker
     * @return correction proposals
     */
    public static String[] getProposals(IMarker marker) {
        return (String[]) instance.proposalMap.get(marker);
    }

    /**
     * Finds the spelling correction proposals for the word at the given offset.
     * 
     * @param offset text offset in the current file
     * @return completion proposals, or null if there is no marker at the given offset
     */
    public static ICompletionProposal[] getSpellingProposal(int offset, int replacementOffset, int replacementLength) {
        
        IResource res = SelectedResourceManager.getDefault().getSelectedResource();
        if (res == null) {
            return null;
        }
        
        IMarker[] markers = null;
        try {
            markers = res.findMarkers(SPELLING_ERROR_MARKER_TYPE, false, IResource.DEPTH_ZERO);
        } catch (CoreException e) {
            return null;
        }

        for (int i = 0; i < markers.length; i++) {
            int charBegin = markers[i].getAttribute(IMarker.CHAR_START, -1);
            int charEnd = markers[i].getAttribute(IMarker.CHAR_END, -1);
            if (charBegin <= offset && offset <= charEnd) {
                SpellingResolutionGenerator gen = new SpellingResolutionGenerator();
                return convertAll(gen.getResolutions(markers[i]), markers[i], replacementOffset, replacementLength);
            }
        }
        
        return null;
    }

    /**
     * Converts the given marker resolutions to completion proposals.
     * 
     * @param resolutions marker resolutions
     * @param marker marker that holds the given resolutions
     * @param 
     * @return completion proposals for the given marker
     */
    private static ICompletionProposal[] convertAll(IMarkerResolution[] resolutions, IMarker marker, int offset, int replacementLength) {
        
        ICompletionProposal[] array = new ICompletionProposal[resolutions.length];
        
        for (int i = 0; i < resolutions.length; i++) {
            SpellingMarkerResolution smr = (SpellingMarkerResolution) resolutions[i];
            array[i] = new SpellingCompletionProposal(smr.getSolution(), offset, replacementLength, marker);
        }
        
        return array;
    }

    /**
     * The manipulation described by the document event will be performed.
     * This implementation does nothing.
     * @param event the document event describing the document change 
     */
    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    /**
     * The manipulation described by the document event has been performed.
     * This implementation updates spelling error markers to get the positions
     * right.
     * @param event the document event describing the document change
     */
    public void documentChanged(DocumentEvent event) {
        int origLength = event.getLength();
        String eventText = event.getText();
        if (eventText == null) {
            eventText = "";
        }
        int length = eventText.length();
        int offset = event.getOffset();
        int diff = length - origLength;
        
        Iterator iter = proposalMap.keySet().iterator();
        while (iter.hasNext()) {
            IMarker marker = (IMarker) iter.next();
            int start = marker.getAttribute(IMarker.CHAR_START, -1);
            if (start > offset) {
                int end = marker.getAttribute(IMarker.CHAR_END, -1);
                try {
                    marker.setAttribute(IMarker.CHAR_START, start + diff);
                    marker.setAttribute(IMarker.CHAR_END, end + diff);
                } catch (CoreException e) {
                    TexlipsePlugin.log("Setting marker location", e);
                }
            }
        }
    }
}
