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

import java.util.Map;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;


/**
 * Listens for word wrap toggle -actions, toggling wrap on or off.
 *
 * @author Laura Takkinen
 * @author Oskar Ojala
 * @author Matthias Erll
 */
public class TexWordWrapHandler extends AbstractHandler
    implements IElementUpdater {

    /** Command ID string. */
    private static final String COMMAND_ID_STR = "net.sourceforge.texlipse.commands.texWordWrap";

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        Command command = event.getCommand();
        boolean checked = !HandlerUtil.toggleCommandState(command);
        TexlipsePlugin.getDefault().getPreferenceStore()
            .setValue(TexlipseProperties.WORDWRAP_DEFAULT, checked);
        return null;
    }

    @Override
    public final void setEnabled(final Object evaluationContext) {
        super.setEnabled(evaluationContext);
        boolean checked = TexlipsePlugin.getDefault().getPreferenceStore()
                .getBoolean(TexlipseProperties.WORDWRAP_DEFAULT);
        TexlipseHandlerUtil.setStateChecked(COMMAND_ID_STR, checked);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("rawtypes")
    public final void updateElement(final UIElement element, final Map parameters) {
        element.setChecked(TexlipseHandlerUtil.isStateChecked(COMMAND_ID_STR));
    }

}
