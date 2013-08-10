package net.sourceforge.texlipse.builder.factory;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import net.sourceforge.texlipse.builder.Builder;
import net.sourceforge.texlipse.builder.factory.BuilderDescription.BuilderProperty;


/**
 * Handler for XML input, processing builder information.
 *
 * @author Matthias Erll
 *
 */
public class BuilderXmlHandler extends DefaultHandler {

    // builder description which is currently being read
    private BuilderDescription current;

    // property field of the builder description which is currently being read
    private BuilderProperty property;

    // map of all builders completely read
    private final Map<String, BuilderDescription> builders;

    /**
     * Constructor.
     */
    public BuilderXmlHandler() {
        super();
        this.builders = new HashMap<String, BuilderDescription>();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if (BuilderDescription.BUILDER_XML_ELEMENT.equals(qName)) {
            final String idAttr =
                    attributes.getValue(BuilderDescription.BUILDER_XML_ID_ATTR);
            if (idAttr != null) {
                final String legacyIdAttr =
                        attributes.getValue(BuilderDescription.BUILDER_XML_LEG_ID_ATTR);
                if (legacyIdAttr != null) {
                    int lid;
                    try {
                        lid = Integer.valueOf(legacyIdAttr);
                    }
                    catch (NumberFormatException e) {
                        lid = -1;
                    }
                    current = new BuilderDescription(idAttr, lid);
                } else {
                    current = new BuilderDescription(idAttr);
                }
            }
            else {
                throw new SAXException("id attribute is obligatory!");
            }
        } else if (!BuilderDescription.BUILDER_XML_ROOT.equals(qName)) {
            property = BuilderDescription.getBuilderProperty(qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (BuilderDescription.BUILDER_XML_ELEMENT.equals(qName)) {
            if (current != null) {
                builders.put(current.getId(), current);
                current = null;
            }
        } else {
            property = null;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (current == null || property == null) {
            return;
        }

        final String str = new String(ch, start, length);
        switch (property) {
        case LABEL:
            current.setLabel(str);
            break;
        case OUTPUTFORMAT:
            current.setOutputFormat(str);
            break;
        case BUILDERCLASS:
            try {
                Class<?> builderClass = Class.forName(str);
                current.setBuilderClass(builderClass.asSubclass(Builder.class));
            }
            catch (ClassNotFoundException e) {
                throw new SAXException(e);
            }
            catch (ClassCastException e) {
                throw new SAXException(e);
            }
            break;
        case RUNNERID:
            current.setRunnerId(str);
            break;
        case SECONDARYBUILDER:
            current.setSecondaryBuilderId(str);
            break;
        default:
        }
    }

    /**
     * Returns all builders, which have been read from the XML file.
     *
     * @return map with builder ids and descriptions
     */
    public Map<String, BuilderDescription> getBuilders() {
        return builders;
    }

}
