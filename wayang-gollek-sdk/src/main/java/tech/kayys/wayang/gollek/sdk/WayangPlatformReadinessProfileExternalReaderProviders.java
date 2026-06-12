package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Discovers optional readiness profile external reader providers from the classpath.
 */
public final class WayangPlatformReadinessProfileExternalReaderProviders {

    private WayangPlatformReadinessProfileExternalReaderProviders() {
    }

    public static WayangPlatformReadinessProfileExternalReaders discover(
            WayangGollekSdkConfig config) {
        return discover(config, Thread.currentThread().getContextClassLoader());
    }

    static WayangPlatformReadinessProfileExternalReaders discover(
            WayangGollekSdkConfig config,
            ClassLoader classLoader) {
        WayangGollekSdkConfig resolvedConfig = config == null ? WayangGollekSdkConfig.local() : config;
        WayangPlatformReadinessProfileExternalReaders discovered =
                WayangPlatformReadinessProfileExternalReaders.none();
        for (WayangPlatformReadinessProfileExternalReaderProvider provider : loadProviders(classLoader).providers()) {
            discovered = discovered.withFallbacks(readers(provider, resolvedConfig));
        }
        return discovered;
    }

    public static WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport discoveryReport(
            WayangGollekSdkConfig config) {
        return discoveryReport(config, Thread.currentThread().getContextClassLoader());
    }

    static WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport discoveryReport(
            WayangGollekSdkConfig config,
            ClassLoader classLoader) {
        WayangGollekSdkConfig resolvedConfig = config == null ? WayangGollekSdkConfig.local() : config;
        LoadedProviders loaded = loadProviders(classLoader);
        List<WayangPlatformReadinessProfileExternalReaderProviderDiagnostics> diagnostics =
                new ArrayList<>(loaded.failures());
        for (WayangPlatformReadinessProfileExternalReaderProvider provider : loaded.providers()) {
            diagnostics.add(diagnostics(provider, resolvedConfig));
        }
        return new WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport(
                diagnostics,
                requiredReaderTypes(resolvedConfig));
    }

    private static LoadedProviders loadProviders(
            ClassLoader classLoader) {
        ServiceLoader<WayangPlatformReadinessProfileExternalReaderProvider> loader =
                ServiceLoader.load(
                        WayangPlatformReadinessProfileExternalReaderProvider.class,
                        resolvedClassLoader(classLoader));
        List<WayangPlatformReadinessProfileExternalReaderProvider> providers = new ArrayList<>();
        List<WayangPlatformReadinessProfileExternalReaderProviderDiagnostics> failures = new ArrayList<>();
        Iterator<ServiceLoader.Provider<WayangPlatformReadinessProfileExternalReaderProvider>> iterator =
                loader.stream().iterator();
        while (hasNext(iterator, failures)) {
            ServiceLoader.Provider<WayangPlatformReadinessProfileExternalReaderProvider> provider;
            try {
                provider = iterator.next();
            } catch (ServiceConfigurationError error) {
                failures.add(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics.failure(
                        "unknown",
                        "",
                        100,
                        error));
                continue;
            }
            try {
                providers.add(provider.get());
            } catch (RuntimeException | ServiceConfigurationError error) {
                failures.add(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics.failure(
                        "unknown",
                        providerClass(provider),
                        100,
                        error));
            }
        }
        providers.sort(Comparator
                .comparingInt(WayangPlatformReadinessProfileExternalReaderProviders::priority)
                .thenComparing(WayangPlatformReadinessProfileExternalReaderProviders::providerId));
        return new LoadedProviders(List.copyOf(providers), List.copyOf(failures));
    }

    private static boolean hasNext(
            Iterator<ServiceLoader.Provider<WayangPlatformReadinessProfileExternalReaderProvider>> iterator,
            List<WayangPlatformReadinessProfileExternalReaderProviderDiagnostics> failures) {
        try {
            return iterator.hasNext();
        } catch (ServiceConfigurationError error) {
            failures.add(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics.failure(
                    "unknown",
                    "",
                    100,
                    error));
            return false;
        }
    }

    private static WayangPlatformReadinessProfileExternalReaders readers(
            WayangPlatformReadinessProfileExternalReaderProvider provider,
            WayangGollekSdkConfig config) {
        try {
            WayangPlatformReadinessProfileExternalReaders readers = provider.readers(config);
            return readers == null ? WayangPlatformReadinessProfileExternalReaders.none() : readers;
        } catch (RuntimeException exception) {
            return WayangPlatformReadinessProfileExternalReaders.none();
        }
    }

    private static WayangPlatformReadinessProfileExternalReaderProviderDiagnostics diagnostics(
            WayangPlatformReadinessProfileExternalReaderProvider provider,
            WayangGollekSdkConfig config) {
        String providerId = providerId(provider);
        String providerClass = provider.getClass().getName();
        int priority = priority(provider);
        try {
            WayangPlatformReadinessProfileExternalReaderProviderDiagnostics diagnostics =
                    provider.diagnostics(config);
            if (diagnostics == null) {
                return new WayangPlatformReadinessProfileExternalReaderProviderDiagnostics(
                        providerId,
                        providerClass,
                        priority,
                        false,
                        List.of(),
                        "Readiness profile external reader provider did not publish diagnostics.",
                        java.util.Map.of());
            }
            return new WayangPlatformReadinessProfileExternalReaderProviderDiagnostics(
                    SdkText.trimToDefault(diagnostics.providerId(), providerId),
                    SdkText.trimToDefault(diagnostics.providerClass(), providerClass),
                    diagnostics.priority(),
                    diagnostics.available(),
                    diagnostics.readerTypes(),
                    diagnostics.message(),
                    diagnostics.details());
        } catch (RuntimeException | ServiceConfigurationError error) {
            return WayangPlatformReadinessProfileExternalReaderProviderDiagnostics.failure(
                    providerId,
                    providerClass,
                    priority,
                    error);
        }
    }

    private static List<String> requiredReaderTypes(WayangGollekSdkConfig config) {
        WayangPlatformReadinessProfileRegistryConfig registry = config.readinessProfileRegistry();
        return switch (registry.mode()) {
            case DATABASE -> List.of("database");
            case OBJECT_STORAGE -> List.of("object_storage");
            case HYBRID -> hybridRequiredReaderTypes(registry);
            case BUILTIN, FILE -> List.of();
        };
    }

    private static List<String> hybridRequiredReaderTypes(
            WayangPlatformReadinessProfileRegistryConfig registry) {
        if (!registry.filePath().isBlank()) {
            return List.of();
        }
        if (registry.objectStorage().configured()) {
            return List.of("object_storage");
        }
        if (!registry.databaseUrl().isBlank()) {
            return List.of("database");
        }
        return List.of();
    }

    private static int priority(WayangPlatformReadinessProfileExternalReaderProvider provider) {
        try {
            return provider.priority();
        } catch (RuntimeException exception) {
            return 100;
        }
    }

    private static String providerId(WayangPlatformReadinessProfileExternalReaderProvider provider) {
        try {
            return SdkText.trimToDefault(provider.providerId(), provider.getClass().getName());
        } catch (RuntimeException exception) {
            return provider.getClass().getName();
        }
    }

    private static ClassLoader resolvedClassLoader(ClassLoader classLoader) {
        return classLoader == null
                ? WayangPlatformReadinessProfileExternalReaderProvider.class.getClassLoader()
                : classLoader;
    }

    private static String providerClass(
            ServiceLoader.Provider<WayangPlatformReadinessProfileExternalReaderProvider> provider) {
        try {
            return provider.type().getName();
        } catch (RuntimeException | ServiceConfigurationError error) {
            return "";
        }
    }

    private record LoadedProviders(
            List<WayangPlatformReadinessProfileExternalReaderProvider> providers,
            List<WayangPlatformReadinessProfileExternalReaderProviderDiagnostics> failures) {
    }
}
