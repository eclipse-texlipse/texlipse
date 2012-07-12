package net.sourceforge.texlipse.model;

import java.util.ArrayList;
import java.util.List;


/**
 * Class for maintaining tex package references and their option or
 * option/value pairs.
 *
 * @author Matthias Erll
 */
public class PackageEntry extends AbstractEntry {

    private List<PackageOptionEntry> options;

    /**
     * Creates a new package entry, with an empty options list.
     */
    public PackageEntry(final String key) {
        super();
        this.key = key;
        this.options = new ArrayList<PackageOptionEntry>();
    }

    /**
     * Checks whether this package has an option registered with
     * the given name. This does not check, if the option is possibly
     * registered more than once, or has been set with values.
     *
     * @param option option name
     * @return <code>true</code> if there is any option with this name,
     *  <code>false</code> otherwise.
     */
    public boolean hasOption(final String option) {
        for (PackageOptionEntry o : options) {
            if (option.equals(o.option)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether this package has an option with the given name registered,
     * along with the given value. The value <code>null</code> is also considered
     * valid.
     *
     * @param option option name
     * @param value value
     * @return <code>true</code> if there is a matching option / value pair,
     *  <code>false</code> otherwise.
     */
    public boolean hasOptionValue(final String option, final String value) {
        for (PackageOptionEntry o : options) {
            if (option.equals(o.option) && ((value == null && o.value == null)
                    || value.equals(o.value))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the value for the given option, disregarding the fact that there
     * could be more than one value registered for this option.
     *
     * @param option option name
     * @return the (first) value associated with this option
     */
    public String getSingleOptionValue(final String option) {
        for (PackageOptionEntry o : options) {
            if (option.equals(o.option)) {
                return o.value;
            }
        }
        return null;
    }

    /**
     * Retrieves the list of option entries, with option names and values.
     *
     * @return list of option entries
     */
    public List<PackageOptionEntry> getOptionEntries() {
        return this.options;
    }

    /**
     * Adds an option to the option/value entry list. If the combination already exists,
     * no duplicate is created. However, options may be used more than once with different
     * values. The value is optional and can be <code>null</code>.
     *
     * @param option option name
     * @param value value (optional, can be <code>null</code>)
     * @return <code>true</code> if a new entry has been added, <code>false</code> if the
     *  same option/value combination was present before
     */
    public boolean addOptionEntry(final String option, final String value) {
        if (!hasOptionValue(option, value)) {
            final PackageOptionEntry entry = new PackageOptionEntry(option, value);
            options.add(entry);
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Adds an option to the option entry list. If the option is already registered with
     * a <code>null</code> value, no duplicate is created.
     *
     * @param option option name
     * @return <code>true</code> if a new entry has been added, <code>false</code> if the
     *  same option with value <code>null</code> was present before
     */
    public boolean addOption(final String option) {
        return addOptionEntry(option, null);
    }

}
