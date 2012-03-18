package net.sourceforge.texlipse.builder.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.texlipse.builder.ProgramRunner;

import org.eclipse.core.runtime.Platform;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Handler for XML input, processing runner information.
 *
 * @author Matthias Erll
 *
 */
public class RunnerXmlHandler extends DefaultHandler {

    // runner XML field elements
    private static final String[] RUNNER_PROPERTY_STR = { "label", "description",
            "inputFormat", "outputFormat", "runnerClass", "legacyClass", "executable",
            "executable.windows", "defaultArguments" };

    // runner XML field identifiers
    private static enum RunnerProperty { LABEL, DESCRIPTION, INPUTFORMAT, OUTPUTFORMAT,
            RUNNERCLASS, LEGACYCLASS, EXECUTABLE, EXECUTABLE_WINDOWS, DEFAULTARGUMENTS };

    // runner description which is currently being read
    private RunnerDescription current;

    // property field of the runner description which is currently being read
    private RunnerProperty property;

    // flag for overriding Windows-specific executable settings
    private boolean overrideExecutable;
    // list of all output formats, used for generating an array
    private List<String> outputFormats;

    // map of all runners completely read
    private final Map<String, RunnerDescription> runners;
    // used for modifying executable names
    private final boolean isWindowsPlatform;

    /**
     * Constructor.
     */
    public RunnerXmlHandler() {
        super();
        this.runners = new HashMap<String, RunnerDescription>();
        this.isWindowsPlatform = Platform.getOS().equals(Platform.OS_WIN32);
    }

    @Override
    public void startElement(String uri, String localName,
            String qName, Attributes attributes) throws SAXException {
        if ("runner".equals(qName)) {
            final String id = attributes.getValue("id");
            if (id != null) {
                current = new RunnerDescription(id);
            }
            else {
                throw new SAXException("id attribute is mandatory!");
            }
            outputFormats = new ArrayList<String>();
            overrideExecutable = false;
        } else {
            boolean found = false;
            int i = 0;
            while (!found && i < RUNNER_PROPERTY_STR.length) {
                found = RUNNER_PROPERTY_STR[i].equals(qName);
                i++;
            }
            if (found) {
                property = RunnerProperty.values()[i - 1];
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if ("runner".equals(qName)) {
            if (current != null) {
                if (!outputFormats.isEmpty()) {
                    current.setOutputFormats(outputFormats.toArray(new String[0]));
                }
                outputFormats = null;
                runners.put(current.getId(), current);
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
        case DESCRIPTION:
            current.setDescription(str);
            break;
        case INPUTFORMAT:
            current.setInputFormat(str);
            break;
        case OUTPUTFORMAT:
            outputFormats.add(str);
            break;
        case RUNNERCLASS:
            try {
                Class<?> runnerClass = Class.forName(str);
                current.setRunnerClass(runnerClass.asSubclass(ProgramRunner.class));
            }
            catch (ClassNotFoundException e) {
                throw new SAXException(e);
            }
            catch (ClassCastException e) {
                throw new SAXException(e);
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
     * Returns all runners, which have been read from the XML file.
     *
     * @return map with runner ids and descriptions
     */
    public Map<String, RunnerDescription> getRunners() {
        return runners;
    }

}
