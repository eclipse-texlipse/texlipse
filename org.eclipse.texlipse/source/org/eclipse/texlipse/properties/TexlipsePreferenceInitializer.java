/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.properties;

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.texlipse.PathUtils;
import org.eclipse.texlipse.TexlipsePlugin;
import org.eclipse.texlipse.bibeditor.BibColorProvider;
import org.eclipse.texlipse.builder.BuilderRegistry;
import org.eclipse.texlipse.builder.ProgramRunner;
import org.eclipse.texlipse.editor.ColorManager;
import org.eclipse.texlipse.spelling.SpellChecker;
import org.eclipse.texlipse.viewer.ViewerAttributeRegistry;
import org.eclipse.texlipse.viewer.util.FileLocationClient;


/**
 * Initialize the plugin preferences.
 * 
 * @author Kimmo Karlsson
 * @author Torkild U. Resheim
 */
public class TexlipsePreferenceInitializer extends
        AbstractPreferenceInitializer {

    /**
     * Creates a new preference initializer
     */
    public TexlipsePreferenceInitializer() {
        super();
    }

    /**
     * Save the program paths into preferences.
     * @param pref preferences
     */
    private void initializePaths(IPreferenceStore pref) {
        String path = PathUtils.findInEnvPath("latex", "/Library/TeX/texbin", "latex", "/usr/bin", "latex.exe", "C:\\texmf\\miktex\\bin");
        
        int size = BuilderRegistry.getNumberOfRunners();
        for (int i = 0; i < size; i++) {
            ProgramRunner runner = BuilderRegistry.getRunner(i);
            File prog = new File(path + File.separator + runner.getProgramName());
            if (prog.exists()) {
                runner.initializeDefaults(pref, prog.getAbsolutePath());
            } else {
                runner.initializeDefaults(pref, "");
            }
        }
    }
    
    /**
     * Initialize all preferences to some default values.
     */
    public void initializeDefaultPreferences() {
        IPreferenceStore pref = TexlipsePlugin.getDefault().getPreferenceStore();
        
        pref.setDefault(TexlipseProperties.BIB_DIR, "");
        pref.setDefault(TexlipseProperties.OUTPUT_FORMAT, TexlipseProperties.OUTPUT_FORMAT_PDF);
        pref.setDefault(TexlipseProperties.BUILDER_NUMBER, 2);
        pref.setDefault(TexlipseProperties.BUILDER_CONSOLE_OUTPUT, true);
        pref.setDefault(TexlipseProperties.BUILDER_PARSE_AUX_FILES, true);
        pref.setDefault(TexlipseProperties.BUILD_BEFORE_VIEW, false);
        pref.setDefault(TexlipseProperties.FILE_LOCATION_PORT, FileLocationClient.DEFAULT_PORTNUMBER);
        
        initializePaths(pref);
        
        ColorManager.initializeDefaults(pref);
        
        BibColorProvider.initializeDefaults(pref);
        
        ViewerAttributeRegistry.initializeDefaults(pref);
        
        SpellChecker.initializeDefaults(pref);
        
        pref.setDefault(TexlipseProperties.BUILD_ENV_SETTINGS, "");
        pref.setDefault(TexlipseProperties.VIEWER_ENV_SETTINGS, "");
        pref.setDefault(TexlipseProperties.TEMP_FILE_EXTS, ".aux,.log,.toc,.ind,.ilg,.bbl,.blg,.lot,.lof,.snm,.nav,.out,.vrb,.run.xml,.bcf");
        pref.setDefault(TexlipseProperties.DERIVED_FILES, ".synctex.gz,.synctex,.pdfsync");
        
        pref.setDefault(TexlipseProperties.BIB_COMPLETION, true);
        pref.setDefault(TexlipseProperties.BIB_COMPLETION_DELAY, 500);
        pref.setDefault(TexlipseProperties.TEX_COMPLETION, true);
        pref.setDefault(TexlipseProperties.TEX_COMPLETION_DELAY, 500);
        pref.setDefault(TexlipseProperties.AUTO_PARSING, true);
        pref.setDefault(TexlipseProperties.AUTO_PARSING_DELAY, 2000);
        pref.setDefault(TexlipseProperties.SECTION_CHECK, true);
        
        pref.setDefault(TexlipseProperties.BIB_CODE_FOLDING, true);
        pref.setDefault(TexlipseProperties.BIB_FOLD_INITIAL, false);
        pref.setDefault(TexlipseProperties.BIB_STRING, false);
        
        pref.setDefault(TexlipseProperties.CODE_FOLDING, true);
        pref.setDefault(TexlipseProperties.CODE_FOLDING_PREAMBLE, false);
        pref.setDefault(TexlipseProperties.CODE_FOLDING_PART, false);
        pref.setDefault(TexlipseProperties.CODE_FOLDING_CHAPTER, false);
        pref.setDefault(TexlipseProperties.CODE_FOLDING_SECTION, false);
        pref.setDefault(TexlipseProperties.CODE_FOLDING_SUBSECTION, false);
        pref.setDefault(TexlipseProperties.CODE_FOLDING_SUBSUBSECTION, false);
        pref.setDefault(TexlipseProperties.CODE_FOLDING_PARAGRAPH, false);
        pref.setDefault(TexlipseProperties.CODE_FOLDING_ENVS, "");
        
        pref.setDefault(TexlipseProperties.MATCHING_BRACKETS, true);
        PreferenceConverter.setDefault(pref, TexlipseProperties.MATCHING_BRACKETS_COLOR, new RGB(192, 192, 192));
        
        pref.setDefault(TexlipseProperties.INDENTATION, true);
        pref.setDefault(TexlipseProperties.INDENTATION_LEVEL, 2);
        pref.setDefault(TexlipseProperties.INDENTATION_TABS, false);
        pref.setDefault(TexlipseProperties.INDENTATION_ENVS, "list,enumerate,itemize");
        pref.setDefault(TexlipseProperties.WORDWRAP_LENGTH, 80);
        pref.setDefault(TexlipseProperties.WORDWRAP_TYPE, TexlipseProperties.WORDWRAP_TYPE_HARD);
        pref.setDefault(TexlipseProperties.WORDWRAP_DEFAULT, true);
        pref.setDefault(TexlipseProperties.TEX_ITEM_COMPLETION, true);
        
        pref.setDefault(TexlipseProperties.SMART_BACKSPACE, true);
        pref.setDefault(TexlipseProperties.SMART_PARENS, true);
        pref.setDefault(TexlipseProperties.SMART_QUOTES, true);
        pref.setDefault(TexlipseProperties.SMART_LDOTS, true);
//      B----------------------------------- mmaus
        //pref.setDefault(TexlipseProperties.SMART_KEY_SETTINGS, "ll=list,en=enumerate,iz=itemize");
        pref.setDefault(TexlipseProperties.STYLE_COMPLETION_SETTINGS, "bold=\\textbf{,italic=\\textit{,roman=\\textrm{,sans serif=\\textsf{,small caps=\\textsc{,slanted=\\textsl{,teletype=\\texttt{,emphasize=\\emph{");
//      E----------------------------------- mmaus
        pref.setDefault(TexlipseProperties.TEX_EDITOR_ANNOTATATIONS, true);
        
        pref.setDefault(TexlipseProperties.OUTLINE_PREAMBLE, true);
        pref.setDefault(TexlipseProperties.OUTLINE_PART, true);
        pref.setDefault(TexlipseProperties.OUTLINE_CHAPTER, true);
        pref.setDefault(TexlipseProperties.OUTLINE_SECTION, true);
        pref.setDefault(TexlipseProperties.OUTLINE_SUBSECTION, true);
        pref.setDefault(TexlipseProperties.OUTLINE_SUBSUBSECTION, true);
        pref.setDefault(TexlipseProperties.OUTLINE_PARAGRAPH, true);
        pref.setDefault(TexlipseProperties.OUTLINE_ENVS, "list,enumerate,itemize,figure,table,tabular");
        
        pref.setDefault(TexlipseProperties.ECLIPSE_BUILDIN_SPELLCHECKER, true);
        pref.setDefault(TexlipseProperties.SPELLCHECKER_IGNORE_COMMENTS, true);
        pref.setDefault(TexlipseProperties.SPELLCHECKER_IGNORE_MATH, false);
        pref.setDefault(TexlipseProperties.SPELLCHECKER_IGNORE_MIXED_CASE, true);
        
        pref.setDefault(TexlipseProperties.BUILDER_FORCE_RETURN_FOCUS, true);
    }
}
