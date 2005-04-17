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
import net.sourceforge.texlipse.editor.TexDocumentProvider;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.MarkerUtilities;

/**
 * Character buffer for external program output.
 * @author Kimmo Karlsson
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
        while (reader != null) {
            try {
                int ch = reader.read();
                if (ch == -1) {
                    reader = null;
                    return;
                }
                synchronized (buffer) {
                    buffer.append((char)ch);
                }
            } catch (IOException e) {
            }
        }
    }
}

/**
 * An abstraction to a spell checker program.
 * 
 * @author Kimmo Karlsson
 */
public class SpellChecker implements IPropertyChangeListener {

    // marker type for the spelling errors
    public static final String SPELLING_ERROR_MARKER_TYPE = TexlipseProperties.PACKAGE_NAME + ".spellingproblem";
        
    // preference constants
    public static final String SPELL_CHECKER_COMMAND = "spellCmd";
    public static final String SPELL_CHECKER_ARGUMENTS = "spellArgs";
    public static final String SPELL_CHECKER_ENV = "spellEnv";
    private static final String SPELL_CHECKER_ENCODING = "spellEnc";
    
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
    
    // document provider to use with editors
    private TexDocumentProvider provider;
    
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
        prefs.setDefault(SPELL_CHECKER_ARGUMENTS, "-a -t --encoding=%encoding --lang=%language");
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
    private void checkLanguage() {
        String pLang = null;
        IProject prj = TexlipsePlugin.getCurrentProject();
        if (prj != null) {
            pLang = TexlipseProperties.getProjectProperty(prj, TexlipseProperties.LANGUAGE_PROPERTY);
        }
        
        if (pLang != null && pLang.length() > 0) {
            if (!pLang.equals(language)) {
                language = pLang;
                readSettings();
            }
        }
    }

    /**
     * Check spelling of a single line.
     * 
     * @param line the line of text
     * @param offset start offset of the line in the document
     * @return fix proposals, or empty array if all correct
     */
    public static void checkSpelling(String line, int offset, int lineNumber) {
        instance.checkProgram();
        instance.checkLineSpelling(line, offset, lineNumber);
    }
    
    /**
     * Check spelling of the entire document.
     * 
     * @param document document editor
     */
    public static void checkSpelling(IDocument document) {
        instance.checkProgram();
        instance.checkDocumentSpelling(document);
    }
    
    /**
     * Check if the spelling program is still running.
     * Restart the program, if necessary.
     */
    protected void checkProgram() {
        checkLanguage();
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
        
        // read error message
        ReaderBuffer errorStream = new ReaderBuffer(new InputStreamReader(spellProgram.getErrorStream()));
        new Thread(errorStream).start();
        Thread.yield();
        String errors = errorStream.getBuffer();
        
        // read the version info
        Thread.yield();
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
        instance.stopProgram();
    }
    
    /**
     * Check spelling of a single line.
     * This method parses ispell-style spelling error proposals.
     * 
     * @param line the line of text
     * @param offset start offset of the line in the document
     * @return fix proposals, or empty array if all correct
     */
    protected void checkLineSpelling(String line, int offset, int lineNumber) {
        
        // check if we can mark the errors
        IResource res = SelectedResourceManager.getDefault().getSelectedResource();
        if (res == null) {
            //BuilderRegistry.printToConsole("Can't find reference to selected file.");
            return;
        }
        
        // give the speller something to parse
        line = line.replace('%', ' ');
        output.println(line);
        output.flush();
        Thread.yield();
        
        // read the ReaderBuffer contents
        StringBuffer sb = new StringBuffer();
        String tmp = input.getBuffer();
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
                
                createMarker(res, proposals, offset + column, wordLength, lineNumber);
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
     * Returns the spelling correction proposal words for the given marker.
     * 
     * @param marker a marker
     * @return correction proposals
     */
    public static String[] getProposals(IMarker marker) {
        return (String[]) instance.proposalMap.get(marker);
    }
    
    /**
     * Check spelling of the entire document.
     * 
     * @param doc the document
     */
    protected void checkDocumentSpelling(IDocument doc) {
        deleteOldProposals();
        checkProgram();
        try {
            int num = doc.getNumberOfLines();
            for (int i = 0; i < num; i++) {
                int offset = doc.getLineOffset(i);
                int length = doc.getLineLength(i);
                String line = doc.get(offset, length);
                checkLineSpelling(line, offset, i+1);
            }
        } catch (BadLocationException e) {
        }
    }

    /**
     * Deletes all the error markers of the previous check.
     * This has to be done to avoid duplicates.
     * Also, old markers are probably not anymore in the correct positions.
     */
    private void deleteOldProposals() {
        Iterator iter = proposalMap.keySet().iterator();
        while (iter.hasNext()) {
            IMarker mark = (IMarker) iter.next();
            try {
                mark.delete();
            } catch (CoreException e) {
            }
        }
        proposalMap.clear();
    }

    /**
     * The IPropertyChangeListener method.
     * Re-reads the settings from preferences.
     */
    public void propertyChange(PropertyChangeEvent event) {
        String prop = event.getProperty();
        if (prop.startsWith("spell")) {
            readSettings();
        }
    }

    /**
     * Finds the spelling correction proposals for the word at the given offset.
     * 
     * @param offset text offset in the current file
     * @return completion proposals, or null if there is no marker at the given offset
     */
    public static ICompletionProposal[] getSpellingProposal(int offset) {
        
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
                return convertAll(gen.getResolutions(markers[i]), markers[i]);
            }
        }
        
        return null;
    }

    /**
     * Converts the given marker resolutions to completion proposals.
     * 
     * @param resolutions marker resolutions
     * @param marker marker that holds the given resolutions
     * @return completion proposals for the given marker
     */
    private static ICompletionProposal[] convertAll(IMarkerResolution[] resolutions, IMarker marker) {
        
        ICompletionProposal[] array = new ICompletionProposal[resolutions.length];
        
        for (int i = 0; i < resolutions.length; i++) {
            SpellingMarkerResolution smr = (SpellingMarkerResolution) resolutions[i];
            array[i] = new SpellingCompletionProposal(smr.getSolution(), smr.getOffset(), smr.getLength(), marker);
        }
        
        return array;
    }
}
