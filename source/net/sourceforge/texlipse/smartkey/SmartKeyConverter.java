package net.sourceforge.texlipse.smartkey;

import java.util.HashMap;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Display;

/**
 * Integration of SmartKey Converting. The smart key converter replaces predefined
 * keys directly when a space is entered. The last replacement can be revoked by 
 * directly pressing backspace.
 * 
 * @author Markus Maus
 * @author Reza Esmaeili Soumeeh
 * @author Ehsan Baghi
 */
public class SmartKeyConverter implements IPropertyChangeListener,
IDocumentListener, VerifyKeyListener {
    
    // the instance.
    private static SmartKeyConverter instance;
    
    // the smart keys.
    private HashMap keyValue;
    
    // temp variables for the replacement.
    private String replacedWord;
    private String newWord;
    private boolean bsPressed;
    private int replacingOffset;
    
    /**
     * Private constructor because of the Singleton pattern.
     * Reads the settings.
     *
     */
    private SmartKeyConverter() {
        readSettings();
        initializeTemps();
    }
    
    /**
     * Initialize the temp variables needed for the smart key conversion and undo functionality.
     *
     */
    private void initializeTemps() {
        replacedWord = null;
        newWord = null;
        bsPressed = false;
        replacingOffset = 0;
    }
    
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(TexlipseProperties.STYLE_COMPLETION_SETTINGS)) {
            readSettings();
        }
    }
    
    /**
     * Reads the smart keys defined at the preference page.
     *
     */
    private void readSettings() {
        String[] keys = TexlipsePlugin.getPreferenceArray(TexlipseProperties.STYLE_COMPLETION_SETTINGS);
        
        keyValue = new HashMap();
        for (int i = 0; i < keys.length; i++) {
            
            int index = keys[i].indexOf('=');
            if (index <= 0) {
                continue;
            }
            
            keyValue.put(keys[i].substring(0, index), keys[i].substring(index+1));
        }
    }
    
    /**
     * Currently unused.
     */
    public void documentAboutToBeChanged(DocumentEvent event) {
    }
    
    /**
     * Checks if a key has been entered, which should be replaced.
     * Replaces the word, if such a key is available.
     * Revokes the change if the backspace has been pressed, and the
     * word has been replaced before.
     */
    public void documentChanged(DocumentEvent event) {
        String word = null;
        final int offset = event.getOffset();
        final IDocument document = event.getDocument();
        
        // get the actual word
        try {
            if (Character.isSpaceChar(document.getChar(offset))) {
                int beginIndex = offset;
                
                while (beginIndex > 0
                        && !Character.isWhitespace(document.getChar(beginIndex-1)))
                    beginIndex--;
                
                word = event.getDocument().get(beginIndex, offset-beginIndex);
            }
        }
        catch (BadLocationException e) {
        }
        
        // check if worked on after a replace. So the undo is no longer available.
        if (replacingOffset < offset ) {
            bsPressed = false;
            replacedWord = null;
        }	
        
        // something to replace.
        if (keyValue.containsKey(word) || (replacedWord != null && bsPressed)) {
            final String key = word;
            
            Display.getCurrent().asyncExec(new Runnable() {
                public void run() {
                    try {
                        // undo.
                        if (replacedWord != null && bsPressed) {
                            document.replace(offset - newWord.length(), newWord.length(), replacedWord + " ");
                            bsPressed = false;
                            replacedWord = null;
                        }
                        // normal replace.
                        else {
                            newWord = (String) keyValue.get(key);
                            if (newWord != null) {
                                document.replace(offset - key.length(), key.length(), newWord);
                                replacedWord = key;
                                replacingOffset = offset - key.length() + newWord.length();
                            }
                        }
                    } catch (BadLocationException e) {
                    }
                }
            });
        }
    }
    
    /**
     * Creates an instance of the smart key converter, if it doesn't exist yet.
     * Adds the propertyChangeListener.
     * @return the instance.
     */
    public static SmartKeyConverter getInstance() {
        if (instance == null) {
            instance = new SmartKeyConverter();
            TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(instance);
        }
        return instance;
    }
    
    /**
     * Checks whether the backspace has been pressed. 
     * Used to revoke a replacement.
     */
    public void verifyKey(VerifyEvent event) {
        if (event.keyCode == SWT.BS)
            bsPressed=true;
    }
}

