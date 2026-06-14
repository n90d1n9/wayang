package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Discovers initialized object-storage services for readiness profile reads.
 */
public final class WayangReadinessProfileObjectStorageServiceProviders {

    private WayangReadinessProfileObjectStorageServiceProviders() {
    }

    public static WayangReadinessProfileObjectStorageServiceRegistry registry(
            WayangGollekSdkConfig config) {
        return registry(config, Thread.currentThread().getContextClassLoader());
    }

    public static List<WayangReadinessProfileObjectStorageServiceProviderDiagnostics> diagnostics(
            WayangGollekSdkConfig config) {
        return diagnostics(config, Thread.currentThread().getContextClassLoader());
    }

    public static WayangReadinessProfileObjectStorageServiceDiscoveryReport discoveryReport(
            WayangGollekSdkConfig config) {
        return discoveryReport(config, Thread.currentThread().getContextClassLoader());
    }

    static WayangReadinessProfileObjectStorageServiceRegistry registry(
            WayangGollekSdkConfig config,
            ClassLoader classLoader) {
        Map<String, ObjectStorageService> services = new LinkedHashMap<>();
        for (WayangReadinessProfileObjectStorageServiceProvider provider : providers(classLoader)) {
            for (WayangReadinessProfileObjectStorageServiceRegistration registration
                    : registrations(provider, config)) {
                services.putIfAbsent(registration.serviceId(), registration.service());
            }
        }
        WayangReadinessProfileObjectStorageServiceRegistry.Builder builder =
                WayangReadinessProfileObjectStorageServiceRegistry.builder();
        services.forEach(builder::register);
        return builder.build();
    }

    static List<WayangReadinessProfileObjectStorageServiceProviderDiagnostics> diagnostics(
            WayangGollekSdkConfig config,
            ClassLoader classLoader) {
        return providers(classLoader).stream()
                .map(provider -> diagnostic(provider, config))
                .toList();
    }

    static WayangReadinessProfileObjectStorageServiceDiscoveryReport discoveryReport(
            WayangGollekSdkConfig config,
            ClassLoader classLoader) {
        return WayangReadinessProfileObjectStorageServiceDiscoveryReport.of(
                diagnostics(config, classLoader));
    }

    private static List<WayangReadinessProfileObjectStorageServiceProvider> providers(
            ClassLoader classLoader) {
        ServiceLoader<WayangReadinessProfileObjectStorageServiceProvider> loader =
                ServiceLoader.load(
                        WayangReadinessProfileObjectStorageServiceProvider.class,
                        resolvedClassLoader(classLoader));
        List<WayangReadinessProfileObjectStorageServiceProvider> providers = new ArrayList<>();
        Iterator<ServiceLoader.Provider<WayangReadinessProfileObjectStorageServiceProvider>> iterator =
                loader.stream().iterator();
        while (hasNext(iterator)) {
            ServiceLoader.Provider<WayangReadinessProfileObjectStorageServiceProvider> provider;
            try {
                provider = iterator.next();
            } catch (ServiceConfigurationError error) {
                continue;
            }
            try {
                providers.add(provider.get());
            } catch (RuntimeException | ServiceConfigurationError ignored) {
            }
        }
        providers.sort(Comparator
                .comparingInt(WayangReadinessProfileObjectStorageServiceProviders::priority)
                .thenComparing(WayangReadinessProfileObjectStorageServiceProviders::providerId));
        return providers;
    }

    private static boolean hasNext(
            Iterator<ServiceLoader.Provider<WayangReadinessProfileObjectStorageServiceProvider>> iterator) {
        try {
            return iterator.hasNext();
        } catch (ServiceConfigurationError error) {
            return false;
        }
    }

    private static List<WayangReadinessProfileObjectStorageServiceRegistration> registrations(
            WayangReadinessProfileObjectStorageServiceProvider provider,
            WayangGollekSdkConfig config) {
        try {
            List<WayangReadinessProfileObjectStorageServiceRegistration> registrations =
                    provider.services(config);
            return registrations == null || registrations.isEmpty()
                    ? List.of()
                    : registrations.stream()
                            .filter(registration -> registration != null)
                            .toList();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    private static WayangReadinessProfileObjectStorageServiceProviderDiagnostics diagnostic(
            WayangReadinessProfileObjectStorageServiceProvider provider,
            WayangGollekSdkConfig config) {
        String providerId = providerId(provider);
        int priority = priority(provider);
        try {
            Map<String, Object> details = provider.diagnostics(config);
            details = details == null ? Map.of() : details;
            boolean available = available(details, serviceIds(details));
            List<String> serviceIds = available || !details.containsKey("available")
                    ? serviceIds(details)
                    : List.of();
            return new WayangReadinessProfileObjectStorageServiceProviderDiagnostics(
                    providerId,
                    priority,
                    available,
                    serviceIds,
                    details,
                    text(details.get("message"), "Object-storage service provider diagnostics are available."));
        } catch (RuntimeException exception) {
            return new WayangReadinessProfileObjectStorageServiceProviderDiagnostics(
                    providerId,
                    priority,
                    false,
                    List.of(),
                    Map.of(),
                    "Object-storage service provider diagnostics failed: " + exception.getMessage());
        }
    }

    private static List<String> serviceIds(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return List.of();
        }
        Object serviceIds = details.get("serviceIds");
        if (serviceIds instanceof Iterable<?> values) {
            List<String> resolved = new ArrayList<>();
            for (Object value : values) {
                String text = text(value, "");
                if (!text.isBlank()) {
                    resolved.add(text);
                }
            }
            return resolved;
        }
        String serviceId = text(details.get("serviceId"), "");
        return serviceId.isBlank() ? List.of() : List.of(serviceId);
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private static boolean available(Map<String, Object> details, List<String> serviceIds) {
        if (details.containsKey("available")) {
            return bool(details.get("available"));
        }
        return serviceIds != null && !serviceIds.isEmpty();
    }

    private static String text(Object value, String defaultValue) {
        return value == null ? defaultValue : trimToDefault(String.valueOf(value), defaultValue);
    }

    private static int priority(WayangReadinessProfileObjectStorageServiceProvider provider) {
        try {
            return provider.priority();
        } catch (RuntimeException exception) {
            return 100;
        }
    }

    private static String providerId(WayangReadinessProfileObjectStorageServiceProvider provider) {
        try {
            return trimToDefault(provider.providerId(), provider.getClass().getName());
        } catch (RuntimeException exception) {
            return provider.getClass().getName();
        }
    }

    private static ClassLoader resolvedClassLoader(ClassLoader classLoader) {
        return classLoader == null
                ? WayangReadinessProfileObjectStorageServiceProvider.class.getClassLoader()
                : classLoader;
    }

    private static String trimToDefault(String value, String defaultValue) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isBlank() ? defaultValue : trimmed;
    }
}
