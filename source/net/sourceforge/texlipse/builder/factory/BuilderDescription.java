package net.sourceforge.texlipse.builder.factory;

import net.sourceforge.texlipse.builder.Builder;


public class BuilderDescription {

    private final String id;
    private final int legacyId;
    private String label;
    private String outputFormat;
    private Class<? extends Builder> builderClass;
    private String runnerId;
    private String secondaryBuilderId;

    public BuilderDescription(String id, int legacyId) {
        super();
        this.id = id;
        this.legacyId = legacyId;
    }

    public BuilderDescription(String id) {
        this(id, -1);
    }

    public String getId() {
        return id;
    }

    public int getLegacyId() {
        return legacyId;
    }

    public String getLabel() {
        return label;
    }

    protected void setLabel(String label) {
        this.label = label;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    protected void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public Class<? extends Builder> getBuilderClass() {
        return builderClass;
    }

    protected void setBuilderClass(Class<? extends Builder> builderClass) {
        this.builderClass = builderClass;
    }

    public String getRunnerId() {
        return runnerId;
    }

    protected void setRunnerId(String runnerId) {
        this.runnerId = runnerId;
    }

    public String getSecondaryBuilderId() {
        return secondaryBuilderId;
    }

    protected void setSecondaryBuilderId(String secondaryBuilderId) {
        this.secondaryBuilderId = secondaryBuilderId;
    }

}
