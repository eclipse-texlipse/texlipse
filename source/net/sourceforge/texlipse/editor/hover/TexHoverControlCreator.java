package net.sourceforge.texlipse.editor.hover;

import net.sourceforge.texlipse.editor.TexEditor;

import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.swt.widgets.Shell;

/**
 * @author boris
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TexHoverControlCreator implements IInformationControlCreator {

	private TexEditor editor;
	
	public TexHoverControlCreator(TexEditor editor){
		this.editor = editor;
	}
	
	public IInformationControl createInformationControl(Shell parent) {
		return new TexInformationControl(editor, parent);
	}

}
