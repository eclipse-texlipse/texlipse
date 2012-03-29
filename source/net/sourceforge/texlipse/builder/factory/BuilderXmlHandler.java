package net.sourceforge.texlipse.builder.factory;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import net.sourceforge.texlipse.builder.Builder;
import net.sourceforge.texlipse.builder.factory.BuilderDescription.BuilderProperty;


/**
 * Handler for XML input, processing builder information.
 *
 * @author Matthias Erll
 *
 */
public class BuilderXmlHandler {

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

    /**
     * Processes an XML element start tag.
     *
     * @param start start element
     * @throws XMLStreamException if the input was invalid
     */
    public void processStartElement(final StartElement start)
            throws XMLStreamException {
        final String name = start.getName().getLocalPart();
        if (BuilderDescription.BUILDER_XML_ELEMENT.equals(name)) {
            final Attribute idAttr = start.getAttributeByName(
                    new QName(BuilderDescription.BUILDER_XML_ID_ATTR));
            if (idAttr != null) {
                final Attribute legacyIdAttr = start.getAttributeByName(
                        new QName(BuilderDescription.BUILDER_XML_LEG_ID_ATTR));
                if (legacyIdAttr != null) {
                    int lid;
                    try {
                        lid = Integer.valueOf(legacyIdAttr.getValue());
                    }
                    catch (NumberFormatException e) {
                        lid = -1;
                    }
                    current = new BuilderDescription(idAttr.getValue(), lid);
                } else {
                    current = new BuilderDescription(idAttr.getValue());
                }
            }
            else {
                throw new XMLStreamException("id attribute is obligatory!");
            }
        } else if (!BuilderDescription.BUILDER_XML_ROOT.equals(name)) {
            property = BuilderDescription.getBuilderProperty(name);
        }
    }

    /**
     * Processes an XML element end tag.
     *
     * @param end end element
     */
    public void processEndElement(final EndElement end) {
        final String name = end.getName().getLocalPart();
        if (BuilderDescription.BUILDER_XML_ELEMENT.equals(name)) {
            if (current != null) {
                builders.put(current.getId(), current);
                current = null;
            }
        } else {
            property = null;
        }
    }

    /**
     * Processes characters found in the XML stream and transfers
     * them to the current object.
     *
     * @param characters character object
     * @throws XMLStreamException if the contents of the stream are invalid
     */
    public void processCharacters(final Characters characters)
            throws XMLStreamException {
        if (current == null || property == null) {
            return;
        }

        final String str = characters.getData();
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
                throw new XMLStreamException(e);
            }
            catch (ClassCastException e) {
                throw new XMLStreamException(e);
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
     * Reads the XML file using the given parser.
     *
     * @param parser XML parser
     * @throws XMLStreamException if the stream contains invalid elements or could
     *  not be read
     */
    public void readXmlFile(XMLEventReader parser) throws XMLStreamException {
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

    /**
     * Returns all builders, which have been read from the XML file.
     *
     * @return map with builder ids and descriptions
     */
    public Map<String, BuilderDescription> getBuilders() {
        return builders;
    }

}
