package net.sourceforge.texlipse.builder.cache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import net.sourceforge.texlipse.builder.cache.ProjectFileInfo.FileProperty;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Stores cached file information in an XML file in the project folder, so
 * it can be restored between different TeXlipse sessions.
 *
 * @author Matthias Erll
 */
public class ProjectFileCacheWriter {

    private static final int BUFFER_SIZE = 8192;
    private static final String XML_HEADER_STR = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final IProject project;
    private Collection<ProjectFileInfo> files;

    /**
     * Converts the given byte array to a string of hexadecimal values,
     * which can be clearly read in the XML file.
     *
     * @param byteArray byte array
     * @return string of hexadecimal values
     */
    private String byteArrayToHexString(final byte[] byteArray) {
        int len = byteArray.length;
        final char[] charArray = new char[len * 2];
        for (int i = 0; i < len; i++) {
            int b = (int) byteArray[i];
            charArray[i * 2] = Character.forDigit(b >> 4 & 0x0f, 16);
            charArray[i * 2 + 1] = Character.forDigit(b & 0x0f, 16);
        }
        return String.valueOf(charArray);
    }

    /**
     * Writes a simple open tag.
     *
     * @param out output writer
     * @param tag tag name
     * @throws IOException if writing the XML file failed
     */
    private void writeOpenTag(final BufferedWriter out, final String tag)
            throws IOException {
        out.write('<');
        out.write(tag);
        out.write('>');
    }

    /**
     * Writes an open tag with one additional attribute and value.
     *
     * @param out output writer
     * @param tag tag name
     * @param attributeName attribute name
     * @param attributeValue attribute value
     * @throws IOException if writing the XML file failed
     */
    private void writeOpenTag(final BufferedWriter out, final String tag,
            final String attributeName, final String attributeValue)
                    throws IOException {
        out.write('<');
        out.write(tag);
        out.write(' ');
        out.write(attributeName);
        out.write("=\"");
        out.write(attributeValue);
        out.write("\">");
    }

    /**
     * Writes a simple closing tag.
     *
     * @param out output writer
     * @param tag tag name
     * @throws IOException if writing the XML file failed
     */
    private void writeCloseTag(final BufferedWriter out, final String tag)
            throws IOException {
        out.write("</");
        out.write(tag);
        out.write('>');
    }

    /**
     * Writes characters to the XML file, enclosed by tags.
     *
     * @param out output writer
     * @param tag tag name
     * @param value characters
     * @throws IOException if writing the XML file failed
     */
    private void writeCharacters(final BufferedWriter out,
            final String tag, final String value) throws IOException {
        writeOpenTag(out, tag);
        out.write(value);
        writeCloseTag(out, tag);
    }

    /**
     * Writes the current file cache into the given output stream.
     *
     * @param out output writer
     * @throws IOException if writing the XML file failed
     */
    private void writeFileCache(BufferedWriter out) throws IOException {
        out.write(XML_HEADER_STR);
        writeOpenTag(out, ProjectFileInfo.FILE_XML_ROOT);

        if (files != null) {
            for (ProjectFileInfo file : files) {
                if (file.getName() != null) {
                    writeOpenTag(out, ProjectFileInfo.FILE_XML_ELEMENT,
                            ProjectFileInfo.FILE_XML_NAME_ATTR, file.getName().toString());
                    if (file.getModificationStamp() > 0) {
                        writeCharacters(out, ProjectFileInfo.getXmlPropertyStr(
                                FileProperty.MODSTAMP), file.getModificationStamp().toString());
                    }
                    if (file.getHashValue() != null) {
                        writeCharacters(out, ProjectFileInfo.getXmlPropertyStr(
                                FileProperty.HASHVALUE), byteArrayToHexString(file.getHashValue()));
                    }
                    writeCloseTag(out, ProjectFileInfo.FILE_XML_ELEMENT);
                }
            }
        }
        writeCloseTag(out, ProjectFileInfo.FILE_XML_ROOT);
    }

    /**
     * Constructor.
     *
     * @param project current project, for writing the cache file
     */
    public ProjectFileCacheWriter(final IProject project) {
        super();
        this.project = project;
    }

    /**
     * Removes any previously-stored information on cached files.
     *
     * @param monitor progress monitor
     */
    public void clear(IProgressMonitor monitor) {
        IFile cacheFile = project.getFile(".fileCache.xml");
        try {
            if (cacheFile.exists()) {
                cacheFile.delete(true, monitor);
            }
        }
        catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Writes the given collection of files into an XML file, which is located in the project folder.
     * Any previously-stored information is overwritten.
     *
     * @param files files
     * @param monitor progress monitor
     * @throws CoreException if an error occurred while refreshing the persistent store information or
     *  setting attributes to the new file.
     */
    public void writeFiles(final Collection<ProjectFileInfo> files, IProgressMonitor monitor)
            throws CoreException {
        this.files = files;
        IFile cacheFile = project.getFile(".fileCache.xml");
        File fn = new File(cacheFile.getLocationURI());
        FileWriter fileWriter = null;
        BufferedWriter out = null;
        try {
            fileWriter = new FileWriter(fn);
            out = new BufferedWriter(fileWriter, BUFFER_SIZE);
            writeFileCache(out);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
            catch (IOException e) {
            }
        }
        cacheFile.refreshLocal(0, monitor);
        cacheFile.setHidden(true);
        cacheFile.setDerived(true);
    }

}
