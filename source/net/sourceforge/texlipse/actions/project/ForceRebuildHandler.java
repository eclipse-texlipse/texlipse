/*
 * $Id
 */
package net.sourceforge.texlipse.actions.project;

import net.sourceforge.texlipse.TexlipsePlugin;
import net.sourceforge.texlipse.actions.TexlipseHandlerUtil;
import net.sourceforge.texlipse.properties.TexlipseProperties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;


/**
 * Forces a (partial) rebuild of the current project.
 *
 * @author Boris von Loesch
 * @author Matthias Erll
 */
public class ForceRebuildHandler extends AbstractHandler {

    /**
     * {@inheritDoc}
     */
    public final Object execute(final ExecutionEvent event) throws ExecutionException {
        final IProject project = TexlipseHandlerUtil.getProject(event);
        if (project == null) {
            return null;
        }
        TexlipseProperties.setSessionProperty(project, TexlipseProperties.FORCED_REBUILD, true);
        IProgressService ps = PlatformUI.getWorkbench().getProgressService();
        Job buildJob = new Job("Forced build") {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    project.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
                    TexlipseProperties.setSessionProperty(project, TexlipseProperties.FORCED_REBUILD, null);
                }
                catch (CoreException e) {
                    TexlipsePlugin.log("Force rebuild CoreException", e);
                }
                return new Status(Status.OK, TexlipsePlugin.getPluginId(), "Finished");
            }
        };
        buildJob.schedule();
        ps.showInDialog(null, buildJob);

        return null;
    }

}
