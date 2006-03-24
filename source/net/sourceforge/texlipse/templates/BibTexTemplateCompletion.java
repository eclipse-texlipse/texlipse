/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.templates;

import java.util.Collections;
import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.swt.graphics.Image;


/**
 * @author Esa Seuranen
 *
 * A class for handling BiBTeX template completitions.
 */
public class BibTexTemplateCompletion extends TemplateCompletionProcessor {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
     */
    protected Template[] getTemplates(String contextTypeId) {
        return TexlipsePlugin.getDefault().getBibTemplateStore().getTemplates();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
     *      org.eclipse.jface.text.IRegion)
     */
    protected TemplateContextType getContextType(ITextViewer viewer,
            IRegion region) {
        return TexlipsePlugin.getDefault().getBibContextTypeRegistry()
                .getContextType(BibTexContextType.BIBTEX_CONTEXT_TYPE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getImage(org.eclipse.jface.text.templates.Template)
     */
    protected Image getImage(Template template) {
        return TexlipsePlugin.getImage("template");
    }

    /**
     * Adds all available BiBTeX templates to the given list
     * 
     * @param viewer
     * @param documentOffset
     * @param propList The list, into which the proposals are added
     */
    public void addTemplateProposals(ITextViewer viewer, int documentOffset,
            List propList) {
        
        ICompletionProposal[] templateProposals = 
                computeCompletionProposals(viewer, documentOffset);

        for (int j = 0; j < templateProposals.length; j++) {
            propList.add(templateProposals[j]);
        }
        
        Collections.sort(propList,new CompletionProposalComparator());
    }

    /**
     * This method overrides the default one (which is suited for Java
     * (i.e. result in NOT replacing anything before '.', which causes
     * inconvenience, when templates are named like "list.itemize"
     * 
     * @param viewer
     * @param offset Document offset
     * @return prefix (all character counting back from current cursont
     *     position, until a space(' '), linefeed('\n'), carriage return('\r'),
     *     a tab('\t') or the beginning of the file is encountered
     */
    protected String extractPrefix(ITextViewer viewer, int offset) {
        int i = offset - 1;
        if (i == -1) {
            return "";
        }
        
        StringBuffer sb = new StringBuffer("");
        char c;
        try {
            c = viewer.getDocument().getChar(i);
            while (!Character.isWhitespace(c)) {
                sb.append(c);
                i--;
                if (i < 0) {
                    break;
                } else {
                    c = viewer.getDocument().getChar(i);
                }
            }
        } catch (BadLocationException e) {
            TexlipsePlugin.log("BibTemplateCompletion, extractPrefix.", e);
        }
        return sb.reverse().toString();
    }
}
