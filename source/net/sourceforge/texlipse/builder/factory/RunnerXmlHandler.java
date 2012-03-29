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

import net.sourceforge.texlipse.builder.ProgramRunner;
import net.sourceforge.texlipse.builder.factory.RunnerDescription.RunnerProperty;

import org.eclipse.core.runtime.Platform;


/**
 * Handler for XML input, processing runner information.
 *
 * @author Matthias Erll
 *
 */
public class RunnerXmlHandler {

    // runner description which is currently being read
    private RunnerDescription current;

    // property field of the runner description which is currently being read
    private RunnerProperty property;

    // flag for overriding Windows-specific executable settings
    private boolean overrideExecutable;

    // map of all runners completely read
    private final Map<String, RunnerDescription> runners;
    // used for modifying executable names
    private final boolean isWindowsPlatform;

    /**
     * Processes an XML element start tag.
     *
     * @param start start start element
     * @throws XMLStreamException if the input was invalid
     */
    private void processStartElement(final StartElement start)
            throws XMLStreamException {
        final String name = start.getName().getLocalPart();
        if (RunnerDescription.RUNNER_XML_ELEMENT.equals(name)) {
            final Attribute idAttr = start.getAttributeByName(
                    new QName(RunnerDescription.RUNNER_XML_ID_ATTR));
            if (idAttr != null) {
                current = new RunnerDescription(idAttr.getValue());
            }
            else {
                throw new XMLStreamException("id attribute is mandatory!");
            }
            overrideExecutable = false;
        } else if (!RunnerDescription.RUNNER_XML_ROOT.equals(name)) {
            property = RunnerDescription.getRunnerProperty(name);
        }
    }

    /**
     * Processes an XML element end tag.
     *
     * @param end end element
     */
    private void processEndElement(final EndElement end) {
        final String name = end.getName().getLocalPart();
        if (RunnerDescription.RUNNER_XML_ELEMENT.equals(name)) {
            if (current != null) {
                runners.put(current.getId(), current);
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
    private void processCharacters(final Characters characters)
            throws XMLStreamException {
        if (current == null || property == null) {
            return;
        }

        final String str = characters.getData();
        switch (property) {
        case LABEL:
            current.setLabel(str);
            break;
        case DESCRIPTION:
            current.setDescription(str);
            break;
        case INPUTFORMAT:
            current.setInputFormat(str);
            break;
        case OUTPUTFORMAT:
            current.setOutputFormat(str);
            break;
        case RUNNERCLASS:
            try {
                Class<?> runnerClass = Class.forName(str);
                current.setRunnerClass(runnerClass.asSubclass(ProgramRunner.class));
            }
            catch (ClassNotFoundException e) {
                throw new XMLStreamException(e);
            }
            catch (ClassCastException e) {
                throw new XMLStreamException(e);
            }
            break;
        case EXECUTABLE:
            if (isWindowsPlatform) {
                if (!overrideExecutable) {
                    current.setExecutable(str.concat(".exe"));
                }
            } else {
                current.setExecutable(str);
            }
            break;
        case EXECUTABLE_WINDOWS:
            overrideExecutable = true;
            if (isWindowsPlatform) {
                current.setExecutable(str);
            }
            break;
        case DEFAULTARGUMENTS:
            current.setDefaultArguments(str);
            break;
        default:
        }
    }

    /**
     * Constructor.
     */
    public RunnerXmlHandler() {
        super();
        this.runners = new HashMap<String, RunnerDescription>();
        this.isWindowsPlatform = Platform.getOS().equals(Platform.OS_WIN32);
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
     * Returns all runners, which have been read from the XML file.
     *
     * @return map with runner ids and descriptions
     */
    public Map<String, RunnerDescription> getRunners() {
        return runners;
    }

}
