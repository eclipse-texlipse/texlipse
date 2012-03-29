package net.sourceforge.texlipse.builder.factory;

import java.io.File;

import net.sourceforge.texlipse.TexlipsePlugin;

import org.eclipse.jface.preference.IPreferenceStore;


/**
 * This class initializes, loads and stores the configuration data for a program runner,
 * which can be modified in the preferences.
 *
 * @author Matthias Erll
 *
 */
public class RunnerConfiguration {

    private final RunnerDescription runner;

    /**
     * Retrieves the old configuration key, if preferences have not yet been set based on
     * the builder id.
     *
     * @return the name of the program runner path -preference in the plugin preferences
     */
    private String getCommandPreferenceNameByClass() {
        String clazz = runner.getLegacyClass();
        if (clazz == null) {
            clazz = runner.getClass().getName();
        }
        return clazz + "_prog";
    }

    /**
     * Retrieves the old configuration key, if preferences have not yet been set based on
     * the builder id.
     *
     * @return the name of the program runner arguments -preference in the plugin preferences
     */
    private String getArgumentsPreferenceNameByClass() {
        String clazz = runner.getLegacyClass();
        if (clazz == null) {
            clazz = runner.getClass().getName();
        }
        return clazz + "_args";
    }

    /**
     * Based on the builder id, retrieves the configuration id of the runner preferences.
     *
     * @return the preferences name of the program runner path
     */
    private String getCommandPreferenceName() {
        return "runner_" + runner.getId() + "_prog";
    }

    /**
     * Based on the builder id, retrieves the configuration id of the runner preferences.
     *
     * @return the preferences name of the program runner arguments
     */
    private String getArgumentsPreferenceName() {
        return "runner_" + runner.getId() + "_args";
    }

    /**
     * Returns the absolute path of the default program file used for executing this runner.
     * This includes the given directory and the default executable. 
     *
     * @param dir directory where to look for the runner executable
     * @return the default program file path
     */
    private String getDefaultProgramFile(String dir) {
        String runnerPath = "";
        if (dir != null && dir.length() > 0) {
            File runnerFile = new File(dir + File.separator + runner.getExecutable());
            if (runnerFile.exists() && runnerFile.isFile()) {
                runnerPath = runnerFile.getAbsolutePath();
            }
        }
        return runnerPath;
    }

    /**
     * Constructor.
     *
     * @param runner description of the runner to configure
     */
    public RunnerConfiguration(RunnerDescription runner) {
        super();
        this.runner = runner;
    }

    /**
     * @return the program path and filename from the preferences
     */
    public String getProgramPath() {
        String path = TexlipsePlugin.getPreference(getCommandPreferenceName());
        if (path == null || path.length() == 0) {
            // Fall back to class-based config
            return TexlipsePlugin.getPreference(getCommandPreferenceNameByClass());
        }
        else {
            return path;
        }
    }

    /**
     * @param path the program path and filename for the preferences
     */
    public void setProgramPath(String path) {
        TexlipsePlugin.getDefault().getPreferenceStore().setValue(getCommandPreferenceName(), path);
    }

    /**
     * Sets the program path of the runner using the given directory and the default
     * executable name. If the runner does not exist under this location, nothing is
     * changed.
     *
     * @param dir directory of the program runner
     */
    public void setProgramDir(String dir) {
        String runnerPath = getDefaultProgramFile(dir);
        if (runnerPath.length() != 0) {
            setProgramPath(runnerPath);
        }
    }

    /**
     * Read the command line arguments for the program from the preferences.
     * The input filename is marked with a "%input" and the output file name
     * is marked with a "%output".
     * @return the command line arguments for the program
     */
    public String getProgramArguments() {
        String args = TexlipsePlugin.getPreference(getArgumentsPreferenceName());
        if (args == null || args.length() == 0) {
            return TexlipsePlugin.getPreference(getArgumentsPreferenceNameByClass());
        }
        else {
            return args;
        }
    }

    /**
     * @param args the program arguments for the preferences
     */
    public void setProgramArguments(String args) {
        TexlipsePlugin.getDefault().getPreferenceStore().setValue(getArgumentsPreferenceName(), args);
    }

    /**
     * Initializes the default preferences for the runner.
     *
     * @param pref preferences
     * @param dir directory for looking up the runner executable
     */
    public void initializeDefaults(IPreferenceStore pref, String dir) {
        pref.setDefault(getCommandPreferenceName(), getDefaultProgramFile(dir));
        String defaultArgs = runner.getDefaultArguments();
        if (defaultArgs == null) {
            defaultArgs = "%input";
        }
        pref.setDefault(getArgumentsPreferenceName(), defaultArgs);
    }

}
