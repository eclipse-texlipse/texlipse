package net.sourceforge.texlipse.builder.factory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import net.sourceforge.texlipse.builder.Builder;
import net.sourceforge.texlipse.builder.ProgramRunner;


public class BuilderFactory {

    protected static BuilderFactory instance;

    private BuilderFactory() {
    }

    public static synchronized BuilderFactory getInstance() {
        if (instance == null) {
            instance = new BuilderFactory();
        }
        return instance;
    }

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
