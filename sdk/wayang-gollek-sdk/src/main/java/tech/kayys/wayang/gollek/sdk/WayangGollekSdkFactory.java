package tech.kayys.wayang.gollek.sdk;

import java.util.Comparator;
import java.util.Optional;
import java.util.ServiceLoader;

public final class WayangGollekSdkFactory {

    private WayangGollekSdkFactory() {
    }

    public static WayangGollekSdk createLocalSdk() {
        WayangGollekSdkConfig config = WayangGollekSdkConfig.local();
        return create(config);
    }

    public static WayangGollekSdk createRemoteSdk(String endpoint, String apiKey) {
        WayangGollekSdkConfig config = WayangGollekSdkConfig.remote(endpoint, apiKey);
        return create(config);
    }

    public static WayangGollekSdk create(WayangGollekSdkConfig config) {
        WayangGollekSdkConfig resolved = config == null ? WayangGollekSdkConfig.local() : config;
        if (resolved.mode() == WayangGollekSdkProvider.Mode.LOCAL) {
            return discover(resolved).orElseGet(() -> new LocalWayangGollekSdk(resolved));
        }
        return discover(resolved)
                .orElseThrow(() -> new UnsupportedOperationException(
                        "No remote Wayang SDK provider is available on the classpath."));
    }

    private static Optional<WayangGollekSdk> discover(WayangGollekSdkConfig config) {
        return ServiceLoader.load(WayangGollekSdkProvider.class).stream()
                .map(ServiceLoader.Provider::get)
                .filter(provider -> provider.mode() == config.mode())
                .min(Comparator.comparingInt(WayangGollekSdkProvider::priority))
                .map(provider -> provider.create(config));
    }
}
