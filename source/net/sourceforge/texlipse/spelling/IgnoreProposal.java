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
import java.util.Set;

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

/**
 * Ignore a word for the rest of the session proposal
 * @author Boris von Loesch
 *
 */
public class IgnoreProposal implements ICompletionProposal {
    private Set<String> fIgnore;
    private String fWord;
    private ISourceViewer fViewer;
    
    public IgnoreProposal(Set<String> ignore, String word, ISourceViewer viewer) {
        fIgnore = ignore;
        fWord = word;
        fViewer = viewer;
    }
    
    public void apply(IDocument document) {
        fIgnore.add(fWord);
        SpellingProblem.removeAll(fViewer, fWord);
    }

    public String getAdditionalProposalInfo() {
        return null;
    }

    public IContextInformation getContextInformation() {
        return null;
    }

    public String getDisplayString() {
        return MessageFormat.format(TexlipsePlugin.getResourceString("spellCheckerIgnoreWord"),
                new Object[] { fWord });
    }

    public Image getImage() {
        return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE);
    }

    public Point getSelection(IDocument document) {
        return null;
    }

}
