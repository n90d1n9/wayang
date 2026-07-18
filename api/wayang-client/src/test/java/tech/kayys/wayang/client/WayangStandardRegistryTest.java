package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.alignment.WayangStandardAlignmentDescriptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangStandardRegistryTest {

    @Test
    void exposesKnownStandardsInStableOrder() {
        assertThat(WayangStandardRegistry.knownStandards())
                .extracting(WayangStandardDefinition::standardId)
                .containsExactly("a2a", "a2ui", "agentic-commerce");
    }

    @Test
    void resolvesCanonicalIdsAndAliases() {
        assertThat(WayangStandardRegistry.canonicalId("Agent2Agent Protocol")).isEqualTo("a2a");
        assertThat(WayangStandardRegistry.canonicalId("agent-to-user-interface")).isEqualTo("a2ui");
        assertThat(WayangStandardRegistry.canonicalId("agenticcommerce")).isEqualTo("agentic-commerce");
        assertThat(WayangStandardRegistry.known("unknown-standard")).isFalse();
    }

    @Test
    void exposesDefaultCatalogFacetsAndAliasLookup() {
        WayangStandardCatalog catalog = WayangStandardCatalog.defaultCatalog();

        assertThat(catalog.totalStandards()).isEqualTo(3);
        assertThat(catalog.standardIds()).containsExactly("a2a", "a2ui", "agentic-commerce");
        assertThat(catalog.versions()).containsExactly("1.0", "v0.8", "2026-01-30");
        assertThat(catalog.bindings()).containsExactly("JSONRPC", "HTTP", "HTTP+JSON");
        assertThat(catalog.bindingCounts())
                .containsEntry("JSONRPC", 1)
                .containsEntry("HTTP", 1)
                .containsEntry("HTTP+JSON", 1);
        assertThat(catalog.standard("a2ui-v0.8"))
                .map(WayangStandardDefinition::standardId)
                .contains("a2ui");
        assertThat(catalog.contains("unknown-standard")).isFalse();
    }

    @Test
    void enrichesSparseDescriptorFromRegistry() {
        WayangStandardAlignmentDescriptor descriptor = WayangStandardAlignmentDescriptor.fromReportMap(Map.of(
                "protocol", "agent2agent",
                "aligned", true));

        assertThat(descriptor.toMap())
                .containsEntry("standardId", "a2a")
                .containsEntry("name", "Agent2Agent Protocol")
                .containsEntry("version", "1.0")
                .containsEntry("binding", "JSONRPC")
                .containsEntry("specUrl", "https://a2a-protocol.org/latest/specification/");
    }

    @Test
    void keepsExplicitReportFieldsWhenEnriching() {
        WayangStandardAlignmentDescriptor descriptor = WayangStandardAlignmentDescriptor.fromReportMap(Map.of(
                "standard", Map.of(
                        "standardId", "a2ui",
                        "name", "Custom A2UI",
                        "version", "v0.8-local",
                        "binding", "SSE",
                        "specUrl", "https://example.test/a2ui"),
                "aligned", true));

        assertThat(descriptor.toMap())
                .containsEntry("standardId", "a2ui")
                .containsEntry("name", "Custom A2UI")
                .containsEntry("version", "v0.8-local")
                .containsEntry("binding", "SSE")
                .containsEntry("specUrl", "https://example.test/a2ui")
                .containsEntry("extensionUri", "https://a2ui.org/a2a-extension/a2ui/v0.8");
    }
}
