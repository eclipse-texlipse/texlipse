/*
 * $Id$
 *
 * Copyright (c) 2004-2005 by the TeXlapse Team.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package net.sourceforge.texlipse.builder;

import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Build a pdf file from tex file(s) using
 * either pslatex+ps2pdf or latex+dvips+ps2pdf.
 *
 * @author Kimmo Karlsson
 */
public class PsBuilder extends AbstractBuilder {

    private Builder dvi;
    private ProgramRunner pdf;
    private Class<? extends Builder> builderClass;
    private boolean stopped;

    public PsBuilder(int i, Class<? extends Builder> clazz) {
        super(i);
        builderClass = clazz;
        isValid();
    }

    public void reset(final IProgressMonitor monitor) {
        super.reset(monitor);
        dvi.reset(monitor);
    }

    /**
     * Check if the needed program runners are operational.
     * Update runners from registry if necessary.
     * @return true, if this builder is ready for operation, false otherwise
     */
    public boolean isValid() {
        if (dvi == null) {
            dvi = BuilderRegistry.get(builderClass, TexlipseProperties.OUTPUT_FORMAT_PS);
        }
        if (pdf == null || !pdf.isValid()) {
            pdf = BuilderRegistry.getRunner(TexlipseProperties.OUTPUT_FORMAT_PS, TexlipseProperties.OUTPUT_FORMAT_PDF, 0);
        }
        return dvi != null && dvi.isValid() && pdf != null && pdf.isValid();
    }

    /**
     * @return pdf
     */
    public String getOutputFormat() {
        return TexlipseProperties.OUTPUT_FORMAT_PDF;
    }
    
    /**
     * @return sequence
     */
    public String getSequence() {
        return dvi.getSequence() + '+' + pdf.getProgramName();
    }

    public void stopRunners() {
        // stopRunners instead of stopBuild, because we didn't start a separate thread for DviBuilder
        dvi.stopRunners();
        pdf.stop();
        stopped = true;
    }

    public void buildResource(IResource resource) throws CoreException {
        // call buildResource directly, because we don't want a separate thread for DviBuilder
        stopped = false;
        dvi.buildResource(resource);
        if (stopped) 
            return;
        
        monitor.subTask("Converting dvi to pdf");
        pdf.run(resource);
        monitor.worked(15);
    }
}
