package net.sourceforge.texlipse.builder.cache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

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
 *
 */
public class ProjectFileCacheWriter {

    private final IProject project;
    private final XMLEventFactory eventFactory;
    private Collection<ProjectFileInfo> files;

    private XMLEvent fileNameEvent;
    private XMLEvent modStampEvent;
    private XMLEvent hashValueEvent;

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
     * Creates XML events for writing the one particular cache file object.
     *
     * @param file cached file information
     */
    private void createFileEvents(final ProjectFileInfo file) {
        fileNameEvent = null;
        modStampEvent = null;
        hashValueEvent = null;
        if (file.getName() != null) {
            fileNameEvent = eventFactory.createAttribute(ProjectFileInfo.FILE_XML_NAME_ATTR, file.getName().toString());
            if (file.getModificationStamp() > 0) {
                modStampEvent = eventFactory.createCharacters(file.getModificationStamp().toString());
            }
            if (file.getHashValue() != null) {
                hashValueEvent = eventFactory.createCharacters(byteArrayToHexString(file.getHashValue()));
            }
        }
    }

    /**
     * Writes the current file cache into the given output stream.
     *
     * @param out output stream
     * @throws XMLStreamException if writing to the XML stream failed
     */
    private void writeFileCache(OutputStream out) throws XMLStreamException {
        final XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLEventWriter writer = null;
        try {
            writer = factory.createXMLEventWriter(out);
            final XMLEvent headerStartEvent = eventFactory.createStartDocument();
            final XMLEvent rootStartEvent = eventFactory.createStartElement("", "", ProjectFileInfo.FILE_XML_ROOT);
            final XMLEvent headerEndEvent = eventFactory.createEndDocument();
            final XMLEvent rootEndEvent = eventFactory.createEndElement("", "", ProjectFileInfo.FILE_XML_ROOT);
            final XMLEvent fileStartEvent = eventFactory.createStartElement("", "", ProjectFileInfo.FILE_XML_ELEMENT);
            final XMLEvent fileEndEvent = eventFactory.createEndElement("", "", ProjectFileInfo.FILE_XML_ELEMENT);
            final XMLEvent modStartEvent = eventFactory.createStartElement("", "", ProjectFileInfo.getXmlPropertyStr(FileProperty.MODSTAMP));
            final XMLEvent modEndEvent = eventFactory.createEndElement("", "", ProjectFileInfo.getXmlPropertyStr(FileProperty.MODSTAMP));
            final XMLEvent hashStartEvent = eventFactory.createStartElement("", "", ProjectFileInfo.getXmlPropertyStr(FileProperty.HASHVALUE));
            final XMLEvent hashEndEvent = eventFactory.createEndElement("", "", ProjectFileInfo.getXmlPropertyStr(FileProperty.HASHVALUE));

            writer.add(headerStartEvent);
            writer.add(rootStartEvent);
            if (files != null) {
                for (ProjectFileInfo file : files) {
                    writer.add(fileStartEvent);
                    createFileEvents(file);
                    if (fileNameEvent != null) {
                        writer.add(fileNameEvent);
                        if (modStampEvent != null) {
                            writer.add(modStartEvent);
                            writer.add(modStampEvent);
                            writer.add(modEndEvent);
                        }
                        if (hashValueEvent != null) {
                            writer.add(hashStartEvent);
                            writer.add(hashValueEvent);
                            writer.add(hashEndEvent);
                        }
                    }
                    writer.add(fileEndEvent);
                }
            }
            writer.add(rootEndEvent);
            writer.add(headerEndEvent);
        }
        finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Constructor.
     *
     * @param project current project, for writing the cache file
     */
    public ProjectFileCacheWriter(final IProject project) {
        super();
        this.project = project;
        this.eventFactory = XMLEventFactory.newInstance();
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
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(fn);
            writeFileCache(stream);
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
        }
        catch (XMLStreamException e) {
            // TODO Auto-generated catch block
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
        cacheFile.refreshLocal(0, monitor);
        cacheFile.setHidden(true);
        cacheFile.setDerived(true);
    }

}
