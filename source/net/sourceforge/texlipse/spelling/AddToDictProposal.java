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

import java.text.MessageFormat;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import com.swabunga.spell.event.SpellCheckEvent;


/**
 * Add a word to user dictionary proposal
 * @author Boris von Loesch
 *
 */
public class AddToDictProposal implements ICompletionProposal {

    private SpellCheckEvent ferror;
    private String fLang;
    private ISourceViewer fviewer;
    
    public AddToDictProposal(SpellCheckEvent error, String lang, 
            ISourceViewer viewer) {
        ferror = error;
        fLang = lang;
        fviewer = viewer;
    }
    
    public void apply(IDocument document) {
        TexSpellDictionary dict = TexSpellingEngine.getDict(fLang);
        dict.addWord(ferror.getInvalidWord());
        SpellingProblem.removeAll(fviewer, ferror.getInvalidWord());
    }

    public String getAdditionalProposalInfo() {
        return null;
    }

    public IContextInformation getContextInformation() {
        return null;
    }

    public String getDisplayString() {
        return MessageFormat.format(TexlipsePlugin.getResourceString("spellCheckerAddToUserDict"),
                new Object[] { ferror.getInvalidWord() });
    }

    public Image getImage() {
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD);
    }

    public Point getSelection(IDocument document) {
        return null;
    }
}
