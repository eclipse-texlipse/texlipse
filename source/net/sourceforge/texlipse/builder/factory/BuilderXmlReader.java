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


public class BuilderXmlReader {

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

    public BuilderXmlReader() {
        super();
    }

    public Map<String, RunnerDescription> getRunnersFromXml() {
        RunnerXmlHandler handler = new RunnerXmlHandler();
        runParser("runners.xml", handler);
        return handler.getRunners();
    }

    public Map<String, BuilderDescription> getBuildersFromXml() {
        BuilderXmlHandler handler = new BuilderXmlHandler();
        runParser("builders.xml", handler);
        return handler.getBuilders();
    }

}
