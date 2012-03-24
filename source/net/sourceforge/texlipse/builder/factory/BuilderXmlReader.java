package net.sourceforge.texlipse.builder.factory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Reads builder information from the XML file, which can be used later by a BuilderFactory
 * for instantiating builders.
 *
 * @author Matthias Erll
 *
 */
public class BuilderXmlReader {

    /**
     * Runs the parser for a particular file stream, using the given
     * XML handler.
     *
     * @param input stream
     * @param handler XML handler
     * @throws IOException if the stream could not be read
     */
    private void runParser(final InputStream in, final DefaultHandler handler)
            throws IOException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(in, handler);
        }
        catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Constructor.
     */
    public BuilderXmlReader() {
        super();
    }

    /**
     * Retrieves information about runners from the given XML stream.
     *
     * @param in input stream to read runner information from
     * @return map with runner ids and descriptions
     * @throws IOException if the stream could not be read
     */
    public Map<String, RunnerDescription> getRunnersFromStream(final InputStream in)
            throws IOException {
        RunnerXmlHandler handler = new RunnerXmlHandler();
        runParser(in, handler);
        return handler.getRunners();
    }

    /**
     * Retrieves information about builders from the given XML stream.
     *
     * @param in input stream to read builder information from
     * @return map with builder ids and descriptions
     * @throws IOException if the stream could not be read
     */
    public Map<String, BuilderDescription> getBuildersFromStream(final InputStream in)
            throws IOException {
        BuilderXmlHandler handler = new BuilderXmlHandler();
        runParser(in, handler);
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
    }

}
