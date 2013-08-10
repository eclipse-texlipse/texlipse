package net.sourceforge.texlipse.builder.factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import net.sourceforge.texlipse.TexlipsePlugin;


/**
 * Reads builder information from the XML file, which can be used later by a BuilderFactory
 * for instantiating builders.
 *
 * @author Matthias Erll
 *
 */
public class BuilderXmlReader {

    private final SAXParserFactory factory;

    /**
     * Constructor.
     */
    public BuilderXmlReader() {
        super();
        factory = SAXParserFactory.newInstance();
    }

    /**
     * Retrieves information about runners from the given XML stream.
     *
     * @param in input stream to read runner information from
     * @return map with runner ids and descriptions
     * @throws IOException if the file could not be read
     * @throws SAXException if the stream could not be processed
     * @throws ParserConfigurationException if the parser config was invalid
     */
    public Map<String, RunnerDescription> getRunnersFromStream(final InputStream in)
            throws SAXException, IOException, ParserConfigurationException {
        SAXParser parser = factory.newSAXParser();
        RunnerXmlHandler handler = new RunnerXmlHandler();
        parser.parse(in, handler);
        return handler.getRunners();
    }

    /**
     * Retrieves information about builders from the given XML stream.
     *
     * @param in input stream to read builder information from
     * @return map with builder ids and descriptions
     * @throws IOException if the file could not be read
     * @throws SAXException if the stream could not be processed
     * @throws ParserConfigurationException if the parser config was invalid
     */
    public Map<String, BuilderDescription> getBuildersFromStream(final InputStream in)
            throws SAXException, IOException, ParserConfigurationException {
        SAXParser parser = factory.newSAXParser();
        BuilderXmlHandler handler = new BuilderXmlHandler();
        parser.parse(in, handler);
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
        catch (SAXException e) {
            e.printStackTrace();
            return null;
        }
        catch (ParserConfigurationException e) {
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
        catch (SAXException e) {
            e.printStackTrace();
            return null;
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }

}
