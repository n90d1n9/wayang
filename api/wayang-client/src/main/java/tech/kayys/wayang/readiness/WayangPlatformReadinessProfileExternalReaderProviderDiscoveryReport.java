package tech.kayys.wayang.readiness;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

/**
 * Aggregate diagnostics for readiness profile external reader provider discovery.
 *
 * <p>The report separates provider availability from configuration readiness:
 * built-in and file registries do not require external readers, while database
 * and object-storage registries must discover an available matching reader.</p>
 */
public record WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport(
        List<WayangPlatformReadinessProfileExternalReaderProviderDiagnostics> providers,
        List<String> requiredReaderTypes) {

    public WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport {
        providers = providers == null || providers.isEmpty()
                ? List.of()
                : providers.stream()
                        .filter(provider -> provider != null)
                        .toList();
        requiredReaderTypes = copyReaderTypes(requiredReaderTypes);
    }

    public static WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport empty() {
        return new WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport(List.of(), List.of());
    }

    public boolean required() {
        return !requiredReaderTypes.isEmpty();
    }

    public boolean available() {
        return availableProviderCount() > 0;
    }

    public boolean ready() {
        return missingRequiredReaderTypes().isEmpty();
    }

    public int exitCode() {
        return ready() ? 0 : 1;
    }

    public int providerCount() {
        return providers.size();
    }

    public int availableProviderCount() {
        return (int) providers.stream()
                .filter(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics::available)
                .count();
    }

    public List<String> providerIds() {
        return providers.stream()
                .map(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics::providerId)
                .toList();
    }

    public List<String> availableProviderIds() {
        return providers.stream()
                .filter(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics::available)
                .map(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics::providerId)
                .toList();
    }

    public int readerTypeCount() {
        return availableReaderTypes().size();
    }

    public List<String> availableReaderTypes() {
        LinkedHashSet<String> readerTypes = new LinkedHashSet<>();
        providers.stream()
                .filter(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics::available)
                .flatMap(provider -> provider.readerTypes().stream())
                .forEach(readerTypes::add);
        return List.copyOf(readerTypes);
    }

    public List<String> missingRequiredReaderTypes() {
        List<String> availableReaderTypes = availableReaderTypes();
        return requiredReaderTypes.stream()
                .filter(readerType -> !availableReaderTypes.contains(readerType))
                .toList();
    }

    public String message() {
        if (ready() && required()) {
            return "Readiness profile external reader discovery found required readers.";
        }
        if (ready()) {
            return "Readiness profile external reader discovery is not required by the current registry config.";
        }
        if (providerCount() == 0) {
            return "Readiness profile external reader discovery found no providers.";
        }
        return "Readiness profile external reader discovery is missing required reader types: "
                + String.join(", ", missingRequiredReaderTypes())
                + ".";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready());
        values.put("required", required());
        values.put("available", available());
        values.put("exitCode", exitCode());
        values.put("message", message());
        values.put("providerCount", providerCount());
        values.put("availableProviderCount", availableProviderCount());
        values.put("providerIds", providerIds());
        values.put("availableProviderIds", availableProviderIds());
        values.put("readerTypeCount", readerTypeCount());
        values.put("requiredReaderTypes", requiredReaderTypes);
        values.put("availableReaderTypes", availableReaderTypes());
        values.put("missingRequiredReaderTypes", missingRequiredReaderTypes());
        values.put("providers", providers.stream()
                .map(WayangPlatformReadinessProfileExternalReaderProviderDiagnostics::toMap)
                .toList());
        return SdkMaps.orderedCopy(values);
    }

    private static List<String> copyReaderTypes(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(SdkText::trimToEmpty)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }
}
