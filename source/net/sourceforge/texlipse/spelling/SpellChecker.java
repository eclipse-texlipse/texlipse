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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.PathUtils;
import net.sourceforge.texlipse.SelectedResourceManager;
import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.builder.BuilderRegistry;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;

/**
 * An abstraction to a spell checker program.
 * 
 * @author Kimmo Karlsson
 * @author Georg Lippold
 * @author Boris von Loesch
 */
public class SpellChecker implements IPropertyChangeListener {

    // marker type for the spelling errors
    public static final String SPELLING_ERROR_MARKER_TYPE = TexlipseProperties.PACKAGE_NAME + ".spellingproblem";
        
    // preference constants
    public static final String SPELL_CHECKER_COMMAND = "spellCmd";
    public static final String SPELL_CHECKER_ARGUMENTS = "spellArgs";
    public static final String SPELL_CHECKER_ENV = "spellEnv";
    private static final String ASPELL_ENCODING = "UTF-8";

    // These two strings have to have multiple words, because otherwise
    // they may come up in aspells proposals.
    public static String SPELL_CHECKER_ADD = "spellCheckerAddToUserDict";
    // The values are resource bundle entry IDs at startup.
    // They are converted to actual strings in the constructor.
    public static String SPELL_CHECKER_IGNORE = "spellCheckerIgnoreWord";
    
    /**
     * A Spell-checker job.
     */
    static class SpellCheckJob extends WorkspaceJob {

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
        public IStatus runInWorkspace(IProgressMonitor monitor) {
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
    private Map<IMarker, String[]> proposalMap;
    
    // the current language
    private String language;
    
    /**
     * Private constructor, because we want to keep this singleton.
     */
    private SpellChecker() {
        proposalMap = new HashMap<IMarker, String[]>();
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
        prefs.addPropertyChangeListener(instance);
        instance.readSettings();
    }
    
    /**
     * Add the given word to Aspell user dictionary.
     * @param word word to add
     */
    public static void addWordToAspell(String word) {
        //patch 1537979 by daniel309
        if (instance.command == null) instance.readSettings();
        String cmd = instance.command;
        
        BuilderRegistry.printToConsole("aspell> adding word: " + word);

        String[] environp = PathUtils.mergeEnvFromPrefs(PathUtils.getEnv(),
                SPELL_CHECKER_ENV);
        try {
            Process p = Runtime.getRuntime().exec(cmd, environp);
            PrintWriter w = new PrintWriter(new
                        OutputStreamWriter(p.getOutputStream(), ASPELL_ENCODING));
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
        
        args = args.replaceAll("%encoding", ASPELL_ENCODING);

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
        
        boolean restart = false;
        if (pLang != null && pLang.length() > 0) {
            if (!pLang.equals(language)) {
                // current project is different language 
                //than currently running process, so change
                language = pLang;
                restart = true;
            }
        }

        if (restart) {
            stopProgram();
            readSettings();
        }
    }

    /**
     * Check if the spelling program is still running.
     * Restart the program, if necessary.
     * @param file
     */
    protected boolean checkProgram(IFile file) {
        checkLanguage(file);
        if (spellProgram == null) {
            return startProgram();
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
                return startProgram();
            }
        }
        return false;
    }
    
    /**
     * Restarts the spelling program.
     * Assumes that the program is not currently running.
     */
    private boolean startProgram(){
        
        BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("viewerRunning") + ' ' + command);
        try {
            if (command == null) throw new IOException();
            spellProgram = Runtime.getRuntime().exec(command, envp);
            
        } catch (IOException e) {
            spellProgram = null;
            input = null;
            output = null;
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("spellProgramStartError"));
            return false;
        }
        
        // get output and input stream
        try {
            output = new PrintWriter(new
                    OutputStreamWriter(spellProgram.getOutputStream(), ASPELL_ENCODING));
            input = new BufferedReader(new
                    InputStreamReader(spellProgram.getInputStream(), ASPELL_ENCODING));
        }
        catch (UnsupportedEncodingException e1) {
            spellProgram = null;
            input = null;
            output = null;
            BuilderRegistry.printToConsole("Unsupported encoding");
            return false;
        }
        
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
                error.close();
                return false;
            }
            BuilderRegistry.printToConsole("aspell> " + message.trim());
            // Now it's up and running :)
            // put it in terse mode, then it's faster
            output.println("!");
            return true;
        } catch (IOException e) {
            TexlipsePlugin.log("Aspell died", e);
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("spellProgramStartError"));
            return false;
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
        if (instance.checkProgram(file)){
            instance.checkLineSpelling(line, offset, lineNumber, file);
        }
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
        if (instance.checkProgram(file)) {
            instance.checkDocumentSpelling(document, file, monitor);
        }
    }

    /**
     * Check spelling of the entire document.
     * 
     * @param doc the document
     * @param file
     */
    private void checkDocumentSpelling(IDocument doc, IFile file, IProgressMonitor monitor) {
        deleteOldProposals(file);
        //doc.addDocumentListener(instance);
        try {
            int num = doc.getNumberOfLines();
            monitor.beginTask("Check spelling", num);
            for (int i = 0; i < num; i++) {
                if (monitor.isCanceled()) break;
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
     * Replaces all Latex coded umlauts like \"a, "a or \ss by the correct
     * character in ISO-8859-1
     * @param line
     * @return
     */
    private static String replaceUmlauts(String line) {
        //FIXME: replacement of "a or "o is missing
        StringBuilder out = new StringBuilder();
        int addWS = 0;
        for (int i=0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (addWS > 0 && Character.isWhitespace(c)) {
                //Correct position by adding whitespaces
                for (int j = 0; j < addWS; j++) out.append(' ');
                addWS = 0;
            }
            if (c == '\\') {
                if (i+2 < line.length() && line.charAt(i+1) == 's' && line.charAt(i+2) == 's') {
                    if (i+3 == line.length() || Character.isWhitespace(line.charAt(i+3)) 
                            || line.charAt(i+3) == '\\' || line.charAt(i+3) == '}') {
                        out.append('ß');
                        i = i+2;
                        addWS = 2;
                        continue;
                    }
                }
                if (i+1 < line.length() && line.charAt(i+1) == '"') {
                    if (i+2 < line.length()) {
                        char c2 = line.charAt(i+2);
                        i = i+2;
                        addWS = 2;
                        switch (c2) {
                        case 'a':
                            out.append('ä');
                            break;
                        case 'u':
                            out.append('ü');
                            break;
                        case 'o':
                            out.append('ö');
                            break;
                        case 'A':
                            out.append('Ä');
                            break;
                        case 'U':
                            out.append('Ü');
                            break;
                        case 'O':
                            out.append('Ö');
                            break;
                        default:
                            i = i-2;
                            addWS = 0;
                            out.append('\\');
                            break;
                        }
                        continue;
                    }
                }
            }
            out.append(c);
        }
        return out.toString();
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
            lineToPost = replaceUmlauts(line);
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
        List<String> lines = new ArrayList<String>();
        try {
            String result = input.readLine();
            while ((!"".equals(result)) && (result != null)) {
                lines.add(result);
                result = input.readLine();
            }
            //Wait a bit and clear the input buffer (sometimes there are more than one empty line)
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                //No problem
            } 
            while (input.ready()) { 
                input.readLine();
            }
        } catch (IOException e) {
            BuilderRegistry.printToConsole(TexlipsePlugin.getResourceString("spellProgramStartError"));
            TexlipsePlugin.log("aspell error at line " + lineNumber + ": " + lineToPost, e);
        }
        
        // loop through the output lines (they contain only errors)
        for (int i = 0; i < lines.size(); i++) {
            String[] tmp = (lines.get(i)).split(":");
            String[] error = tmp[0].split(" ");
            String word = error[1].trim();
            // column, where the word starts in the line of text
            // is always the last entry in error (sometimes 3, if there
            // are matches, else 2)
            // we have to subtract 1 since the first char is always "^"
            int column = Integer.valueOf(error[error.length - 1]).intValue() - 1;
            
            /*
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
            */
            
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
            options[options.length - 2] = MessageFormat.format(SPELL_CHECKER_IGNORE,
                    new Object[] { word });
            options[options.length - 1] = MessageFormat.format(SPELL_CHECKER_ADD, 
                    new Object[] { word });

            createMarker(file, options, offset + column, word,
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
    private void createMarker(IResource file, String[] proposals, int charBegin, String word, int lineNumber) {
        
        Map<String, ? super Object> attributes = new HashMap<String, Object>();
        attributes.put(IMarker.CHAR_START, Integer.valueOf(charBegin));
        attributes.put(IMarker.CHAR_END, Integer.valueOf(charBegin+word.length()));
        attributes.put(IMarker.LINE_NUMBER, Integer.valueOf(lineNumber));
        attributes.put(IMarker.SEVERITY, Integer.valueOf(IMarker.SEVERITY_WARNING));
        attributes.put(IMarker.MESSAGE, 
            MessageFormat.format(TexlipsePlugin.getResourceString("spellMarkerMessage"),
                new Object[] { word }));
        try {
            IMarker marker = file.createMarker(SPELLING_ERROR_MARKER_TYPE);
            marker.setAttributes(attributes);
            proposalMap.put(marker, proposals);
/*            MarkerUtilities.createMarker(file, attributes, SPELLING_ERROR_MARKER_TYPE);
            addProposal(file, charBegin, charBegin+word.length(), proposals);*/
            
        } catch (CoreException e) {
            TexlipsePlugin.log("Adding spelling marker", e);
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
        Iterator<IMarker> iter = proposalMap.keySet().iterator();
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
        return instance.proposalMap.get(marker);
    }

    /**
     * Returns the actual position of <i>marker</i> or null if the marker was
     * deleted. Code inspired by 
     * @param marker
     * @param sourceViewer
     * @return
     */
    private static int[] getMarkerPosition(IMarker marker, ISourceViewer sourceViewer) {
        int[] p = new int[2];
        p[0] = marker.getAttribute(IMarker.CHAR_START, -1);
        p[1] = marker.getAttribute(IMarker.CHAR_END, -1);
     // look up the current range of the marker when the document has been edited
        IAnnotationModel model= sourceViewer.getAnnotationModel();
        if (model instanceof AbstractMarkerAnnotationModel) {

            AbstractMarkerAnnotationModel markerModel= (AbstractMarkerAnnotationModel) model;
            Position pos= markerModel.getMarkerPosition(marker);
            if (pos != null && !pos.isDeleted()) {
                // use position instead of marker values
                p[0] = pos.getOffset();
                p[1] = pos.getOffset() + pos.getLength();
            }

            if (pos != null && pos.isDeleted()) {
                // do nothing if position has been deleted
                return null;
            }
        }
        return p;
    }
    /**
     * Finds the spelling correction proposals for the word at the given offset.
     * 
     * @param offset text offset in the current file
     * @return completion proposals, or null if there is no marker at the given offset
     */
    public static ICompletionProposal[] getSpellingProposal(int offset, ISourceViewer sourceViewer) {
        
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
            int[] p = getMarkerPosition(markers[i], sourceViewer);
            if (p != null && p[0] <= offset && offset <= p[1]) {
                try {
                    //Update marker's position
                    markers[i].setAttribute(IMarker.CHAR_START, p[0]);
                    markers[i].setAttribute(IMarker.CHAR_END, p[1]);
                } catch (CoreException e) {
                    TexlipsePlugin.log("Error while updating Marker", e);
                }
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
}
