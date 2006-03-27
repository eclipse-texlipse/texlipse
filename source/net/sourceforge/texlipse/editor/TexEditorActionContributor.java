package net.sourceforge.texlipse.editor;

import net.sourceforge.texlipse.actions.TexInsertMathSymbolAction;
import net.sourceforge.texlipse.model.TexCommandContainer;
import net.sourceforge.texlipse.model.TexCommandEntry;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.editors.text.TextEditorActionContributor;

/**
 * This class creates the math symbol menu
 * 
 * @author Boris von Loesch
 * 
 */
public class TexEditorActionContributor extends TextEditorActionContributor {

    private TexInsertMathSymbolAction[] greekSmall;
    private TexInsertMathSymbolAction[] greekCapital;
    private TexInsertMathSymbolAction[] arrows;
    private TexInsertMathSymbolAction[] stdCompare;
    private TexInsertMathSymbolAction[] stdBinOp;

    /**
     * Fills the actions array with the TexCommandEntries from commands
     * 
     * @param actions
     * @param commands
     */
    private void createMathActions(TexInsertMathSymbolAction[] actions, TexCommandEntry[] commands) {
        for (int i = 0; i < commands.length; i++) {
            String name = commands[i].key;
            actions[i] = new TexInsertMathSymbolAction(name);
            actions[i].setText(name);
            actions[i].setImageDescriptor(commands[i].imageDesc);
        }
    }

    public TexEditorActionContributor() {
        super();
        greekSmall = new TexInsertMathSymbolAction[TexCommandContainer.greekSmall.length];
        greekCapital = new TexInsertMathSymbolAction[TexCommandContainer.greekCapital.length];
        arrows = new TexInsertMathSymbolAction[TexCommandContainer.stdArrows.length];
        stdCompare = new TexInsertMathSymbolAction[TexCommandContainer.stdCompare.length];
        stdBinOp = new TexInsertMathSymbolAction[TexCommandContainer.stdBinOpSymbols.length];
        createMathActions(greekSmall, TexCommandContainer.greekSmall);
        createMathActions(greekCapital, TexCommandContainer.greekCapital);
        createMathActions(arrows, TexCommandContainer.stdArrows);
        createMathActions(stdCompare, TexCommandContainer.stdCompare);
        createMathActions(stdBinOp, TexCommandContainer.stdBinOpSymbols);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.part.EditorActionBarContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
     */
    public void contributeToMenu(IMenuManager menuManager) {
        IMenuManager editMenu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_WINDOW);
        MenuManager manager = new MenuManager("Latex Symbols");
        if (editMenu != null) {
            menuManager.insertBefore(IWorkbenchActionConstants.M_WINDOW, manager);
            MenuManager smallGreekMenu = new MenuManager("Greek lower case");
            MenuManager captialGreekMenu = new MenuManager("Greek upper case");
            MenuManager arrowsMenu = new MenuManager("Arrows");
            MenuManager compareMenu = new MenuManager("Compare symbols");
            MenuManager stdBinOpMenu = new MenuManager("Binary Operator");
            manager.add(captialGreekMenu);
            manager.add(smallGreekMenu);
            manager.add(new Separator());
            manager.add(arrowsMenu);
            manager.add(compareMenu);
            manager.add(stdBinOpMenu);

            for (int i = 0; i < greekSmall.length; i++)
                smallGreekMenu.add(greekSmall[i]);
            for (int i = 0; i < greekCapital.length; i++)
                captialGreekMenu.add(greekCapital[i]);
            for (int i = 0; i < arrows.length; i++)
                arrowsMenu.add(arrows[i]);
            for (int i = 0; i < stdCompare.length; i++)
                compareMenu.add(stdCompare[i]);
            for (int i = 0; i < stdBinOp.length; i++)
                stdBinOpMenu.add(stdBinOp[i]);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IEditorActionBarContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IEditorPart part) {
        super.setActiveEditor(part);
        for (int i = 0; i < greekSmall.length; i++)
            greekSmall[i].setActiveEditor(part);
        for (int i = 0; i < greekCapital.length; i++)
            greekCapital[i].setActiveEditor(part);
        for (int i = 0; i < arrows.length; i++)
            arrows[i].setActiveEditor(part);
        for (int i = 0; i < stdCompare.length; i++)
            stdCompare[i].setActiveEditor(part);
        for (int i = 0; i < stdBinOp.length; i++)
            stdBinOp[i].setActiveEditor(part);
    }

    /*
     * @see IEditorActionBarContributor#init(IActionBars, IWorkbenchPage)
     */
    public void init(IActionBars bars, IWorkbenchPage page) {
        super.init(bars, page);
    }
}
