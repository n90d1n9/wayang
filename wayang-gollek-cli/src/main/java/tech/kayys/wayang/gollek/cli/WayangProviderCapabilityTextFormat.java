package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityDescriptor;
import tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityDiscovery;
import tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityEnvelopes;
import tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Text renderer for provider capability discovery and detail responses shown by the Wayang CLI.
 */
final class WayangProviderCapabilityTextFormat {

    private WayangProviderCapabilityTextFormat() {
    }

    static String text(String productName, WayangProviderCapabilityDiscovery discovery) {
        WayangProviderCapabilityDiscovery model = WayangProviderCapabilityEnvelopes.normalize(discovery);
        StringBuilder output = new StringBuilder("Wayang provider capabilities\n");
        output.append("product: ").append(productName).append('\n');
        output.append("totalCapabilities: ").append(model.totalCapabilities()).append('\n');
        output.append("matchingCapabilities: ").append(model.matchingCapabilities()).append('\n');
        appendQuery(output, model.query(), model.search());
        for (Map.Entry<String, List<WayangProviderCapabilityDescriptor>> entry : groupedByModule(model.capabilities()).entrySet()) {
            output.append('\n').append(entry.getKey()).append('\n');
            for (WayangProviderCapabilityDescriptor capability : entry.getValue()) {
                output.append("  - ")
                        .append(capability.id())
                        .append(": ")
                        .append(capability.name())
                        .append(" [")
                        .append(capability.state().id())
                        .append("]")
                        .append('\n');
                if (!capability.description().isBlank()) {
                    output.append("    ").append(capability.description()).append('\n');
                }
                output.append("    provider: ").append(capability.providerId()).append('\n');
                output.append("    type: ").append(capability.capabilityType()).append('\n');
                CliText.appendIndentedListLine(output, "surfaces", capability.surfaceIds());
                CliText.appendIndentedListLine(output, "standards", capability.standardIds());
                CliText.appendIndentedListLine(output, "tags", capability.tags());
            }
        }
        return output.append('\n').toString();
    }

    static String detailText(String productName, WayangProviderCapabilityDescriptor capability) {
        StringBuilder output = new StringBuilder("Wayang provider capability\n");
        output.append("product: ").append(productName).append('\n');
        output.append("id: ").append(capability.id()).append('\n');
        output.append("name: ").append(capability.name()).append('\n');
        output.append("state: ").append(capability.state().id()).append('\n');
        output.append("provider: ").append(capability.providerId()).append('\n');
        output.append("namespace: ").append(capability.providerNamespace()).append('\n');
        output.append("module: ").append(capability.moduleId()).append('\n');
        output.append("type: ").append(capability.capabilityType()).append('\n');
        if (!capability.description().isBlank()) {
            output.append("description: ").append(capability.description()).append('\n');
        }
        CliText.appendListLine(output, "surfaces", capability.surfaceIds());
        CliText.appendListLine(output, "standards", capability.standardIds());
        CliText.appendListLine(output, "tags", capability.tags());
        if (!capability.metadata().isEmpty()) {
            output.append("metadata: ");
            output.append(CliText.inlineKeyValueMap(capability.metadata()));
            output.append('\n');
        }
        return output.append('\n').toString();
    }

    private static void appendQuery(StringBuilder output, WayangProviderCapabilityQuery query, String search) {
        if (query.hasCapabilityId()) {
            output.append("capability: ").append(query.capabilityId()).append('\n');
        }
        if (query.hasProviderId()) {
            output.append("provider: ").append(query.providerId()).append('\n');
        }
        if (query.hasProviderNamespace()) {
            output.append("namespace: ").append(query.providerNamespace()).append('\n');
        }
        if (query.hasModuleId()) {
            output.append("module: ").append(query.moduleId()).append('\n');
        }
        if (query.hasCapabilityType()) {
            output.append("type: ").append(query.capabilityType()).append('\n');
        }
        if (query.state() != null) {
            output.append("state: ").append(query.state().name().toLowerCase(Locale.ROOT)).append('\n');
        }
        if (query.hasSurfaceId()) {
            output.append("surface: ").append(query.surfaceId()).append('\n');
        }
        if (query.hasStandardId()) {
            output.append("standard: ").append(query.standardId()).append('\n');
        }
        if (query.hasTag()) {
            output.append("tag: ").append(query.tag()).append('\n');
        }
        String normalizedSearch = CliText.blankToNull(search);
        if (normalizedSearch != null) {
            output.append("search: ").append(normalizedSearch).append('\n');
        }
    }

    private static Map<String, List<WayangProviderCapabilityDescriptor>> groupedByModule(
            List<WayangProviderCapabilityDescriptor> capabilities) {
        Map<String, List<WayangProviderCapabilityDescriptor>> grouped = new LinkedHashMap<>();
        for (WayangProviderCapabilityDescriptor capability
                : capabilities == null ? List.<WayangProviderCapabilityDescriptor>of() : capabilities) {
            grouped.computeIfAbsent(capability.moduleId(), ignored -> new ArrayList<>()).add(capability);
        }
        return grouped;
    }
}
