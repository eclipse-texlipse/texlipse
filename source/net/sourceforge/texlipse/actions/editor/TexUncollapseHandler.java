/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.actions.editor;

import net.sourceforge.texlipse.actions.TexSelections;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Action for uncollapsing code folds. Expands all folds contained in the
 * selection or the fold the cursor is currently at.
 *
 * @author Oskar Ojala
 */
public class TexUncollapseHandler extends AbstractHandler {

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        ITextEditor editor = TexlipseHandlerUtil.getTextEditor(event);
        TexSelections selection = new TexSelections(editor);

        int firstOffset = selection.getStartLine().getOffset();
        int lastOffset = selection.getEndLine().getOffset();

        ProjectionAnnotationModel model = (ProjectionAnnotationModel) editor
                .getAdapter(ProjectionAnnotationModel.class);

        if (model != null) {
            // the predefined method permits us to do this, even if length=0
            model.expandAll(firstOffset, lastOffset - firstOffset);
        }
        return null;
    }

}
