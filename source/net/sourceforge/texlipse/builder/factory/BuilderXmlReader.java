package net.sourceforge.texlipse.builder.factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import net.sourceforge.texlipse.TexlipsePlugin;


/**
 * Reads builder information from the XML file, which can be used later by a BuilderFactory
 * for instantiating builders.
 *
 * @author Matthias Erll
 *
 */
public class BuilderXmlReader {

    private final XMLInputFactory factory;

    /**
     * Constructor.
     */
    public BuilderXmlReader() {
        super();
        factory = XMLInputFactory.newInstance();
    }

    /**
     * Retrieves information about runners from the given XML stream.
     *
     * @param in input stream to read runner information from
     * @return map with runner ids and descriptions
     * @throws XMLStreamException if the stream could not be processed
     */
    public Map<String, RunnerDescription> getRunnersFromStream(final InputStream in)
            throws XMLStreamException {
        RunnerXmlHandler handler = new RunnerXmlHandler();
        XMLEventReader parser = factory.createXMLEventReader(in);
        handler.readXmlFile(parser);
        return handler.getRunners();
    }

    /**
     * Retrieves information about builders from the given XML stream.
     *
     * @param in input stream to read builder information from
     * @return map with builder ids and descriptions
     * @throws IOException if the stream could not be read
     * @throws XMLStreamException if the stream could not be processed
     */
    public Map<String, BuilderDescription> getBuildersFromStream(final InputStream in)
            throws IOException, XMLStreamException {
        BuilderXmlHandler handler = new BuilderXmlHandler();
        XMLEventReader parser = factory.createXMLEventReader(in);
        handler.readXmlFile(parser);
        return handler.getBuilders();
    }

    /**
     * Retrieves information about configurable runners from the default XML file.
     *
     * @return map with builder ids and descriptions
     */
    public Map<String, RunnerDescription> getDefaultRunners() {
        try {
            return getRunnersFromStream(TexlipsePlugin.getDefault().getBundle()
                    .getEntry("defaultRunners.xml").openStream());
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        catch (XMLStreamException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves information about configurable builders from the default XML file.
     *
     * @return map with runner ids and descriptions
     */
    public Map<String, BuilderDescription> getDefaultBuilders() {
        try {
            return getBuildersFromStream(TexlipsePlugin.getDefault().getBundle()
                    .getEntry("defaultBuilders.xml").openStream());
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        catch (XMLStreamException e) {
            e.printStackTrace();
            return null;
        }
    }

}
