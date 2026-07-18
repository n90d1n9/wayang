package tech.kayys.wayang.code;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;

/**
 * Aggregated discovery result for coding-agent extensions.
 */
public record WayangCodeAgentExtensionDiscovery(
        List<WayangCodeAgentExtensionDiagnostics> diagnostics,
        List<WayangCodeAgentContribution> contributions) {

    public WayangCodeAgentExtensionDiscovery {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        contributions = contributions == null ? List.of() : List.copyOf(contributions);
    }

    public static WayangCodeAgentExtensionDiscovery empty() {
        return new WayangCodeAgentExtensionDiscovery(List.of(), List.of());
    }

    public int discoveredCount() {
        return diagnostics.size();
    }

    public int activeCount() {
        return (int) diagnostics.stream().filter(WayangCodeAgentExtensionDiagnostics::available).count();
    }

    public List<String> activeExtensionIds() {
        return diagnostics.stream()
                .filter(WayangCodeAgentExtensionDiagnostics::available)
                .map(WayangCodeAgentExtensionDiagnostics::extensionId)
                .toList();
    }

    public List<String> promptAdditions() {
        return contributions.stream()
                .flatMap(contribution -> contribution.systemPromptAdditions().stream())
                .toList();
    }

    public List<String> slashCommandHints() {
        return contributions.stream()
                .flatMap(contribution -> contribution.slashCommandHints().stream())
                .distinct()
                .toList();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("discoveredCount", discoveredCount());
        values.put("activeCount", activeCount());
        values.put("activeExtensionIds", activeExtensionIds());
        values.put("diagnostics", diagnostics.stream()
                .map(WayangCodeAgentExtensionDiagnostics::toMap)
                .toList());
        values.put("contributions", contributions.stream()
                .map(WayangCodeAgentContribution::toMap)
                .toList());
        return SdkMaps.orderedCopy(values);
    }
}
