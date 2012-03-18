package net.sourceforge.texlipse.builder.factory;

import net.sourceforge.texlipse.builder.Builder;


/**
 * Descriptive information and parameters which is used by the BuilderFactory to
 * instantiate a builder. Not all fields necessarily need to be provided.
 *
 * @author Matthias Erll
 */
public class BuilderDescription {

    private final String id;
    private final int legacyId;
    private String label;
    private String outputFormat;
    private Class<? extends Builder> builderClass;
    private String runnerId;
    private String secondaryBuilderId;

    /**
     * Constructor for a new builder description.
     *
     * @param id unique id for the builder
     * @param legacyId legacy number for the builder; set to -1 for new builders
     */
    public BuilderDescription(String id, int legacyId) {
        super();
        this.id = id;
        this.legacyId = legacyId;
    }

    /**
     * Constructor. Convenience for BuilderDescription(id, -1)
     *
     * @param id unique id for the builder
     */
    public BuilderDescription(String id) {
        this(id, -1);
    }

    /**
     * Returns the builder id.
     *
     * @return unique id for the builder
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the legacy id number for the builder.
     *
     * @return the formerly unique builder number, or -1 if not assigned
     */
    public int getLegacyId() {
        return legacyId;
    }

    /**
     * Returns the builder label, which is shown in the builder configuration. Should be,
     * but does not have to be unique for functioning properly.
     *
     * @return the builder label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the builder label.
     *
     * @param label the builder label
     */
    protected void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the output format (file extension) produced by the builder.
     *
     * @return the builder output format, <code>null</code> if not set
     */
    public String getOutputFormat() {
        return outputFormat;
    }

    /**
     * Sets the output format (file extension) produced by the builder.
     *
     * @param outputFormat the builder output format
     */
    protected void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * Returns the builder class, which the BuilderFactory uses for instantiating the
     * builder. This can be used for multiple builders, if only certain parameters
     * (e.g. the builder program) differ.
     *
     * @return the builder class
     */
    public Class<? extends Builder> getBuilderClass() {
        return builderClass;
    }

    /**
     * Sets the builder class.
     *
     * @param builderClass the builder class
     */
    protected void setBuilderClass(Class<? extends Builder> builderClass) {
        this.builderClass = builderClass;
    }

    /**
     * Returns the runner id, which is used by the builder for retrieving the
     * runner instance, in order to produce output.
     * This id must be valid at the time of instantiation.
     *
     * @return the runner id
     */
    public String getRunnerId() {
        return runnerId;
    }

    /**
     * Sets the runner id.
     *
     * @param runnerId the runner id
     */
    protected void setRunnerId(String runnerId) {
        this.runnerId = runnerId;
    }

    /**
     * Returns, if applicable, the id of the secondary builder, which is used for
     * constructing a sequence of latex programs.
     * This id must be valid at the time of instantiation.
     *
     * @return the secondary builder id, <code>null</code> if not set
     */
    public String getSecondaryBuilderId() {
        return secondaryBuilderId;
    }

    /**
     * Sets the secondary builder id.
     *
     * @param secondaryBuilderId the secondary builder id
     */
    protected void setSecondaryBuilderId(String secondaryBuilderId) {
        this.secondaryBuilderId = secondaryBuilderId;
    }

}
