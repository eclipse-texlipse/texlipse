package net.sourceforge.texlipse.builder.factory;

import net.sourceforge.texlipse.builder.ProgramRunner;

public class RunnerDescription {

    private final String id;
    private final int legacyId;
    private String label;
    private String description;
    private String inputFormat;
    private String[] outputFormats;
    private Class<? extends ProgramRunner> runnerClass;
    private String executable;
    private String defaultArguments;

    public RunnerDescription(String id, int legacyId) {
        super();
        this.id = id;
        this.legacyId = legacyId;
    }

    public RunnerDescription(String id) {
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

    public String getDescription() {
        return description;
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    public String getInputFormat() {
        return inputFormat;
    }

    protected void setInputFormat(String inputFormat) {
        this.inputFormat = inputFormat;
    }

    public String[] getOutputFormats() {
        return outputFormats;
    }

    protected void setOutputFormats(String[] outputFormats) {
        this.outputFormats = outputFormats;
    }

    public boolean hasOutputFormat(String outputFormat) {
        for (String out : outputFormats) {
            if (out.equals(outputFormat)) {
                return true;
            }
        }
        return false;
    }

    public String getDefaultOutputFormat() {
        if (outputFormats != null && outputFormats.length > 0) {
            return outputFormats[0];
        }
        else {
            return null;
        }
    }

    public Class<? extends ProgramRunner> getRunnerClass() {
        return runnerClass;
    }

    protected void setRunnerClass(Class<? extends ProgramRunner> runnerClass) {
        this.runnerClass = runnerClass;
    }

    public String getExecutable() {
        return executable;
    }

    protected void setExecutable(String executable) {
        this.executable = executable;
    }

    public String getDefaultArguments() {
        return defaultArguments;
    }

    protected void setDefaultArguments(String defaultArguments) {
        this.defaultArguments = defaultArguments;
    }

}
