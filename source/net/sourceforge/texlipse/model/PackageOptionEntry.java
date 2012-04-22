package net.sourceforge.texlipse.model;


/**
 * Simple class for maintaining package option / value pairs.
 *
 * @author Matthias Erll
 */
public class PackageOptionEntry {

    public String option;
    public String value;

    /**
     * Creates a new option / value entry.
     */
    public PackageOptionEntry() {
        super();
    }

    /**
     * Creates a new option / value entry initialized with the given information.
     *
     * @param option option name
     * @param value value (optional, can be <code>null</code>)
     */
    public PackageOptionEntry(final String option, final String value) {
        this();
        this.option = option;
        this.value = value;
    }

}
