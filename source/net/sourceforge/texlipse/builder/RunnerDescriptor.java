package net.sourceforge.texlipse.builder;

public class RunnerDescriptor {

    private final Class<? extends ProgramRunner> runnerClass;
    private final String inputFormat;
    private final String outputFormat;
    private final String programName;
    private final String description;

    public RunnerDescriptor(final Class<? extends ProgramRunner> runnerClass)
            throws Exception {
        this.runnerClass = runnerClass;
        final ProgramRunner tempInstance = BuilderFactory.getInstance()
                .getRunnerInstance(runnerClass);
        this.inputFormat = tempInstance.getInputFormat();
        this.outputFormat = tempInstance.getOutputFormat();
        this.programName = tempInstance.getProgramName();
        this.description = tempInstance.getDescription();
    }

}
