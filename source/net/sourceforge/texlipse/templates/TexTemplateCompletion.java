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
import java.util.Collections;

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
 * A class for handling (tex)template completitions.
 * 
 * @author Esa Seuranen
 * @author Oskar Ojala
 */
public class TexTemplateCompletion extends TemplateCompletionProcessor {

    TemplateContextType context;
    
    /**
     * 
     */
    public TexTemplateCompletion(String contextName) {
        super();
        this.context = TexlipsePlugin.getDefault().getTexContextTypeRegistry()
        .getContextType(contextName);
    }

    /**
     * Returns the templates valid for the context type specified by <code>contextTypeId</code>.
     * This implementation always returns the shared TexTemplateStore.
     * @see net.sourceforge.texlipse.TexlipsePlugin#getTexTemplateStore()
     * @param contextTypeId the context type id
     * @return the templates valid for this context type id
     */
    protected Template[] getTemplates(String contextTypeId) {
        return TexlipsePlugin.getDefault().getTexTemplateStore().getTemplates();
    }

    /**
     * Returns the context type that can handle template insertion at the given
     * region in the viewer's document.
     * This implementation always returns the TEX_CONTEXT_TYPE.
     * @see TexContextType
     * @param viewer the text viewer
     * @param region the region into the document displayed by viewer
     * @return the context type that can handle template expansion for the given location, or <code>null</code> if none exists
     */
    protected TemplateContextType getContextType(ITextViewer viewer,
            IRegion region) {
//        return TexlipsePlugin.getDefault().getTexContextTypeRegistry()
//                .getContextType(TexContextType.TEX_CONTEXT_TYPE);
//        return TexlipsePlugin.getDefault().getTexContextTypeRegistry()
//               .getContextType(TexContextType.MATH_CONTEXT_TYPE);
        return context;
    }

    /**
     * Returns an image for the given template.
     * This implementation always returns the same icon (called "template").
     * 
     * @param template template
     * @return image for the given template
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
        Collections.sort(propList, new CompletionProposalComparator());
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
