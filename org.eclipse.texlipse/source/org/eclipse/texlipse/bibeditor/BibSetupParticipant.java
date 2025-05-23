/*******************************************************************************
 * Copyright (c) 2017, 2025 TeXlipse and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The TeXlipse team - initial API and implementation
 *******************************************************************************/

package org.eclipse.texlipse.bibeditor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

/**
 * A document setup participant for the BibTeX editor. 
 * 
 * @author Oskar Ojala
 */
public class BibSetupParticipant implements IDocumentSetupParticipant {

    /**
     * Creates a new document setup participant.
     */
    public BibSetupParticipant() {
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.filebuffers.IDocumentSetupParticipant#setup(org.eclipse.jface.text.IDocument)
     */
    public void setup(IDocument document) {
        if (document instanceof IDocumentExtension3) {
            IDocumentExtension3 extension3 = (IDocumentExtension3) document;
            IDocumentPartitioner partitioner = 
                new FastPartitioner(new BibPartitionScanner(), BibPartitionScanner.BIB_PARTITION_TYPES);
            extension3.setDocumentPartitioner(BibEditor.BIB_PARTITIONING, partitioner);
            partitioner.connect(document);
        }
    }
}
