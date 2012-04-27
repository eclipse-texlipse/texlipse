package net.sourceforge.texlipse.builder.factory;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.texlipse.builder.ProgramRunner;
import net.sourceforge.texlipse.builder.factory.RunnerDescription.RunnerProperty;

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

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        if (RunnerDescription.RUNNER_XML_ELEMENT.equals(qName)) {
            final String idAttr =
                    attributes.getValue(RunnerDescription.RUNNER_XML_ID_ATTR);
            if (idAttr != null) {
                current = new RunnerDescription(idAttr);
            }
            else {
                throw new SAXException("id attribute is mandatory!");
            }
            overrideExecutable = false;
        } else if (!RunnerDescription.RUNNER_XML_ROOT.equals(qName)) {
            property = RunnerDescription.getRunnerProperty(qName);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (RunnerDescription.RUNNER_XML_ELEMENT.equals(qName)) {
            if (current != null) {
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
            current.setOutputFormat(str);
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
        case ISCORERUNNER:
            current.setCoreRunner("true".equals(str));
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
     * Returns all runners, which have been read from the XML file.
     *
     * @return map with runner ids and descriptions
     */
    public Map<String, RunnerDescription> getRunners() {
        return runners;
    }

}
