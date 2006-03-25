package net.sourceforge.texlipse.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Point;

public class TexStyleCompletionManager implements IPropertyChangeListener{

    private Map keyValue;
    private static TexStyleCompletionManager theInstance;

    private final static String[] STYLETAGS = new String[] { 
        "\\bf", "\\it", "\\rm", "\\sf", "\\sc", "\\em", "\\huge", "\\Huge"
    };
    private final static String[] STYLELABELS = new String[] { 
        "bold", "italic", "roman", "sans serif", "small caps", "emphasize", "huge", "Huge"
    };
    
    private TexStyleCompletionManager() {
        this.keyValue = new HashMap();
        readSettings();
    }

    public static TexStyleCompletionManager getInstance() {
        if (theInstance == null) {
            theInstance = new TexStyleCompletionManager();
            TexlipsePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(theInstance);
        }
        return theInstance;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getProperty().equals(TexlipseProperties.SMART_KEY_SETTINGS)) {
            readSettings();
        }
    }
    
    /**
     * Reads the smart keys defined at the preference page.
     */
    private void readSettings() {
        String[] keys = TexlipsePlugin.getPreferenceArray(TexlipseProperties.SMART_KEY_SETTINGS);
        
        for (int i = 0; i < keys.length; i++) {
            int index = keys[i].indexOf('=');
            if (index <= 0) {
                continue;
            }
            
            keyValue.put(keys[i].substring(0, index), keys[i].substring(index+1));
        }
    }

    public ICompletionProposal[] getStyleCompletions(String selectedText, Point selectedRange) {
        ICompletionProposal[] result = new ICompletionProposal[keyValue.size()];

        int i=0;
        for (Iterator iter = keyValue.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            String value = (String) keyValue.get(key);
            
            
            String replacement = "{" + key + " " + selectedText + "}";
            int cursor = key.length() + 2;
            IContextInformation contextInfo = new ContextInformation(null, value+" Style");
            result[i] = new CompletionProposal(replacement, 
                    selectedRange.x, selectedRange.y,
                    cursor, null, value,
                    contextInfo, replacement);
            i++;
        }
        
        /*
        // Loop through all styles
        for (int i = 0; i < STYLETAGS.length; i++) {
            String tag = STYLETAGS[i];
            
            // Compute replacement text
            String replacement = "{" + tag + " " + selectedText + "}";
            
            // Derive cursor position
            int cursor = tag.length() + 2;
            
            // Compute a suitable context information
            IContextInformation contextInfo = 
                new ContextInformation(null, STYLELABELS[i]+" Style");
            
            // Construct proposal
            result[i] = new CompletionProposal(replacement, 
                    selectedRange.x, selectedRange.y,
                    cursor, null, STYLELABELS[i], 
                    contextInfo, replacement);
        }*/
        return result;
    }
    
}
