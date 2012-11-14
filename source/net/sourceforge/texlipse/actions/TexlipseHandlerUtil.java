package net.sourceforge.texlipse.actions;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.State;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * In addition to HandlerUtil, this class provides some more functions
 * frequently needed by handlers.
 *
 * @author Matthias Erll
 */
public class TexlipseHandlerUtil {

    /**
     * Default constructor.
     */
    protected TexlipseHandlerUtil() {
    }

    /**
     * Retrieves the state with the type toggleState of a command.
     *
     * @param commandId id string of the command
     * @return the state object of the command
     */
    public static State getToggleState(final String commandId) {
        ICommandService service =
                (ICommandService) PlatformUI.getWorkbench()
                .getService(ICommandService.class);
        Command command = service.getCommand(commandId);
        return command.getState("org.eclipse.ui.commands.toggleState");
    }

    /**
     * Checks if the given command has a toggleState, and if it is set.
     *
     * @param commandId id string of the command
     * @return <code>true</code> if both aforementioned conditions are met,
     *  <code>false</code> otherwise or if the command was not found at all.
     */
    public static boolean isStateChecked(final String commandId) {
        State state = getToggleState(commandId);
        if (state != null) {
            return state.getValue() != null
                    && state.getValue().equals(Boolean.TRUE);
        }
        else {
            return false;
        }
    }

    /**
     * Sets the state of the given command, provided that it is given, and a
     * toggleState type.
     *
     * @param commandId id string of the command
     * @param value value to set
     */
    public static void setStateChecked(final String commandId,
            final boolean value) {
        State state = getToggleState(commandId);
        if (state != null) {
            state.setValue(new Boolean(value));
        }
    }

    /**
     * Retrieves the file in the current editor for the given event, provided
     * that it is a file editor.
     *
     * @param event the execution event that occurred
     * @return the file resource, or <code>null</code> if the active editor is
     *  not a file editor
     * @throws ExecutionException if the editor cannot be determined
     */
    public static IFile getFile(final ExecutionEvent event)
            throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
        IEditorInput input = editor.getEditorInput();
        if (input instanceof FileEditorInput) {
            return ((FileEditorInput) input).getFile();
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves the file in the current editor for the given event, provided
     * that it is a file editor.
     *
     * @param evaluationContext evaluation context object
     * @return the file resource, or <code>null</code> if it cannot be
     *  determined
     */
    public static IFile getFile(final Object evaluationContext) {
        Object editor = HandlerUtil.getVariable(evaluationContext,
                "activeEditor");
        if (editor instanceof IEditorPart) {
            IEditorInput input = ((IEditorPart) editor).getEditorInput();
            if (input instanceof FileEditorInput) {
                return ((FileEditorInput) input).getFile();
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves the project which the file in the current editor is part of.
     *
     * @param event the execution event that occurred
     * @return the project, or <code>null</code> if the active editor is not
     *  a file editor
     * @throws ExecutionException if the editor cannot be determined
     */
    public static IProject getProject(final ExecutionEvent event)
            throws ExecutionException {
        IFile file = getFile(event);
        if (file != null) {
            return file.getProject();
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves the project which the file in the current editor is part of.
     *
     * @param evaluationContext evaluation context object
     * @return the project of the file, or <code>null</code> if it cannot be
     *  determined
     */
    public static IProject getProject(final Object evaluationContext) {
        IFile file = getFile(evaluationContext);
        if (file != null) {
            return file.getProject();
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves the currently active text editor.
     *
     * @param event execution event that occurred
     * @return the editor
     * @throws ExecutionException if the editor cannot be determined
     */
    public static ITextEditor getTextEditor(final ExecutionEvent event)
            throws ExecutionException {
        IEditorPart editor = HandlerUtil.getActiveEditorChecked(event);
        if (editor instanceof ITextEditor) {
            return (ITextEditor) editor;
        }
        else {
            throw new RuntimeException("Expecting text editor. Found: "
                    + editor.getClass().getName());
        }
    }

    /**
     * Retrieves the currently active text editor.
     *
     * @param evaluationContext evaluation context object
     * @return the editor, or <code>null</code> if it cannot be determined,
     *  or if it is not a text editor
     */
    public static ITextEditor getTextEditor(final Object evaluationContext) {
        Object editor = HandlerUtil.getVariable(evaluationContext,
                "activeEditor");
        if (editor instanceof ITextEditor) {
            return (ITextEditor) editor;
        }
        else {
            return null;
        }
    }

}
