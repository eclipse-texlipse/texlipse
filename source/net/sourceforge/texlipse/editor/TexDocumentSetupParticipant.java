/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.editor;

import net.sourceforge.texlipse.editor.partitioner.FastLaTeXPartitionScanner;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

/**
 * @author Antti Pirinen
 *
 */
public class TexDocumentSetupParticipant implements IDocumentSetupParticipant {

	/**
	 * Empty constructor
	 */
	public TexDocumentSetupParticipant() {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.filebuffers.IDocumentSetupParticipant#setup(org.eclipse.jface.text.IDocument)
	 */
	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) document;

			IDocumentPartitioner partitioner = 
			    new FastPartitioner(
			            new FastLaTeXPartitionScanner(), 
			            FastLaTeXPartitionScanner.TEX_PARTITION_TYPES);

			extension3.setDocumentPartitioner(TexEditor.TEX_PARTITIONING, partitioner);
	        
			partitioner.connect(document);
			
		}	
	}
}
