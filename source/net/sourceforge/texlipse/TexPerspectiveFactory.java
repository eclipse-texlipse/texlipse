package net.sourceforge.texlipse;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

/**
 * A simple LaTeX perspective
 * 
 * @author Boris von Loesch
 *
 */
public class TexPerspectiveFactory implements IPerspectiveFactory {
    
    private final static String ID_TABLE_VIEW = "net.sourceforge.texlipse.TableView";
    private final static String ID_FULL_OUTLINE = "net.sourceforge.texlipse.FullOutline";
    private final static String ID_PROJECT_WIZARD = "net.sourceforge.texlipse.TexProjectWizard";
    private final static String ID_LATEX_FILE_WIZARD = "net.sourceforge.texlipse.wizards.TexlipseNewTexFileWizard";

    public void createInitialLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
                
        //Navigator view left
        layout.addView(IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.LEFT, 0.25f, editorArea);
        
        //Outline view on the left
        IFolderLayout left = layout.createFolder("left", IPageLayout.BOTTOM, 0.50f, 
                IPageLayout.ID_PROJECT_EXPLORER);
        left.addView(IPageLayout.ID_OUTLINE);
        left.addView(ID_FULL_OUTLINE);
        
        IFolderLayout bottom =
           layout.createFolder(
              "bottom",
              IPageLayout.BOTTOM,
              0.70f,
              editorArea);
        bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
        bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);
        bottom.addView(IPageLayout.ID_TASK_LIST);
        bottom.addPlaceholder(IPageLayout.ID_BOOKMARKS);
        bottom.addView(ID_TABLE_VIEW);
        
        layout.addShowViewShortcut(IPageLayout.ID_BOOKMARKS);
        layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(ID_FULL_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
        layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
        layout.addShowViewShortcut(ID_TABLE_VIEW);

        //Add project and Latex file creation wizards to menu
        layout.addNewWizardShortcut(ID_PROJECT_WIZARD);
        layout.addNewWizardShortcut(ID_LATEX_FILE_WIZARD);
    }

}
