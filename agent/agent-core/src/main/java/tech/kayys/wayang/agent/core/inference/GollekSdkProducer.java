package tech.kayys.wayang.agent.core.inference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gollek.factory.GollekSdkFactory;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;

/**
 * CDI producer that bridges the {@link GollekSdkFactory} with Quarkus dependency injection.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>If a CDI-managed {@code GollekSdk} implementation is already available
 *       (e.g. {@code LocalGollekSdk} when {@code gollek-sdk-java-local} is on the classpath),
 *       use it directly.</li>
 *   <li>Otherwise, fall back to {@link GollekSdkFactory#create()} which uses
 *       {@link java.util.ServiceLoader} to discover the best available implementation.</li>
 * </ol>
 *
 * <p>This allows the agent module to depend on the abstract {@code gollek-sdk} facade
 * and have the concrete implementation (local or remote) resolved at deployment time
 * based on which module is on the classpath.
 */
@ApplicationScoped
public class GollekSdkProducer {

    private static final Logger log = LoggerFactory.getLogger(GollekSdkProducer.class);

    /**
     * Produces a {@link GollekSdk} instance for CDI injection.
     *
     * <p>Tries CDI-managed implementations first (e.g., {@code LocalGollekSdk});
     * falls back to ServiceLoader-based discovery via {@code GollekSdkFactory}.
     */
    @Produces
    @Singleton
    @Default
    @Alternative
    @Priority(1)
    public GollekSdk gollekSdk(jakarta.enterprise.inject.spi.BeanManager beanManager) {
        // 1. If a CDI-managed implementation exists (e.g. LocalGollekSdk @ApplicationScoped),
        //    use it directly — no factory needed.
        // We use BeanManager here instead of Instance<GollekSdk> to avoid infinite recursion
        // during bean resolution.
        java.util.Set<jakarta.enterprise.inject.spi.Bean<?>> beans = beanManager.getBeans(GollekSdk.class, 
                new jakarta.enterprise.util.AnnotationLiteral<jakarta.enterprise.inject.Any>() {});
        
        for (jakarta.enterprise.inject.spi.Bean<?> bean : beans) {
            // Skip this producer itself to avoid recursion
            if (bean.getBeanClass() == GollekSdkProducer.class) {
                continue;
            }

            // Also skip if it's another producer in this class or if we are in a test and it's not a mock
            // But generally, any other bean is preferred over the ServiceLoader fallback.
            
            log.info("Using CDI-managed GollekSdk bean: {}", bean.getBeanClass().getSimpleName());
            jakarta.enterprise.context.spi.CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
            return (GollekSdk) beanManager.getReference(bean, GollekSdk.class, ctx);
        }

        // 2. Fall back to ServiceLoader discovery
        try {
            GollekSdk sdk = GollekSdkFactory.create();
            log.info("Created GollekSdk via factory: {}", sdk.getClass().getSimpleName());
            return sdk;
        } catch (SdkException e) {
            throw new RuntimeException(
                    "Failed to create GollekSdk. Ensure gollek-sdk-java-local or " +
                    "gollek-sdk-java-remote is on the classpath.", e);
        }
    }
}
