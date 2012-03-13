package net.sourceforge.texlipse.builder;


public class BuilderDescriptor {

    private final Class<? extends Builder> builderClass;
    private final Class<? extends Builder> secondaryBuilderClass;
    private final Class<? extends ProgramRunner> runnerClass;
    private final String outputFormat;
    private final String sequence;

    public BuilderDescriptor(final Class<? extends Builder> builderClass,
            final Class<? extends Builder> secondaryBuilder,
            final Class<? extends ProgramRunner> runnerClass) throws Exception {
        this.builderClass = builderClass;
        this.secondaryBuilderClass = secondaryBuilder;
        this.runnerClass = runnerClass;
        final Builder tempInstance = BuilderFactory.getInstance().getBuilderInstance(
                    builderClass, secondaryBuilder, runnerClass);
        this.outputFormat = tempInstance.getOutputFormat();
        this.sequence = tempInstance.getSequence();
    }

    public BuilderDescriptor(final Class<? extends Builder> builderClass,
            final Class<? extends ProgramRunner> runnerClass) throws Exception {
        this(builderClass, null, runnerClass);
    }

    public Class<? extends Builder> getBuilderClass() {
        return builderClass;
    }

    public Class<? extends Builder> getSecondaryBuilderClass() {
        return secondaryBuilderClass;
    }

    public Class<? extends ProgramRunner> getRunnerClass() {
        return runnerClass;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getSequence() {
        return sequence;
    }

}
