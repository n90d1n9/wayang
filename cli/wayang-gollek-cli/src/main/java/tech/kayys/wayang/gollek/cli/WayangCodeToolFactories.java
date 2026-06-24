package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.sdk.gollek.tools.ToolsFactory;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Discovers optional coding-agent tool factories and supplies the OSS fallback.
 */
final class WayangCodeToolFactories {

    private WayangCodeToolFactories() {
    }

    static Optional<ToolsFactory> discover() {
        try {
            for (ToolsFactory toolsFactory : ServiceLoader.load(ToolsFactory.class)) {
                if (toolsFactory != null) {
                    return Optional.of(toolsFactory);
                }
            }
        } catch (RuntimeException | ServiceConfigurationError ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    static ToolsFactory fallback() {
        return WayangCodeFallbackToolsFactory.create();
    }
}
