package net.sourceforge.texlipse.builder.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.texlipse.builder.cache.ProjectFileInfo.FileProperty;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Reads information about project files from an XML file into a list.
 *
 * @author Matthias Erll
 *
 */
public class ProjectFileCacheReader {

    private final IProject project;
    private Collection<ProjectFileInfo> files;

    /**
     * Converts the given string of hexadecimal values to a byte array,
     * whereas each byte reflects one hexadecimal value.
     *
     * @param str hex string
     * @return array of bytes, or <code>null</code> if the string was
     *  found to be obviously invalid.
     */
    private static byte[] hexStringToByteArray(final String str) {
        int len = str.length();
        if (len % 2 != 0) {
            // Invalid
            return null;
        }
        final byte[] byteArray = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4)
                    + Character.digit(str.charAt(i + 1), 16));
        }
        return byteArray;
    }

    private DefaultHandler getHandler() {
        return new DefaultHandler() {
            private ProjectFileInfo currentFile;
            private FileProperty property;

            @Override
            public void startElement(String uri, String localName, String qName,
                    Attributes attributes) throws SAXException {
                if (ProjectFileInfo.FILE_XML_ELEMENT.equals(qName)) {
                    final String pathAttr = attributes.getValue("name");
                    if (pathAttr != null) {
                        IPath fileName = project.getFile(pathAttr).getProjectRelativePath();
                        currentFile = new ProjectFileInfo(fileName);
                    }
                }
                else {
                    property = ProjectFileInfo.getFileProperty(qName);
                }
            }
        
            @Override
            public void endElement(String uri, String localName, String qName)
                    throws SAXException {
                if (ProjectFileInfo.FILE_XML_ELEMENT.equals(qName)) {
                    files.add(currentFile);
                    currentFile = null;
                } else {
                    property = null;
                }
            }
        
            @Override
            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                if (currentFile != null && property != null) {
                    final String str = new String(ch, start, length);
                    switch (property) {
                    case MODSTAMP:
                        try {
                            Long modStamp = Long.valueOf(str);
                            currentFile.setModificationStamp(modStamp);
                        }
                        catch (NumberFormatException e) {
                        }
                        break;
                    case HASHVALUE:
                        byte[] hashVal = hexStringToByteArray(str);
                        currentFile.setHashValue(hashVal);
                        break;
                    default:
                    }
                }
            }
        };
    }

    /**
     * Processes the given input steam and stores all file information found
     * in a list.
     *
     * @param in input stream
     * @throws SAXException if the stream could not be processed
     * @throws ParserConfigurationException if the parser config was invalid
     * @throws IOException if the file could not be read
     */
    private void readFileCache(InputStream in) throws SAXException,
            ParserConfigurationException, IOException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(in, getHandler());
    }

    /**
     * Constructor.
     *
     * @param project current project, for locating the cache file
     */
    public ProjectFileCacheReader(final IProject project) {
        super();
        this.project = project;
    }

    /**
     * Reads the XML found in the project folder and returns a set of cached files. If no
     * information from an earlier session was found, or it could not be processed, an empty
     * collection is returned.
     *
     * @param monitor progress monitor
     * @return collection (set) of stored file information
     * @throws CoreException if an error occurred
     */
    public Collection<ProjectFileInfo> readFiles(IProgressMonitor monitor) throws CoreException {
        this.files = new HashSet<ProjectFileInfo>();
        IFile cacheFile = project.getFile(".fileCache.xml");
        InputStream stream = null;
        try {
            cacheFile.refreshLocal(0, monitor);
            if (cacheFile.exists()) {
                stream = cacheFile.getContents();
                readFileCache(stream);
            }
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                }
                catch (IOException e) {
                }
            }
        }
        return this.files;
    }

}
