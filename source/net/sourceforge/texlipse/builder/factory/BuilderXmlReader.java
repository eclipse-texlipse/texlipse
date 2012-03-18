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
     * Runs the parser for a particular file in the plugin folder, using the given
     * XML handler.
     * 
     * @param fileName file name, relative to the Texlipse plugin folder
     * @param handler XML handler
     */
    private void runParser(final String fileName, final DefaultHandler handler) {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            final InputStream inputStream = TexlipsePlugin.getDefault().getBundle()
                    .getEntry(fileName).openStream();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, handler);
        }
        catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
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
     * Retrieves information about configurable runners from the default XML file.
     *
     * @return map with builder ids and descriptions
     */
    public Map<String, RunnerDescription> getRunnersFromXml() {
        RunnerXmlHandler handler = new RunnerXmlHandler();
        runParser("runners.xml", handler);
        return handler.getRunners();
    }

    /**
     * Retrieves information about configurable builders from the default XML file.
     *
     * @return map with runner ids and descriptions
     */
    public Map<String, BuilderDescription> getBuildersFromXml() {
        BuilderXmlHandler handler = new BuilderXmlHandler();
        runParser("builders.xml", handler);
        return handler.getBuilders();
    }

}
