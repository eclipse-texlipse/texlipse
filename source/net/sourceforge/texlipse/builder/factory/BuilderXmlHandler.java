package net.sourceforge.texlipse.builder.factory;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.texlipse.builder.Builder;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Handler for XML input, processing builder information.
 *
 * @author Matthias Erll
 *
 */
public class BuilderXmlHandler extends DefaultHandler {

    // builder XML field elements
    private static final String[] BUILDER_PROPERTY_STR = { "label", "outputFormat",
            "builderClass", "runnerId", "secondaryBuilder" };
    // builder XML field identifiers
    private static enum BuilderProperty { LABEL, OUTPUTFORMAT, BUILDERCLASS, RUNNERID,
            SECONDARYBUILDER };

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
    public void startElement(String uri, String localName,
            String qName, Attributes attributes) throws SAXException {
        if ("builder".equals(qName)) {
            final String id = attributes.getValue("id");
            if (id != null) {
                final String legacyId = attributes.getValue("legacyId");
                if (legacyId != null) {
                    int lid;
                    try {
                        lid = Integer.valueOf(legacyId);
                    }
                    catch (NumberFormatException e) {
                        lid = -1;
                    }
                    current = new BuilderDescription(id, lid);
                } else {
                    current = new BuilderDescription(id);
                }
            }
            else {
                throw new SAXException("id attribute is obligatory!");
            }
        } else {
            boolean found = false;
            int i = 0;
            while (!found && i < BUILDER_PROPERTY_STR.length) {
                found = BUILDER_PROPERTY_STR[i].equals(qName);
                i++;
            }
            if (found) {
                property = BuilderProperty.values()[i - 1];
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if ("builder".equals(qName)) {
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
