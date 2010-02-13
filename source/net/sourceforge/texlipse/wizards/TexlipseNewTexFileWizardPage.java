package net.sourceforge.texlipse.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

/**
 * @author Boris von Loesch
 */

public class TexlipseNewTexFileWizardPage extends WizardNewFileCreationPage {

	/**
	 * Constructor
	 * 
	 */
	public TexlipseNewTexFileWizardPage(IStructuredSelection selection) {
		super("wizardPage", selection);
		setTitle("LaTeX/TeX file");
		setDescription("This wizard creates a new file with *.tex extension.");
		setFileExtension("tex");
	}
}