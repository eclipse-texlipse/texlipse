package net.sourceforge.texlipse.wizards;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

/**
 * @author Boris von Loesch
 */

public class TexlipseNewTexFileWizard extends Wizard implements INewWizard {
	private TexlipseNewTexFileWizardPage page;
	private IStructuredSelection selection;
	private IWorkbench workbench;
	
	/**
	 * Constructor for TexlipseNewTexFileWizard.
	 */
	public TexlipseNewTexFileWizard() {
		super();
		setWindowTitle("New LaTeX file");
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
	    page = new TexlipseNewTexFileWizardPage(selection);
	    addPage(page);
	}

	@Override
	public boolean performFinish() {
	    IFile file = page.createNewFile();
	    if (file != null) {
	        try {
                IDE.openEditor(workbench.getActiveWorkbenchWindow().getActivePage(), file);
            } catch (PartInitException ex) {
                TexlipsePlugin.log("Error while opening file", ex);
            }
	        return true;
	    }
	    else {
	        return false;
	    }
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	    this.workbench = workbench;
	    this.selection = selection;
	}
}