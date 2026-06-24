package tech.kayys.wayang.gollek.extension;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class WayangGollekExtensionProducer {

    @Produces
    @ApplicationScoped
    public GollekIntegrationService gollekIntegrationService(
            @ConfigProperty(name = "wayang.gollek.enabled", defaultValue = "false") boolean enabled) {
        if (enabled) {
            System.out.println("[WayangGollekExtension] Gollek integration enabled at runtime");
            return new RealGollekIntegrationService();
        }
        System.out.println("[WayangGollekExtension] Gollek integration disabled; using Noop adapters");
        return new NoopGollekIntegrationService();
    }
}

interface GollekIntegrationService {
    String name();
    default boolean enabled() { return false; }
}

class RealGollekIntegrationService implements GollekIntegrationService {
    @Override
    public String name() { return "wayang-gollek-extension-runtime"; }
    @Override
    public boolean enabled() { return true; }
}

class NoopGollekIntegrationService implements GollekIntegrationService {
    @Override
    public String name() { return "noop-wayang-gollek-extension"; }
}
