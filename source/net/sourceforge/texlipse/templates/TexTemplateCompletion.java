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

import java.util.ArrayList;

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
 * @author Esa Seuranen, Oskar Ojala
 * 
 * A class for handling template completitions
 */
public class TexTemplateCompletion extends TemplateCompletionProcessor {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getTemplates(java.lang.String)
     */
    protected Template[] getTemplates(String contextTypeId) {
        return TexlipsePlugin.getDefault().getTexTemplateStore().getTemplates();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.templates.TemplateCompletionProcessor#getContextType(org.eclipse.jface.text.ITextViewer,
     *      org.eclipse.jface.text.IRegion)
     */
    protected TemplateContextType getContextType(ITextViewer viewer,
            IRegion region) {
        return TexlipsePlugin.getDefault().getTexContextTypeRegistry()
                .getContextType(TexContextType.TEX_CONTEXT_TYPE);
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
     * Adds all available TeX templates to the given list, available meaning here that
     * the templates match the found prefix.
     * 
     * @param viewer The viewer associated with this editor
     * @param documentOffset The offset in the document where the completions hould take place
     * @param prefix The prefix of the completion string
     * @return An <code>ArrayList</code> containing the <code>ICompletionProposals</code>
     */
    public ArrayList addTemplateProposals(ITextViewer viewer, int documentOffset, String prefix) {
        ArrayList propList = new ArrayList();
        ICompletionProposal[] templateProposals = 
                computeCompletionProposals(viewer, documentOffset);

        // compute doesn't give the right proposal list for some reason,
        // so we need to filter here
        for (int j = 0; j < templateProposals.length; j++) {
            ICompletionProposal proposal = templateProposals[j];
            if (proposal.getDisplayString().startsWith(prefix)) {
                propList.add(templateProposals[j]);
            }
        }
        return propList;
    }

    /**
     * This method overrides the default one (which is suited for Java
     * (i.e. result in NOT replacing anything before '.', which causes
     * inconvenience, when templates are named like "list.itemize"
     * 
     * @param viewer
     * @param offset Document offset
     * @return prefix (all character counting back from current cursont
     *     position, until a whitespace or the beginning of the file is
     *     encountered
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
            TexlipsePlugin.log("TexTemplateCompletion, extractPrefix.", e);
        }
        return sb.reverse().toString();
    }
}
