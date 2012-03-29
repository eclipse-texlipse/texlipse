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

import net.sourceforge.texlipse.builder.factory.BuilderDescription;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Build a ps or pdf file from tex file(s) using latex and dvips or dvipdf.
 *
 * @author Kimmo Karlsson
 */
public class DviBuilder extends AbstractBuilder implements AdaptableBuilder {

    private Builder dvi;
    private ProgramRunner ps;
    private boolean stopped;

    public DviBuilder(BuilderDescription description) {
        super(description);
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
            dvi = BuilderRegistry.getBuilder(description.getSecondaryBuilderId());
        }
        if (ps == null || !ps.isValid()) {
            ps = BuilderRegistry.getRunner(description.getRunnerId());
        }
        return dvi != null && dvi.isValid() && ps != null && ps.isValid();
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
        
        monitor.subTask("Converting dvi to " + description.getOutputFormat());
        ps.run(resource);
        monitor.worked(15);
    }

    public void updateBuilder(IProject project) {
        if (dvi instanceof AdaptableBuilder) {
            ((AdaptableBuilder) dvi).updateBuilder(project);
        }
    }
}
