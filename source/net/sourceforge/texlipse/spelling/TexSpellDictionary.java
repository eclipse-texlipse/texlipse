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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.swabunga.spell.engine.SpellDictionaryASpell;

/**
 * A memory optimized dictionary class 
 * @author Boris von Loesch
 *
 */
public class TexSpellDictionary extends SpellDictionaryASpell {
    private final static int INITIAL_CAPACITY = 32 * 1024;

    private final static char SEP_CHAR = ';';
    
    protected Map<Integer, StringBuilder> mainDictionary = new HashMap<Integer, StringBuilder>(INITIAL_CAPACITY);

    /**
     * User dictionary
     */
    private File dictFile = null;


    /**
     * Dictionary Constructor.
     * @param wordList The file containing the words list for the dictionary
     * @throws java.io.IOException indicates problems reading the words list
     * file
     */
    public TexSpellDictionary(Reader wordList) throws IOException {
        super((File) null);
        createDictionary(new BufferedReader(wordList));
    }

    /**
     * Dictionary constructor that uses an aspell phonetic file to
     * build the transformation table.
     * @param wordList The file containing the words list for the dictionary
     * @param phonetic The reader to use for phonetic transformation of the 
     * wordlist.
     * @throws java.io.IOException indicates problems reading the words list
     * or phonetic information
     */
    public TexSpellDictionary(Reader wordList, Reader phonetic) throws IOException {
        super(phonetic);
        createDictionary(new BufferedReader(wordList));
    }
    
    /**
     * Add words from a file to existing dictionary hashmap.
     * This function can be called as many times as needed to
     * build the internal word list. Duplicates are not added.
     * <p>
     * Note that adding a dictionary does not affect the target
     * dictionary file for the addWord method. That is, addWord() continues
     * to make additions to the dictionary file specified in createDictionary()
     * <P>
     * @param wordList a File object that contains the words, on word per line.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void addDictionary(File wordList) throws FileNotFoundException, IOException {
        addDictionaryHelper(new BufferedReader(new FileReader(wordList)));
    }

    /**
     * Set the user dictionary file
     * @param userDict
     */
    public void setUserDict(File userDict) {
        dictFile = userDict;
        try {
            addDictionary(userDict);
        } catch (IOException e) {
            //Do nothing
        }
    }

    /**
     * Add a word permanently to the dictionary (and the dictionary file).
     * <p>This needs to be made thread safe (synchronized)</p>
     */
    public void addWord(String word) {
        putWordUnique(word);
        if (dictFile == null) return;
        try {
            if (!dictFile.exists()) {
                boolean succ = dictFile.createNewFile();
                if (!succ) return;
            }
            Writer w = new FileWriter(dictFile.toString(), true);
            // Open with append.
            w.write(word);
            w.write("\n");
            w.close();
        } catch (IOException ex) {
        }            
    }

    /**
     * Constructs the dictionary from a word list file.
     * <p>
     * Each word in the reader should be on a separate line.
     * <p>
     * This is a very slow function. On my machine it takes quite a while to
     * load the data in. I suspect that we could speed this up quite allot.
     */
    protected void createDictionary(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() > 0) {
                putWord(line.trim());
            }
        }
    }

    /**
     * Adds to the existing dictionary from a word list file. If the word
     * already exists in the dictionary, a new entry is not added.
     * <p>
     * Each word in the reader should be on a separate line.
     * <p>
     * Note: for whatever reason that I haven't yet looked into, the phonetic codes
     * for a particular word map to a vector of words rather than a hash table.
     * This is a drag since in order to check for duplicates you have to iterate
     * through all the words that use the phonetic code.
     * If the vector-based implementation is important, it may be better
     * to subclass for the cases where duplicates are bad.
     */
    public void addDictionaryHelper(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null) {
            if (line.length() > 0) {
                putWordUnique(line.trim());
            }
        }
    }

    /**
     * Allocates a word in the dictionary
     * @param word The word to add
     */
    protected void putWord(String word) {
        int code = getCode(word).hashCode();
        StringBuilder words = mainDictionary.get(code);
        if (words != null) {
            words.append(word);
            words.append(SEP_CHAR);
        } else {
            words = new StringBuilder();
            words.append(SEP_CHAR);
            words.append(word);
            words.append(SEP_CHAR);
            mainDictionary.put(code, words);
        }
    }

    /**
     * Allocates a word, if it is not already present in the dictionary. A word
     * with a different case is considered the same.
     * @param word The word to add
     */
    protected void putWordUnique(String word) {

        int code = getCode(word).hashCode();
        StringBuilder words = mainDictionary.get(code);

        if (words != null) {
            if (words.indexOf(SEP_CHAR + word + SEP_CHAR) == -1) {
                words.append(word);
                words.append(SEP_CHAR);            
            }
            //else the word is already in the dictionary
        }
        else {
            words = new StringBuilder();
            words.append(SEP_CHAR);
            words.append(word);
            words.append(SEP_CHAR);
            mainDictionary.put(code, words);
        }
    }

    /**
     * Compresses the dictionary so that it takes less memory
     */
    public void compress() {
        Collection<StringBuilder> c = mainDictionary.values();
        for (StringBuilder st : c) {
            st.trimToSize();
        }
    }
    
    /**
     * Returns a list of strings (words) for the code.
     */
    @Override
    public List<String> getWords(String code) {
        //Check the main dictionary.
        StringBuilder mainDictResult = mainDictionary.get(code.hashCode());
        if (mainDictResult == null) return new ArrayList<String>(1);
        StringTokenizer stk = new StringTokenizer(mainDictResult.toString(), ""+SEP_CHAR);
        List<String> list = new ArrayList<String>(1);
        while (stk.hasMoreTokens()) list.add(stk.nextToken());
        return list;
    }

    /**
     * Returns true if the word is correctly spelled against the current word list.
     */
    @Override
    public boolean isCorrect(String word) {
        StringBuilder words = mainDictionary.get(getCode(word).hashCode());
        if (words == null) return false;
        if (words.indexOf(SEP_CHAR + word + SEP_CHAR) >= 0) return true;
        //JMH should we always try the lowercase version. If I dont then capitalised
        //words are always returned as incorrect.
        if (words.indexOf(SEP_CHAR + word.toLowerCase() + SEP_CHAR) >= 0) return true;
        return false;
    }

}
