package net.sourceforge.texlipse.model;

import java.util.Arrays;

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

//    private Map keyValue;
    private static TexStyleCompletionManager theInstance;

    private String[] STYLETAGS = new String[] { 
        "\\textbf{", "\\textit{", "\\textrm{", "\\textsf{", "\\textsc", "\\textsl{", "\\texttt{","\\emph{", "{\\huge", "{\\Huge"
    };
    private String[] STYLELABELS = new String[] { 
        "bold", "italic", "roman", "sans serif", "small caps", "slanted", "teletype", "emphasize", "huge", "Huge"
    };
    
    private TexStyleCompletionManager() {
//        this.keyValue = new HashMap();
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
        if (event.getProperty().equals(TexlipseProperties.STYLE_COMPLETION_SETTINGS)) {
            readSettings();
        }
    }
    
    /**
     * Reads the smart keys defined at the preference page.
     */
    private void readSettings() {
        String[] props = TexlipsePlugin.getPreferenceArray(TexlipseProperties.STYLE_COMPLETION_SETTINGS);

        Arrays.sort(props);
        
        STYLELABELS = new String[props.length];
        STYLETAGS = new String[props.length];
        
        for (int i = 0; i < props.length; i++) {
            String[] pair = props[i].split("=");

//            int index = keys[i].indexOf('=');
//            if (index <= 0) {
//                continue;
//            }            
//            keyValue.put(keys[i].substring(0, index), keys[i].substring(index+1));
            STYLELABELS[i] = pair[0];
            STYLETAGS[i] = pair[1];
        }
    }

    public ICompletionProposal[] getStyleCompletions(String selectedText, Point selectedRange) {

        /*
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
        */
        
        ICompletionProposal[] result = new ICompletionProposal[STYLELABELS.length];
        // Loop through all styles
        for (int i = 0; i < STYLETAGS.length; i++) {
            String tag = STYLETAGS[i];
            
            // Compute replacement text
            String replacement = tag + selectedText + "}";
            
            // Derive cursor position
            int cursor = tag.length() + 1;
            
            // Compute a suitable context information
            IContextInformation contextInfo = 
                new ContextInformation(null, STYLELABELS[i]+" Style");
            
            // Construct proposal
            result[i] = new CompletionProposal(replacement, 
                    selectedRange.x, selectedRange.y,
                    cursor, null, STYLELABELS[i], 
                    contextInfo, replacement);
        }
        return result;
    }
    
    public IContextInformation[] getStyleContext() {
        ContextInformation[] contextInfos = new ContextInformation[STYLELABELS.length];
        
        // Create one context information item for each style
        for (int i = 0; i < STYLELABELS.length; i++) {
            contextInfos[i] = new ContextInformation(null, STYLELABELS[i]+" Style");
        }
        return contextInfos;
    }

}
