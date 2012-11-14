/*
 * $Id$
 *
 * Copyright (c) 2006 by the TeXlipse team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions.editor;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.bibeditor.BibEditor;
import net.sourceforge.texlipse.builder.KpsewhichRunner;
import net.sourceforge.texlipse.editor.TexEditor;
import net.sourceforge.texlipse.model.AbstractEntry;
import net.sourceforge.texlipse.model.TexCommandEntry;
import net.sourceforge.texlipse.properties.TexlipseProperties;
import net.sourceforge.texlipse.texparser.LatexParserUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.SubStatusLineManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;


/**
 * This handler opens the declaration of
 * <ul><li>References</li>
 * <li>Citations</li>
 * <li>Custom commands (/newcommand)</li>
 * <li>include, input and bibliography.</li>
 * </ul>
 *
 * @author Boris von Loesch
 */
public class OpenDeclarationHandler extends AbstractHandler {

    /**
     * Prints an error message on the status line and make a beep.
     * @param message the error message
     * @param editor  the editor
     */
    private static void createStatusLineErrorMessage(final TexEditor editor,
            final String message) {
        SubStatusLineManager slm =
            (SubStatusLineManager) editor.getEditorSite().getActionBars().getStatusLineManager();
        slm.setErrorMessage(message);
        slm.setVisible(true);

        editor.getViewer().getTextWidget().getDisplay().beep();
    }

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        IEditorPart targetEditor = HandlerUtil.getActiveEditorChecked(event);

        TexEditor editor;
        if (targetEditor instanceof TexEditor) {
            editor = (TexEditor) targetEditor;
        }
        else {
            throw new RuntimeException("Expecting text editor. Found: "+ targetEditor.getClass().getName());
        }
        IProject project = editor.getProject();
        if (project == null)
            return null;

        ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
        IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        String docString = doc.get();

        //Get command under cursor
        IRegion comRegion = LatexParserUtils.getCommand(docString, selection.getOffset());
        if (comRegion == null) {
            createStatusLineErrorMessage(editor,
                    TexlipsePlugin.getResourceString("gotoDeclarationNoCommandFound"));
            return null;
        }
        String command = docString.substring(comRegion.getOffset(), comRegion.getOffset() + comRegion.getLength());

        AbstractEntry refEntry = null;
        if (selection.getOffset() < comRegion.getOffset() + comRegion.getLength()) {
            //Cursor is over a command, not the argument, we first try to find the command in the user defined commands
            List<TexCommandEntry> entries = editor.getDocumentModel().getRefMana().getCompletionsCom(command.substring(1), TexCommandEntry.NORMAL_CONTEXT);
            if (entries != null && entries.size() > 0 && entries.get(0).fileName != null) {
                //the command is defined by the user
                refEntry = entries.get(0);
            }
        }

        if (refEntry == null && (command.indexOf("ref") >= 0 || command.indexOf("cite") >=0 ||
                command.equals("\\input") || command.equals("\\include")) ||
                command.equals("\\bibliography")) {
            //We need the argument
            IRegion region = null;
            region = LatexParserUtils.getCommandArgument(docString, comRegion.getOffset());
            if (region == null) {
                createStatusLineErrorMessage(editor,
                        TexlipsePlugin.getResourceString("gotoDeclarationNoArgumentFound"));
                return null;
            }
            String ref = docString.substring(region.getOffset(), region.getOffset() + region.getLength());

            if (command.indexOf("ref") >= 0) {
                //Find the matching label
                refEntry = editor.getDocumentModel().getRefMana().getLabel(ref);
            }
            else if (command.indexOf("cite") >= 0) {
                //There could be more than one reference (e.g. cite1,cite2)
                if (ref.indexOf(',') > 0) {
                    int cIndex = selection.getOffset() - region.getOffset();
                    if (cIndex < 0) {
                        createStatusLineErrorMessage(editor,
                                TexlipsePlugin.getResourceString("gotoDeclarationNoArgumentFound"));
                        return null;
                    }
                    if (ref.charAt(cIndex) == ',') cIndex--;
                    int start = ref.lastIndexOf(',', cIndex) + 1;
                    if (start < 0)
                        start = 0;
                    int end = ref.indexOf(',', cIndex);
                    if (end < 0)
                        end = ref.length();
                    ref = ref.substring(start, end);
                }
                refEntry = editor.getDocumentModel().getRefMana().getBib(ref.trim());
            }
            else if (command.equals("\\include") || command.equals("\\input") ||
                    command.equals("\\bibliography")) {

                if (command.equals("\\bibliography")) {
                    if (!ref.toLowerCase().endsWith(".bib")) {
                        ref = ref + ".bib";
                    }
                }
                else {
                    if (!ref.toLowerCase().endsWith(".tex")) {
                        ref = ref + ".tex";
                    }
                }

                IContainer dir = TexlipseProperties.getProjectSourceDir(project);
                IResource file = dir.findMember(ref);

                if (file == null) {
                    //Fallback strategy, try to find the file from the referring file ddir
                    IFile refFile = editor.getDocumentModel().getFile();
                    if (refFile != null) {
                        dir = refFile.getParent();
                        file = dir.findMember(ref);
                    }
                }

                if (file == null) {
/*                  TODO: Kpsewhich support
                    KpsewhichRunner filesearch = new KpsewhichRunner();
                    try {
                        String filepath = filesearch.getFile(editor.getDocumentModel().getFile(), refEntry.fileName, "bibtex");
                        File f = new File(filepath);
                        //Open the correct document and jump to label
                        IDE.openEditor(editor.getEditorSite().getPage(), f.toURI(), BibEditor.ID, true);
                    } catch (PartInitException e) {
                        TexlipsePlugin.log("Jump2Label PartInitException", e);
                    } catch (CoreException ce) {
                        TexlipsePlugin.log ("Can't run Kpathsea", ce)
                    }*/
                    createStatusLineErrorMessage(editor,
                            MessageFormat.format(TexlipsePlugin.getResourceString("gotoDeclarationNoFileFound"),
                            new Object[]{ref}));
                    return null;
                }
                try {
                    IDE.openEditor(editor.getEditorSite().getPage(), (IFile) file.getAdapter(IFile.class));
                }
                catch (PartInitException e) {
                    TexlipsePlugin.log("Open declaration:", e);
                }
                return null;
            }
        }

        if (refEntry == null || refEntry.fileName == null) {
            createStatusLineErrorMessage(editor,
                    TexlipsePlugin.getResourceString("gotoDeclarationNoDeclarationFound"));
            return null;
        }

        IFile file = project.getFile(refEntry.fileName);
        try {
            AbstractTextEditor part;
            if (!file.exists()) {
                //Try kpathsea
                KpsewhichRunner filesearch = new KpsewhichRunner();
                String filepath = filesearch.getFile(editor.getDocumentModel().getFile(), refEntry.fileName, "bibtex");
                if ("".equals(filepath)) {
                    createStatusLineErrorMessage(editor,
                            TexlipsePlugin.getResourceString("gotoDeclarationNoDeclarationFound"));
                    return null;
                }
                File f = new File(filepath);
                //Open the correct document and jump to label
                part = (AbstractTextEditor) IDE.openEditor(editor.getEditorSite().getPage(),
                        f.toURI(), BibEditor.ID, true);
            }
            else {
                //Open the correct document and jump to label
                part = (AbstractTextEditor) IDE.openEditor(editor.getEditorSite().getPage(), file);
            }
            IDocument doc2 = part.getDocumentProvider().getDocument(part.getEditorInput());
            int lineOffset = doc2.getLineOffset(refEntry.startLine - 1);
            int offset = 0;
            if (command.indexOf("ref") >= 0 && refEntry.position != null) {
                offset = refEntry.position.offset;
            }
            part.getEditorSite().getSelectionProvider().setSelection(
                    new TextSelection(lineOffset + offset, 0));
        }
        catch (PartInitException e) {
            TexlipsePlugin.log("Jump2Label PartInitException", e);
        }
        catch (BadLocationException e) {
            TexlipsePlugin.log("Jump2Label BadLocationException", e);
        }
        catch (CoreException ce) {
            TexlipsePlugin.log("Can't run Kpathsea", ce);
        }
        return null;
    }

}
