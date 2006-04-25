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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
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
import org.eclipse.core.resources.ResourcesPlugin;
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
 * @author Georg Lippold
 */
public class SpellChecker implements IPropertyChangeListener, IDocumentListener {

    // marker type for the spelling errors
    public static final String SPELLING_ERROR_MARKER_TYPE = TexlipseProperties.PACKAGE_NAME + ".spellingproblem";
        
    // preference constants
    public static final String SPELL_CHECKER_COMMAND = "spellCmd";
    public static final String SPELL_CHECKER_ARGUMENTS = "spellArgs";
    public static final String SPELL_CHECKER_ENV = "spellEnv";
    private static final String SPELL_CHECKER_ENCODING = "spellEnc";
    private static final String encoding = ResourcesPlugin.getEncoding();

    // These two strings have to have multiple words, because otherwise
    // they may come up in aspells proposals.
    public static String SPELL_CHECKER_ADD = "spellCheckerAddToUserDict";
    // The values are resource bundle entry IDs at startup.
    // They are converted to actual strings in the constructor.
    public static String SPELL_CHECKER_IGNORE = "spellCheckerIgnoreWord";

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
            SpellChecker.checkSpellingDirectly(document, file, monitor);
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
    private BufferedReader input;

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
        // these two must be initialized in the constructor, otherwise the resource bundle may not be initialized
        SPELL_CHECKER_ADD = TexlipsePlugin.getResourceString(SPELL_CHECKER_ADD);
        SPELL_CHECKER_IGNORE = TexlipsePlugin.getResourceString(SPELL_CHECKER_IGNORE);
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
        prefs.setDefault(SPELL_CHECKER_ENV, "");
        prefs.setDefault(SPELL_CHECKER_ARGUMENTS,
            "-a -t --lang=%language --encoding=%encoding");
        prefs.setDefault(SPELL_CHECKER_ENCODING, encoding);
        prefs.addPropertyChangeListener(instance);
        instance.readSettings();
    }
    
    /**
     * Add the given word to Aspell user dictionary.
     * @param word word to add
     */
    public static void addWordToAspell(String word) {
        String path = TexlipsePlugin.getPreference(SPELL_CHECKER_COMMAND);
        if (path == null || path.length() == 0) {
            return;
        }

        File f = new File(path);
        if (!f.exists() || f.isDirectory()) {
            return;
        }

        String args = "--encoding=" + encoding
                    + " --lang=" + instance.language + " -a";
        BuilderRegistry.printToConsole("aspell> adding word: " + word);

        String cmd = f.getAbsolutePath() + " " + args;
        String[] environp = PathUtils.mergeEnvFromPrefs(PathUtils.getEnv(),
                SPELL_CHECKER_ENV);
        try {
            Process p = Runtime.getRuntime().exec(cmd, environp);
            PrintWriter w = new PrintWriter(p.getOutputStream());
            w.println("*" + word);
            w.println("#");
            w.flush();
            w.close();
            p.getOutputStream().close();
            p.waitFor();
        } catch (Exception e) {
            BuilderRegistry.printToConsole("Error adding word \""
                    + word + "\" to Aspell user dict\n");
            TexlipsePlugin.log("Adding word \""
                    + word + "\" to Aspell user dict", e);
        }
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
        
        String encoding = TexlipsePlugin.getPreference(SPELL_CHECKER_ENCODING).toLowerCase();
        args = args.replaceAll("%encoding", encoding);

        // Aspell only responds to the name "iso8859-*". And since Eclipse uses
        // the "right" name we need to rename before passing it on to Aspell
        args = args.replaceAll("iso-8859", "iso8859"); 
        
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
        input = new BufferedReader(new InputStreamReader(spellProgram
                        .getInputStream()));
        // read the version info
        try {
            String message = input.readLine();
            if (null == message) { // Something went wrong, get message from aspell's error stream
                BufferedReader error = new BufferedReader(new InputStreamReader(spellProgram.getErrorStream()));
                message = error.readLine();
                if (null == message) {
                    BuilderRegistry.printToConsole("Aspell failed! No output could be read.");
                } else {
                    BuilderRegistry.printToConsole("aspell> " + message.trim());
                }
                return;
            }
            BuilderRegistry.printToConsole("aspell> " + message.trim());
            // Now it's up and running :)
            // put it in terse mode, then it's faster
            output.println("!");
        } catch (IOException e) {
            TexlipsePlugin.log("Aspell died", e);
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("spellProgramStartError"));
        }
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
    private static void checkSpellingDirectly(IDocument document, IFile file, IProgressMonitor monitor) {
        instance.checkProgram(file);
        instance.checkDocumentSpelling(document, file, monitor);
    }

    /**
     * Check spelling of the entire document.
     * 
     * @param doc the document
     * @param file
     */
    private void checkDocumentSpelling(IDocument doc, IFile file, IProgressMonitor monitor) {
        deleteOldProposals(file);
        doc.addDocumentListener(instance);
        try {
            int num = doc.getNumberOfLines();
            monitor.beginTask("Check spelling", num);
            for (int i = 0; i < num; i++) {
                int offset = doc.getLineOffset(i);
                int length = doc.getLineLength(i);
                String line = doc.get(offset, length);
                checkLineSpelling(line, offset, i+1, file);
                monitor.worked(1);
            }
        } catch (BadLocationException e) {
            TexlipsePlugin.log("Checking spelling on a line", e);
        }
        stopProgram();
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
        
        if (line.trim().length() == 0) {
            return;
        }
        
        // give the speller something to parse
        String lineToPost = line;
        if (language.equals("de")) {
            // from Boris Brodski:
            // This is very usefull for german texts
            // It's not a clean solution.
            // TODO The problem: if ASpell found a error in the word with \"... command
            // a marker will set wrong.
            // This is ISO-8859-1
            lineToPost = lineToPost.replaceAll("\\\"a", "ä");
            lineToPost = lineToPost.replaceAll("\\\"u", "ü");
            lineToPost = lineToPost.replaceAll("\\\"o", "ö");
            lineToPost = lineToPost.replaceAll("\\\"A", "Ä");
            lineToPost = lineToPost.replaceAll("\\\"U", "Ü");
            lineToPost = lineToPost.replaceAll("\\\"O", "Ö");
            lineToPost = lineToPost.replaceAll("\\ss ", "ß");
            lineToPost = lineToPost.replaceAll("\\ss\\\\", "ß\\");
            //lineToPost = lineToPost.replaceAll("\\ss[^a-zA-Z]", "ß");
        }
        
        /*
         * a prefixed "^" tells aspell to parse the line without exceptions. From
         * http://aspell.sourceforge.net/man-html/Through-A-Pipe.html#Through-A-Pipe:
         * "lines of single words prefixed with any of `*', `&', `@', `+', `-',
         * `~', `#', `!', `%', or `^'" are also valid and have a special meaning
         * Special meaning of "^" is to ignore all other prefixes.
         * 
         */
        output.println("^" + lineToPost);
        output.flush();
        
        // wait until there is input
        ArrayList lines = new ArrayList();
        try {
            String result = input.readLine();
            while ((!"".equals(result)) && (result != null)) {
                lines.add(result);
                result = input.readLine();
            }
        } catch (IOException e) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("spellProgramStartError"));
            TexlipsePlugin.log("aspell error at line " + lineNumber + ": " + lineToPost, e);
        }
        
        // loop through the output lines (they contain only errors)
        for (int i = 0; i < lines.size(); i++) {
            String[] tmp = ((String) lines.get(i)).split(":");
            String[] error = tmp[0].split(" ");
            String word = error[1].trim();
            // column, where the word starts in the line of text
            // is always the last entry in error (sometimes 3, if there
            // are matches, else 2)
            // we have to subtract 1 since the first char is always "^"
            int column = Integer.valueOf(error[error.length - 1]).intValue() - 1;

            // if we have multi byte chars (e.g. umlauts in utf-8), then aspell
            // returns them as multiple columns. computing the difference
            // between byte-length and String-length:
            byte[] bytes = lineToPost.getBytes();
            byte[] before = new byte[column];
            for (int j = 0; j < column; j++) {
                before[j] = bytes[j];
            }
            int difference = column - (new String(before)).length();
            column -= difference;

            // list of proposals starts after the colon
            String[] options;
            if (tmp.length > 1) {
                String[] proposals = (tmp[1].trim()).split(", ");
                options = new String[proposals.length + 2];
                for (int j = 0; j < proposals.length; j++) {
                    options[j] = proposals[j].trim();
                }
            } else {
                options = new String[2];
            }
            options[options.length - 2] = SPELL_CHECKER_IGNORE;
            options[options.length - 1] = SPELL_CHECKER_ADD;

            createMarker(file, options, offset + column, word.length(),
                    lineNumber);
        }
    }

    /**
     * Adds a spelling error marker to the given file.
     * 
     * @param file the resource to add the marker to
     * @param proposals list of proposals for correcting the error
     * @param charBegin  beginning offset in the file
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
            array[i] = new SpellingCompletionProposal(smr.getSolution(), marker);
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
