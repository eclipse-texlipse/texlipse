package net.sourceforge.texlipse.builder.cache;

import org.eclipse.core.runtime.IPath;


/**
 * Object for storing cached file information.
 * <p>
 * Also contains constants for the XML persistence.
 *
 * @author Matthias Erll
 *
 */
public class ProjectFileInfo {

    public static final String FILE_XML_ROOT = "files";
    public static final String FILE_XML_ELEMENT = "file";
    public static final String FILE_XML_NAME_ATTR = "name";
    public static final String[] FILE_PROPERTY_STR = { "modification", "hashValue" };

    public static enum FileProperty { MODSTAMP, HASHVALUE }

    private final IPath name;
    private Long modStamp;
    private byte[] hashValue;

    /**
     * Retrieves the FileProperty object for the given XML tag name.
     *
     * @param propertyStr XML tag name
     * @return FileProperty, or <code>null</code> if none matches
     */
    public static FileProperty getFileProperty(final String propertyStr) {
        for (int i = 0; i < FILE_PROPERTY_STR.length; i++) {
            if (FILE_PROPERTY_STR[i].equals(propertyStr)) {
                return FileProperty.values()[i];
            }
        }
        return null;
    }

    /**
     * Retrieves the XML element tag name for the given FileProperty object.
     *
     * @param property FileProperty
     * @return XML tag name
     */
    public static String getXmlPropertyStr(final FileProperty property) {
        return FILE_PROPERTY_STR[property.ordinal()];
    }

    /**
     * Creates a new cache file object for storing information.
     *
     * @param name project relative file path
     */
    public ProjectFileInfo(final IPath name) {
        super();
        this.name = name;
    }

    /**
     * Retrieves the cached modification stamp of the file.
     *
     * @return time stamp
     */
    public Long getModificationStamp() {
        return modStamp;
    }

    /**
     * Stores the modification stamp of the file.
     *
     * @param modStamp time stamp
     */
    public void setModificationStamp(Long modStamp) {
        this.modStamp = modStamp;
    }

    /**
     * Retrieves the cached hash value of the file.
     *
     * @return hash value byte array
     */
    public byte[] getHashValue() {
        return hashValue;
    }

    /**
     * Stores the hash value of the file.
     *
     * @param hashValue hash value byte array
     */
    public void setHashValue(byte[] hashValue) {
        this.hashValue = hashValue;
    }

    /**
     * Retrieves the project relative file path.
     *
     * @return the path
     */
    public IPath getName() {
        return name;
    }

}
