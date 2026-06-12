package tech.kayys.wayang.gollek.sdk.storage;

import tech.kayys.wayang.gollek.sdk.WayangSecretRedactor;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Aggregate report for readiness profile object-storage service discovery.
 */
public record WayangReadinessProfileObjectStorageServiceDiscoveryReport(
        List<WayangReadinessProfileObjectStorageServiceProviderDiagnostics> providers) {

    public WayangReadinessProfileObjectStorageServiceDiscoveryReport {
        providers = providers == null || providers.isEmpty()
                ? List.of()
                : providers.stream()
                        .filter(provider -> provider != null)
                        .toList();
    }

    public static WayangReadinessProfileObjectStorageServiceDiscoveryReport of(
            List<WayangReadinessProfileObjectStorageServiceProviderDiagnostics> providers) {
        return new WayangReadinessProfileObjectStorageServiceDiscoveryReport(providers);
    }

    public boolean available() {
        return availableProviderCount() > 0 && serviceCount() > 0;
    }

    public int exitCode() {
        return available() ? 0 : 1;
    }

    public int providerCount() {
        return providers.size();
    }

    public int availableProviderCount() {
        return (int) providers.stream()
                .filter(WayangReadinessProfileObjectStorageServiceProviderDiagnostics::available)
                .count();
    }

    public int serviceCount() {
        return serviceIds().size();
    }

    public List<String> serviceIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        providers.stream()
                .filter(WayangReadinessProfileObjectStorageServiceProviderDiagnostics::available)
                .flatMap(provider -> provider.serviceIds().stream())
                .forEach(ids::add);
        return List.copyOf(ids);
    }

    public String message() {
        if (available()) {
            return "Readiness profile object-storage service discovery found available services.";
        }
        if (providerCount() == 0) {
            return "Readiness profile object-storage service discovery found no providers.";
        }
        return "Readiness profile object-storage service discovery found no available services.";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("available", available());
        values.put("exitCode", exitCode());
        values.put("message", message());
        values.put("providerCount", providerCount());
        values.put("availableProviderCount", availableProviderCount());
        values.put("serviceCount", serviceCount());
        values.put("serviceIds", serviceIds().stream()
                .map(WayangReadinessProfileObjectStorageServiceDiscoveryReport::redact)
                .toList());
        values.put("providers", providers.stream()
                .map(WayangReadinessProfileObjectStorageServiceProviderDiagnostics::toMap)
                .toList());
        return java.util.Collections.unmodifiableMap(values);
    }

    private static String redact(String value) {
        return WayangSecretRedactor.connectionString(value);
    }
}
