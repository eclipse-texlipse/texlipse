package net.sourceforge.texlipse.builder;

import org.eclipse.core.resources.IProject;


/**
 * Builders implementing this interface are notified before the build process starts,
 * so they can reevaluate per-session properties of the project and adjust the build
 * process accordingly.
 *
 * @author Matthias Erll
 *
 */
public interface AdaptableBuilder {

    /**
     * Reads the relevant per-session properties for the project and changes parameters
     * of the build process.
     *
     * @param project project to build
     */
    public void updateBuilder(IProject project);

}
