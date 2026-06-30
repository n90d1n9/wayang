package tech.kayys.wayang.tui.provider;

import tech.kayys.wayang.tui.config.Config;
import tech.kayys.wayang.sdk.provider.Provider;

/** Builds the right {@link Provider} implementation for a profile's configured provider type. */
public final class ProviderFactory {

    private ProviderFactory() {}

    public static Provider create(Config config, Config.Profile profile) {
        Config.ProviderConfig pc = config.provider(profile.provider);
        if (pc == null) {
            throw new IllegalArgumentException("Unknown provider '" + profile.provider +
                    "' referenced by profile '" + profile.name + "'");
        }
        return switch (pc.type) {
            case "anthropic" -> new AnthropicProvider(pc, profile.model);
            case "openai" -> new OpenAiProvider(pc, profile.model);
            case "demo" -> new DemoProvider();
            default -> throw new IllegalArgumentException("Unknown provider type: " + pc.type);
        };
    }
}
