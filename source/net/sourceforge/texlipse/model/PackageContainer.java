package net.sourceforge.texlipse.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Container for tex packages used in the document.
 *
 * @author Matthias Erll
 */
public class PackageContainer {

    private final Map<String, PackageEntry> packages;

    /**
     * Creates a new package container.
     */
    public PackageContainer() {
        super();
        packages = new HashMap<String, PackageEntry>();
    }

    /**
     * Adds a package to the container, along with the given option and option
     * value. If the package has already been registered before, the option is added
     * to the existing package entry. Options can be registered more than once; however,
     * duplicates of the particular option/value combination are avoided. The value
     * is optional, and should be set to <code>null</code>, if it does not exist.
     * <p>
     * In this method, the <code>option</code> parameter cannot be <code>null</code>.
     * For adding a package entry without any options, please use
     * <code>putPackage(String)</code>.
     *
     * @param packageName package name (without path or file extension)
     * @param option option string
     * @param value value (optional, can be <code>null</code>)
     * @return <code>true</code>, if the option/value pair has been added, either to
     *  a new or existing package entry. Returns <code>false</code>, if the option
     *  or value 
     */
    public boolean addPackageOption(final String packageName, final String option,
            final String value) {
        final PackageEntry packageEntry = packages.get(packageName);
        if (packageEntry != null) {
            return packageEntry.addOptionEntry(option, value);
        }
        else {
            final PackageEntry newEntry = putPackage(packageName);
            newEntry.addOptionEntry(option, value);
            return true;
        }
    }

    /**
     * Returns whether the given package is currently registered in the container.
     *
     * @param packageName package name
     * @return <code>true</code> if the package is present, <code>false</code>
     *  otherwise
     */
    public boolean hasPackage(final String packageName) {
        return packages.containsKey(packageName);
    }

    /**
     * Adds a new package without options to the package container. If the package
     * has been present before, existing option entries are not erased.
     *
     * @param packageName package name
     * @return <code>true</code> if the package has not been registered before,
     *  <code>false</code> if the package was not new
     */
    public PackageEntry putPackage(final String packageName) {
        final PackageEntry packageEntry = packages.get(packageName);
        if (packageEntry == null) {
            final PackageEntry newEntry = new PackageEntry();
            newEntry.key = packageName;
            packages.put(packageName, newEntry);
            return newEntry;
        }
        else {
            return packageEntry;
        }
    }

    /**
     * Performs two operations on the existing package set:
     * <ul>
     * <li>Drops all packages in the container, which are not present in the set
     *  provided by the <code>packageNames</code> parameter.</li>
     * <li>Registers all packages, which were not found in the container, but in
     *  the <code>packageNames</code> set. The parameter list is emtpy.</li>
     * </ul>
     * Existing packages with parameter lists are not affected, if they are found
     * in both sets.
     *
     * @param packageNames package name
     */
    public void updatePackages(final Set<String> packageNames) {
        packages.keySet().retainAll(packageNames);
        for (String name : packageNames) {
            putPackage(name);
        }
    }

}
