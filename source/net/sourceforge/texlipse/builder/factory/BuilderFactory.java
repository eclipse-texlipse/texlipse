package net.sourceforge.texlipse.builder.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.sourceforge.texlipse.builder.Builder;
import net.sourceforge.texlipse.builder.ProgramRunner;


/**
 * This factory is used by the BuilderRegistry for lazy instantiation of
 * Builder subclasses and ProgramRunner subclasses, parameters
 * provided in the BuilderDescription and RunnerDescription.
 *
 * @author Matthias Erll
 */
public class BuilderFactory {

    /**
     * Singleton instance.
     */
    protected static BuilderFactory instance;

    private BuilderFactory() {
    }

    /**
     * Retrieves, and if necessary, first creates the BuilderFactory instance.
     *
     * @return instance
     */
    public static synchronized BuilderFactory getInstance() {
        if (instance == null) {
            instance = new BuilderFactory();
        }
        return instance;
    }

    /**
     * Creates a new builder instance according to the given description. The description
     * must contain a valid and accessible class to be instantiated.
     *
     * @param description the builder description, which is also passed on to the builder
     *  instance
     * @return new builder instance
     * @throws InvocationTargetException if an error occurred while using the constructor
     * @throws IllegalArgumentException if the description contains invalid parameters, or
     *  the resulting instance turns out to be invalid
     */
    public Builder getBuilderInstance(final BuilderDescription description)
                    throws InvocationTargetException, IllegalArgumentException {
        if (description == null) {
            throw new IllegalArgumentException("The builder description must be provided.");
        }
        if (description.getBuilderClass() == null) {
            throw new IllegalArgumentException("The builder class cannot be null.");
        }
        final Constructor<?> constructor;
        final Object builderInstance;
        try {
            constructor = description.getBuilderClass().getConstructor(BuilderDescription.class);
            builderInstance = constructor.newInstance(description);
            if (builderInstance instanceof Builder) {
                return (Builder) builderInstance;
            }
            else {
                throw new IllegalArgumentException("Builder class returned an unexpected type.");
            }
        }
        catch (SecurityException e) {
            return null;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No matching constructor found.");
        }
        catch (InstantiationException e) {
            return null;
        }
        catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * Creates a new runner instance according to the given description. The description
     * must contain a valid and accessible class to be instantiated.
     *
     * @param description the runner description, which is also passed on to the runner
     *  instance
     * @return new runner instance
     * @throws InvocationTargetException if an error occurred while using the constructor
     * @throws IllegalArgumentException if the description contains invalid parameters, or
     *  the resulting instance turns out to be invalid
     */
    public ProgramRunner getRunnerInstance(final RunnerDescription description)
                    throws IllegalArgumentException, InvocationTargetException {
        if (description == null) {
            throw new IllegalArgumentException("The runner description must be provided.");
        }
        if (description.getRunnerClass() == null) {
            throw new IllegalArgumentException("The runner class cannot be null.");
        }
        final Constructor<?> constructor;
        final Object runnerInstance;
        try {
            constructor = description.getRunnerClass().getConstructor(RunnerDescription.class);
            runnerInstance = constructor.newInstance(description);
            if (runnerInstance instanceof ProgramRunner) {
                return (ProgramRunner) runnerInstance;
            }
            else {
                throw new IllegalArgumentException("Runner class returned an unexpected type.");
            }
        }
        catch (SecurityException e) {
            return null;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No matching constructor found.");
        }
        catch (InstantiationException e) {
            return null;
        }
        catch (IllegalAccessException e) {
            return null;
        }
    }

}
