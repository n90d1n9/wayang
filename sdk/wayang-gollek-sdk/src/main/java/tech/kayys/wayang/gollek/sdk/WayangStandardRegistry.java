package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SDK-local registry of standards that Wayang can align and report against.
 */
public final class WayangStandardRegistry {

    public static final String A2A = "a2a";
    public static final String A2UI = "a2ui";
    public static final String AGENTIC_COMMERCE = "agentic-commerce";

    private static final List<WayangStandardDefinition> DEFAULT_STANDARDS = List.of(
            new WayangStandardDefinition(
                    A2A,
                    "Agent2Agent Protocol",
                    "1.0",
                    "JSONRPC",
                    "https://a2a-protocol.org/latest/specification/",
                    List.of("agent2agent", "agent-to-agent", "agent2agent-protocol", "a2a-jsonrpc"),
                    Map.of()),
            new WayangStandardDefinition(
                    A2UI,
                    "Agent-to-User Interface",
                    "v0.8",
                    "HTTP",
                    "https://a2ui.org/specification/v0_8/standard_catalog_definition.json",
                    List.of("agent-to-user-interface", "agent-user-interface", "a2ui-v0.8"),
                    Map.of("extensionUri", "https://a2ui.org/a2a-extension/a2ui/v0.8")),
            new WayangStandardDefinition(
                    AGENTIC_COMMERCE,
                    "Agentic Commerce Protocol",
                    "2026-01-30",
                    "HTTP+JSON",
                    "https://www.agenticcommerce.dev",
                    List.of("agenticcommerce", "agentic-commerce-protocol", "agentic-commerce-dev"),
                    Map.of("specHome", "https://www.agenticcommerce.dev")));
    private static final Map<String, WayangStandardDefinition> BY_KEY = index(DEFAULT_STANDARDS);

    private WayangStandardRegistry() {
    }

    public static List<WayangStandardDefinition> knownStandards() {
        return DEFAULT_STANDARDS;
    }

    public static Optional<WayangStandardDefinition> find(String idOrAlias) {
        return Optional.ofNullable(BY_KEY.get(normalizeKey(idOrAlias)));
    }

    public static boolean known(String idOrAlias) {
        return find(idOrAlias).isPresent();
    }

    public static WayangStandardRegistryDriftReport driftReport(WayangStandardAlignmentPortfolio portfolio) {
        return WayangStandardRegistryDriftReport.from(portfolio);
    }

    public static String canonicalId(String idOrAlias) {
        String normalized = SdkText.trimToEmpty(idOrAlias);
        return find(normalized)
                .map(WayangStandardDefinition::standardId)
                .orElse(normalized);
    }

    public static WayangStandardAlignmentDescriptor enrich(WayangStandardAlignmentDescriptor descriptor) {
        if (descriptor == null) {
            return WayangStandardAlignmentDescriptor.fromReportMap(Map.of());
        }
        return find(descriptor.standardId())
                .map(definition -> enrich(descriptor, definition))
                .orElse(descriptor);
    }

    private static WayangStandardAlignmentDescriptor enrich(
            WayangStandardAlignmentDescriptor descriptor,
            WayangStandardDefinition definition) {
        Map<String, Object> attributes = new LinkedHashMap<>(definition.attributes());
        attributes.putAll(descriptor.attributes());
        return new WayangStandardAlignmentDescriptor(
                definition.standardId(),
                defaulted(descriptor.name(), descriptor.standardId(), definition.name()),
                defaulted(descriptor.version(), "", definition.version()),
                defaulted(descriptor.binding(), "", definition.binding()),
                defaulted(descriptor.specUrl(), "", definition.specUrl()),
                attributes);
    }

    private static String defaulted(String actual, String emptyValue, String fallback) {
        String normalized = SdkText.trimToEmpty(actual);
        return normalized.isEmpty() || normalized.equals(emptyValue) ? fallback : normalized;
    }

    private static Map<String, WayangStandardDefinition> index(List<WayangStandardDefinition> standards) {
        Map<String, WayangStandardDefinition> values = new LinkedHashMap<>();
        standards.forEach(definition -> {
            values.put(normalizeKey(definition.standardId()), definition);
            values.put(normalizeKey(definition.name()), definition);
            definition.aliases().forEach(alias -> values.put(normalizeKey(alias), definition));
        });
        return SdkMaps.orderedTypedCopy(values);
    }

    private static String normalizeKey(String value) {
        String normalized = SdkText.trimToEmpty(value).toLowerCase();
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.replaceAll("[^a-z0-9]+", "");
    }
}
