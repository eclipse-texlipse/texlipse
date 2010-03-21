/*
 * $Id$
 *
 * Copyright (c) 2004-2010 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.spelling;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.editor.TeXSpellingReconcileStrategy.TeXSpellingProblemCollector;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.spelling.ISpellingEngine;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import com.swabunga.spell.engine.Word;
import com.swabunga.spell.event.SpellCheckEvent;
import com.swabunga.spell.event.SpellCheckListener;
import com.swabunga.spell.event.SpellChecker;
import com.swabunga.spell.event.StringWordTokenizer;
import com.swabunga.spell.event.TeXWordFinder;

/**
 * The default spelling engine for LaTeX files. Uses Jazzy for spell
 * checking.
 * @author Boris von Loesch
 *
 */
public class TexSpellingEngine implements ISpellingEngine, SpellCheckListener {

    public static class TexSpellingProblem extends SpellingProblem {
        
        private SpellCheckEvent fError;
        private int roffset;
        private String fLang;
        
        private final static Image fCorrectionImage = TexlipsePlugin.getImage("correction_change");
        private final static String CHANGE_TO = TexlipsePlugin.getResourceString("spellCheckerChangeWord");
        private final static String MESSAGE = TexlipsePlugin.getResourceString("spellMarkerMessage");
        
        public TexSpellingProblem(SpellCheckEvent error, int roffset, String lang) {
            this.fError = error;
            this.roffset = roffset;
            fLang = lang;
        }
        
        @Override
        public ICompletionProposal[] getProposals() {
            return getProposals(null);
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public ICompletionProposal[] getProposals(IQuickAssistInvocationContext context) {
            List<Word> sugg = fError.getSuggestions();
            int offset = fError.getWordContextPosition() + roffset;
            int length = fError.getInvalidWord().length();
            ICompletionProposal[] props = new ICompletionProposal[sugg.size() + 2];
            for (int i=0; i < sugg.size(); i++) {
                String s = MessageFormat.format(CHANGE_TO,
                        new Object[] { sugg.get(i).toString() });
                props[i] = new CompletionProposal(sugg.get(i).toString(), 
                        offset, length, length, fCorrectionImage, s, null, null);
            }
            props[props.length - 2] = new IgnoreProposal(ignore, fError.getInvalidWord(), context.getSourceViewer());
            props[props.length - 1] = new AddToDictProposal(fError, fLang, context.getSourceViewer());
            return props;
        }

        @Override
        public int getOffset() {
            return fError.getWordContextPosition()  + roffset;
        }

        @Override
        public String getMessage() {
            return MessageFormat.format(MESSAGE, new Object[] { fError.getInvalidWord() });
        }

        @Override
        public int getLength() {
            return fError.getInvalidWord().length();
        }

    }
    
    private final static String DEFAULT_DICT_PATH = "/dict/";
    private final static String DEFAULT_LANG = "en";
    
    private static SpellChecker spellCheck;
    private static TexSpellDictionary dict;
    private static String currentLang;
    private static Set<String> ignore;
    private static Map<String, String> customDicts = new HashMap<String, String>();
    
    private List<SpellCheckEvent> errors;
    
    /**
     * Returns a SpellChecker that checks the language of the current project
     * @param project
     * @return
     */
    private static SpellChecker getSpellChecker(String lang) {
        if (lang.equals(currentLang)) return spellCheck;
        
        //Set spellCheck to null to allow the GC to trash the current dictionary
        spellCheck = null;
        dict = null;
        try {
            URL u = TexlipsePlugin.getDefault().getBundle().getEntry(DEFAULT_DICT_PATH + lang + ".dict");
            if (u == null) {
                if ("en".equals(currentLang)) return spellCheck;
                //We assume that at least an English dictionary exists
                u = TexlipsePlugin.getDefault().getBundle().getEntry(DEFAULT_DICT_PATH + DEFAULT_LANG + ".dict");
                if (u == null) return null;
                lang = DEFAULT_LANG;
            }
            URL u2 = TexlipsePlugin.getDefault().getBundle().getEntry(DEFAULT_DICT_PATH+lang+".phonet");
            Reader r = new InputStreamReader(u.openStream(), "UTF-8");
            TexSpellDictionary dict;
            if (u2 == null) {
                dict = new TexSpellDictionary(r);                
            }
            else {
                Reader r2 = new InputStreamReader(u2.openStream());
                dict = new TexSpellDictionary(r);
                r2.close();
            }
            String custom = customDicts.get(lang);
            if (custom == null) {
                custom = "";
                customDicts.put(lang, custom);
            }
            String customDictPath = TexlipsePlugin.getPreference(TexlipseProperties.SPELLCHECKER_CUSTOM_DICT_DIR);
            if (customDictPath != null && !"".equals(customDictPath.trim())) {
                dict.setUserDict(new File (customDictPath + File.separator + lang + "_user.dict"));
            }
            spellCheck = new SpellChecker(dict);
            TexSpellingEngine.dict = dict;
            currentLang = lang;
            r.close();
            return spellCheck;
        } catch (IOException e) {
            TexlipsePlugin.log("Error while loading dictionary", e);
        }
        return null;
    }
    
    /**
     * <p>Returns the dictionary for that language, or the default dictionary if no
     * exists.</p> 
     * <p><b>Beware:</b> Only use local references for the dictionary, otherwise
     * it can not be trashed by the GC and we get memory problems.
     * @param lang Language of the file
     * @return The dictionary
     */
    public static TexSpellDictionary getDict(String lang) {
        getSpellChecker(lang);
        return dict;
    }
    
    public void check(IDocument document, IRegion[] regions, SpellingContext context, 
            ISpellingProblemCollector collector, IProgressMonitor monitor) {
        
        if (ignore == null) {
            ignore = new HashSet<String>();
        }

        IProject project = TexlipsePlugin.getCurrentProject();
        String lang = DEFAULT_LANG;
        if (project != null) {
            lang = TexlipseProperties.getProjectProperty(project, TexlipseProperties.LANGUAGE_PROPERTY);
        }
        //Get spellchecker for the correct language
        SpellChecker spellCheck = getSpellChecker(lang);
        if (spellCheck == null) return;
        
        if (collector instanceof TeXSpellingProblemCollector) {
            ((TeXSpellingProblemCollector) collector).setRegions(regions);
        }
        
        try {
            spellCheck.addSpellCheckListener(this);
            for (final IRegion r : regions) {
                errors = new LinkedList<SpellCheckEvent>();
                int roffset = r.getOffset();

                spellCheck.checkSpelling(new StringWordTokenizer(
                        document.get(roffset, r.getLength()), new TeXWordFinder()));
                
                for (SpellCheckEvent error : errors) {
                    SpellingProblem p = new TexSpellingProblem(error, roffset, lang);
                    collector.accept(p);
                }
            }
            spellCheck.removeSpellCheckListener(this);                
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void spellingError(SpellCheckEvent event) {
        if (ignore.contains(event.getInvalidWord())) return;
        if (event.getInvalidWord().length() < 3) return;
        if (event.getInvalidWord().indexOf('_') > -1) return;
        if (event.getInvalidWord().indexOf('^') > -1) return;
        errors.add(event);
      }
}
