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
 * Build a ps or pdf file from tex file(s) using latex and dvips or dvipdf.
 *
 * @author Kimmo Karlsson
 */
public class DviBuilder extends AbstractBuilder {

    private Builder dvi;
    private ProgramRunner ps;
    private String output;
    private boolean stopped;

    public DviBuilder(int i, String outputFormat) {
        super(i);
        output = outputFormat;
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
            dvi = BuilderRegistry.get(null, TexlipseProperties.OUTPUT_FORMAT_DVI);
        }
        if (ps == null || !ps.isValid()) {
            ps = BuilderRegistry.getRunner(TexlipseProperties.OUTPUT_FORMAT_DVI, output, 0);
        }
        return dvi != null && dvi.isValid() && ps != null && ps.isValid();
    }

    /**
     * @return output format of the dvi-processor
     */
    public String getOutputFormat() {
        return ps.getOutputFormat();
    }

    /**
     * @return sequence
     */
    public String getSequence() {
        return dvi.getSequence() + '+' + ps.getProgramName();
    }
    
    public void stopRunners() {
        // stopRunners instead of stopBuild, because we didn't start a separate thread for DviBuilder
        dvi.stopRunners();
        ps.stop();
        stopped = true;
    }

    public void buildResource(IResource resource) throws CoreException {
        // call buildResource directly, because we don't want a separate thread for DviBuilder
        stopped = false;
        dvi.buildResource(resource);
        if (stopped) 
            return;
        
        monitor.subTask("Converting dvi to " + output);
        ps.run(resource);
        monitor.worked(15);
    }
}
