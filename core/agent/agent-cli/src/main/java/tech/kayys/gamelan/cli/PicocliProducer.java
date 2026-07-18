package tech.kayys.gamelan.cli;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * CDI producer for picocli {@link IFactory}.
 * 
 * <p>This provides a default picocli factory that can be injected
 * into {@link GamelanApplication} and other CLI components.
 */
@ApplicationScoped
public class PicocliProducer {

    @Produces
    @ApplicationScoped
    public IFactory produceFactory() {
        return new SimpleFactory();
    }

    /**
     * Simple factory implementation for picocli that uses reflection to create instances.
     */
    private static class SimpleFactory implements IFactory {
        @Override
        public <K> K create(Class<K> clazz) throws InvocationTargetException {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new InvocationTargetException(e, "Cannot create " + clazz.getName());
            }
        }
    }
}
