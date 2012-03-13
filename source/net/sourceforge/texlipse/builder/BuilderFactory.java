package net.sourceforge.texlipse.builder;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


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

    public Builder getBuilderInstance(final BuilderDescriptor descriptor)
            throws SecurityException, IllegalArgumentException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        return getBuilderInstance(descriptor.getBuilderClass(),
                descriptor.getSecondaryBuilderClass(),
                descriptor.getRunnerClass());
    }

    public Builder getBuilderInstance(final Class<? extends Builder> builderClass,
            final Class<? extends Builder> secondaryBuilder,
            final Class<? extends ProgramRunner> runnerClass) throws SecurityException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        if (builderClass == null) {
            throw new IllegalArgumentException("The builder class cannot be null.");
        }
        final Constructor<?> constructor;
        final Object builderInstance;
        if (secondaryBuilder != null && runnerClass == null) {
            constructor = builderClass.getConstructor(
                    int.class, secondaryBuilder.getClass());
            builderInstance = constructor.newInstance(-1, secondaryBuilder);
        }
        else if (secondaryBuilder == null && runnerClass != null) {
            constructor = builderClass.getConstructor(String.class,
                    runnerClass.getClass());
            builderInstance = constructor.newInstance(runnerClass);
        }
        else {
            throw new IllegalArgumentException("No matching constructor found.");
        }
        if (builderInstance instanceof Builder) {
            return (Builder) builderInstance;
        }
        else {
            throw new IllegalArgumentException("Builder class returned an unexpected type.");
        }
    }

    public ProgramRunner getRunnerInstance(final Class<? extends ProgramRunner> runnerClass)
            throws InstantiationException, IllegalAccessException {
        return runnerClass.newInstance();
    }

}
