package net.sourceforge.texlipse.builder.cache;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.sourceforge.texlipse.builder.cache.ProjectFileInfo.FileProperty;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Reads information about project files from an XML file into a list.
 *
 * @author Matthias Erll
 *
 */
public class ProjectFileCacheReader {

    private final IProject project;
    private Collection<ProjectFileInfo> files;

    private ProjectFileInfo currentFile;
    private FileProperty property;

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

    /**
     * Processes an XML element start tag.
     *
     * @param start start element
     */
    private void processStartElement(final StartElement start) {
        final String name = start.getName().getLocalPart();
        if (ProjectFileInfo.FILE_XML_ELEMENT.equals(name)) {
            final Attribute pathAttr = start.getAttributeByName(new QName("name"));
            if (pathAttr != null) {
                IPath fileName = project.getFile(pathAttr.getValue()).getProjectRelativePath();
                currentFile = new ProjectFileInfo(fileName);
            }
        }
        else {
            property = ProjectFileInfo.getFileProperty(name);
        }
    }

    /**
     * Processes an XML element end tag.
     *
     * @param end end element
     */
    private void processEndElement(final EndElement end) {
        final String name = end.getName().getLocalPart();
        if (ProjectFileInfo.FILE_XML_ELEMENT.equals(name)) {
            files.add(currentFile);
            currentFile = null;
        } else {
            property = null;
        }
    }

    /**
     * Processes characters found in the XML stream and transfers
     * them to the current object.
     *
     * @param characters character object
     */
    private void processCharacters(final Characters characters) {
        if (currentFile != null && property != null) {
            final String str = characters.getData();
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

    /**
     * Processes the given input steam and stores all file information found
     * in a list.
     *
     * @param in input stream
     * @throws XMLStreamException if the XML stream was invalid or could not be read
     */
    private void readFileCache(InputStream in) throws XMLStreamException {
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader parser = null;
        try {
            parser = factory.createXMLEventReader(in);
            XMLEvent event;
            while (parser.hasNext()) {
                event = parser.nextEvent();
                switch (event.getEventType()) {
                case XMLEvent.START_ELEMENT:
                    processStartElement(event.asStartElement());
                    break;
                case XMLEvent.END_ELEMENT:
                    processEndElement(event.asEndElement());
                    break;
                case XMLEvent.CHARACTERS:
                    processCharacters(event.asCharacters());
                    break;
                default:
                }
            }
        }
        finally {
            if (parser != null) {
                parser.close();
            }
        }
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
        try {
            cacheFile.refreshLocal(0, monitor);
            if (cacheFile.exists()) {
                readFileCache(cacheFile.getContents());
            }
        }
        catch (XMLStreamException e) {
            //TODO
        }
        return this.files;
    }

}
