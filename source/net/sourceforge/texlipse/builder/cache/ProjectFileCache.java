package net.sourceforge.texlipse.builder.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Gathers and stores information about files in the project folder, namely the modification
 * stamp and hash value of the file contents. This is used by the ProjectFileTracking
 * for distinction between changes in modification stamps and actual changes in file contents.
 * This class also takes care of making the information persistent and restoring it
 * from earlier TeXlipse sessions.
 * <p>
 * As this cache is updated on every LaTeX run-through, it should also at any time
 * reflect the state of the build directory during the build. This means, that also
 * temporary files should be cached as being in the build directory and not in the
 * temporary folder.
 *
 * @author Matthias Erll
 *
 */
public class ProjectFileCache {

    private static final String MD_ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;
    private static final Map<IProject, ProjectFileCache> instances =
            new HashMap<IProject, ProjectFileCache>();

    private final IProject project;

    private Map<IPath, ProjectFileInfo> trackedFiles;
    private Map<IPath, ProjectFileInfo> initialFiles;
    private boolean initialized;

    /**
     * Retrieves the cache instance for the given project. If it does not exist yet,
     * it is created.
     *
     * @param project current project
     * @return cache instance
     */
    public static synchronized ProjectFileCache getInstance(final IProject project) {
        ProjectFileCache cache = instances.get(project);
        if (cache == null) {
            cache = new ProjectFileCache(project);
            instances.put(project, cache);
        }
        return cache;
    }

    /**
     * Generates a hash value of the given file.
     *
     * @param hashFile IPath reference to the file
     * @return byte array with file contents hash value
     * @throws IOException if the file does not exist or cannot be read
     */
    private byte[] getFileHash(IPath hashFile) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(MD_ALGORITHM);
            IFile inIFile = project.getFile(hashFile);
            File inputFile = new File(inIFile.getLocationURI());
            byte[] buffer = new byte[BUFFER_SIZE];
            InputStream in =
                    new BufferedInputStream(new FileInputStream(inputFile));
            int l;
            while ((l = in.read(buffer)) != -1) {
                digest.update(buffer, 0, l);
            }
            in.close();
            return digest.digest();
        }
        catch (NoSuchAlgorithmException e) {
            // Should not occur if JRE is set up properly
            return null;
        }
    }

    /**
     * Creates a new file record using the given information and adds it to the
     * set of tracked files.
     *
     * @param name project relative file path
     * @param modStamp modification stamp
     * @param hashValue hash value of the file contents
     */
    private void addTrackedFile(IPath name, Long modStamp, byte[] hashValue) {
        ProjectFileInfo file = new ProjectFileInfo(name);
        file.setModificationStamp(modStamp);
        file.setHashValue(hashValue);
        trackedFiles.put(name, file);
    }

    /**
     * Creates a new project file cache instance, which stores the project's information.
     *
     * @param project project
     */
    private ProjectFileCache(IProject project) {
        super();
        this.initialized = false;
        this.project = project;
        this.trackedFiles = new HashMap<IPath, ProjectFileInfo>();
    }

    /**
     * Initializes the file cache with the contents from an earlier build cycle or session.
     * This should be called after moving temporary files to the build folder (if applicable),
     * and before merging in the current snapshots of the project folder.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void restore(IProgressMonitor monitor) throws CoreException {
        if (!initialized) {
            initialFiles = new HashMap<IPath, ProjectFileInfo>();
            ProjectFileCacheReader reader = new ProjectFileCacheReader(project);
            for (ProjectFileInfo info : reader.readFiles(monitor)) {
                initialFiles.put(info.getName(), info);
            }
            initialized = true;
        }
        else {
            initialFiles = trackedFiles;
        }
        trackedFiles = new HashMap<IPath, ProjectFileInfo>(initialFiles.size());
    }

    /**
     * Updates the set of tracked files with the given information. If the given modification
     * stamp differs from the previously recorded information, this method also checks if the
     * file's contents have changed.
     *
     * @param name project relative file path
     * @param modStamp current modification stamp of the file
     * @return <code>true</code> only if the file contents have actually changed, or the file
     *  is new; <code>false</code> if the file has not changed at all or shown to be identical
     *  in contents.
     * @throws IOException if attempts to calculate a new hash value have failed because the
     *  file was not accessible. In this case, no update is made to the cached information either.
     */
    public boolean updateTrackedFile(IPath name, Long modStamp) throws IOException {
        ProjectFileInfo fileInfo = trackedFiles.get(name);
        if (fileInfo != null) {
            Long oldStamp = fileInfo.getModificationStamp();
            if (oldStamp == null || !oldStamp.equals(modStamp)) {
                final byte[] newHashVal = getFileHash(name);
                final byte[] oldHashVal = fileInfo.getHashValue();
                // If an IOException occurs above, make sure the modification stamp is not
                // updated either
                fileInfo.setModificationStamp(modStamp);
                if (oldHashVal == null || !Arrays.equals(oldHashVal, newHashVal)) {
                    fileInfo.setHashValue(newHashVal);
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return false;
            }
        }
        else {
            addTrackedFile(name, modStamp, getFileHash(name));
            return true;
        }
    }

    /**
     * Moves initial information from an earlier session or build cycle to a set
     * of currently-tracked files, and checks if files have been changed or modified since
     * then. This should take place before the first latex run-through starts in a build
     * cycle. File information not merged using this method is considered obsolete and
     * will be discarded upon the next restore.
     *
     * @param name project relative file path
     * @param currentModStamp the modification stamp from the current snapshot
     * @return <code>true</code> only if the file contents have actually changed, or the file
     *  is new; <code>false</code> if the file has not changed at all, or shown to be identical
     *  in contents.
     * @throws IOException if attempts to calculate a new hash value have failed because the
     *  file was not accessible. In this case, the file remains in the initial set only.
     */
    public boolean mergeTrackedFile(IPath name, Long currentModStamp)
                throws IOException {
            ProjectFileInfo initialInfo = initialFiles.get(name);
            if (initialInfo != null) {
                // The file has been in the cache before; check if it is newer
                trackedFiles.put(name, initialInfo);
                return updateTrackedFile(name, currentModStamp);
            }
            else {
                // The file has not been cached before
                addTrackedFile(name, currentModStamp, getFileHash(name));
                return true;
            }
        }

    /**
     * Makes the information in this cache persistent, so it can be restored between
     * TeXlipse sessions. However, file information is still retained in the memory.
     *
     * @param monitor progress monitor
     * @throws CoreException if an error occurs
     */
    public void save(IProgressMonitor monitor) throws CoreException {
        ProjectFileCacheWriter writer = new ProjectFileCacheWriter(project);
        writer.writeFiles(trackedFiles.values(), monitor);
    }

    /**
     * Discards (i.e. deletes) all information previously gathered on files in the project
     * folder.
     *
     * @param monitor progress monitor
     */
    public void clear(IProgressMonitor monitor) {
        trackedFiles.clear();
        initialFiles = trackedFiles;
        ProjectFileCacheWriter writer = new ProjectFileCacheWriter(project);
        writer.clear(monitor);
    }

}
