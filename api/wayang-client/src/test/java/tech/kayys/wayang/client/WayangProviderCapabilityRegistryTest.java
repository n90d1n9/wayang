package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangProviderCapabilityRegistryTest {

    @Test
    void normalizesDescriptorMetadataForDiscoveryAndJsonPayloads() {
        WayangProviderCapabilityDescriptor descriptor = new WayangProviderCapabilityDescriptor(
                " A2UI.Contracts ",
                " WAYANG-A2UI ",
                " WAYANG ",
                " A2UI ",
                " Standard ",
                " A2UI Contracts ",
                " UI contract bridge ",
                null,
                List.of(" ASSISTANT-Agent ", "assistant-agent", ""),
                List.of(" Agent-to-User Interface ", "A2UI"),
                List.of(" UI-Contracts ", "ui-contracts", ""),
                Map.of(" endpoint ", " /a2ui ", "blank", " "));

        assertThat(descriptor.id()).isEqualTo("a2ui.contracts");
        assertThat(descriptor.providerId()).isEqualTo("wayang-a2ui");
        assertThat(descriptor.providerNamespace()).isEqualTo("wayang");
        assertThat(descriptor.moduleId()).isEqualTo("a2ui");
        assertThat(descriptor.capabilityType()).isEqualTo("standard");
        assertThat(descriptor.name()).isEqualTo("A2UI Contracts");
        assertThat(descriptor.state()).isEqualTo(WayangProviderCapabilityState.AVAILABLE);
        assertThat(descriptor.surfaceIds()).containsExactly("assistant-agent");
        assertThat(descriptor.standardIds()).containsExactly(WayangStandardRegistry.A2UI);
        assertThat(descriptor.tags()).containsExactly("ui-contracts");
        assertThat(descriptor.metadata()).containsExactly(Map.entry("endpoint", "/a2ui"));
        assertThat(descriptor.supportsSurface(" ASSISTANT-Agent ")).isTrue();
        assertThat(descriptor.supportsStandard(" a2ui-v0.8 ")).isTrue();
        assertThat(descriptor.hasTag(" UI-Contracts ")).isTrue();

        assertThat(descriptor.toMap())
                .containsEntry("id", "a2ui.contracts")
                .containsEntry("state", "available")
                .containsEntry("available", true)
                .containsEntry("standardIds", List.of(WayangStandardRegistry.A2UI));
    }

    @Test
    void registersAndDiscoversCapabilitiesInInsertionOrder() {
        WayangProviderCapabilityRegistry registry = WayangProviderCapabilityRegistry.create();
        WayangProviderCapabilityDescriptor rag = capability(
                "rag.retrieve",
                "wayang-rag",
                "rag",
                WayangProviderCapabilityCatalog.TYPE_RAG,
                WayangProviderCapabilityState.AVAILABLE,
                List.of("assistant-agent"),
                List.of(),
                List.of("retrieval"));
        WayangProviderCapabilityDescriptor a2ui = capability(
                "a2ui.contracts",
                "wayang-a2ui",
                "a2ui",
                WayangProviderCapabilityCatalog.TYPE_STANDARD,
                WayangProviderCapabilityState.PREVIEW,
                List.of("assistant-agent", "platform-admin"),
                List.of(WayangStandardRegistry.A2UI),
                List.of("ui-contracts"));

        registry.register(rag);
        registry.register(a2ui);

        assertThat(registry.capabilityIds()).containsExactly("rag.retrieve", "a2ui.contracts");
        assertThat(registry.providerIds()).containsExactly("wayang-rag", "wayang-a2ui");
        assertThat(registry.providerNamespaces()).containsExactly("wayang");
        assertThat(registry.moduleIds()).containsExactly("rag", "a2ui");
        assertThat(registry.capabilityTypes()).containsExactly("rag", "standard");
        assertThat(registry.surfaceIds()).containsExactly("assistant-agent", "platform-admin");
        assertThat(registry.standardIds()).containsExactly(WayangStandardRegistry.A2UI);
        assertThat(registry.find(" A2UI.Contracts ")).contains(a2ui);
        assertThat(registry.discover(new WayangProviderCapabilityQuery(
                null,
                "wayang-rag",
                "wayang",
                "rag",
                "rag",
                WayangProviderCapabilityState.AVAILABLE,
                "assistant-agent",
                null,
                "retrieval"))).containsExactly(rag);
        assertThat(registry.discover(WayangProviderCapabilityQuery.forStandard("agent-to-user-interface")))
                .containsExactly(a2ui);
        assertThat(registry.maps(WayangProviderCapabilityQuery.forModule("a2ui")))
                .singleElement()
                .satisfies(values -> assertThat(values).containsEntry("id", "a2ui.contracts"));
    }

    @Test
    void rejectsDuplicateCapabilitiesAndReturnsImmutableSnapshots() {
        WayangProviderCapabilityRegistry registry = WayangProviderCapabilityRegistry.of(List.of(capability(
                "rag.retrieve",
                "wayang-rag",
                "rag",
                WayangProviderCapabilityCatalog.TYPE_RAG,
                WayangProviderCapabilityState.AVAILABLE,
                List.of(),
                List.of(),
                List.of())));

        assertThatThrownBy(() -> registry.register(capability(
                " RAG.Retrieve ",
                "other",
                "rag",
                WayangProviderCapabilityCatalog.TYPE_RAG,
                WayangProviderCapabilityState.AVAILABLE,
                List.of(),
                List.of(),
                List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate Wayang provider capability id 'rag.retrieve'.");
        assertThatThrownBy(() -> registry.list().add(capability(
                "new",
                "other",
                "core",
                "general",
                WayangProviderCapabilityState.AVAILABLE,
                List.of(),
                List.of(),
                List.of())))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(registry.unregister(" RAG.Retrieve ")).isTrue();
        assertThat(registry.size()).isZero();
        assertThat(registry.unregister("missing")).isFalse();
    }

    @Test
    void defaultCatalogSeedsCrossModuleProviderCapabilities() {
        WayangProviderCapabilityRegistry registry = WayangProviderCapabilityCatalog.defaultRegistry();

        assertThat(WayangProviderCapabilityCatalog.defaultAvailableCapabilityCount()).isEqualTo(8);
        assertThat(registry.capabilityIds()).containsExactly(
                "skills.dynamic",
                "mcp.bridge",
                "rag.retrieve",
                "storage.hybrid-persistence",
                "a2a.alignment",
                "a2ui.contracts",
                "agentic-commerce.protocol",
                "runtime.lifecycle");
        assertThat(registry.require("a2ui.contracts").standardIds())
                .containsExactly(WayangStandardRegistry.A2UI);
        assertThat(registry.discover(WayangProviderCapabilityQuery.forStandard("agenticcommerce")))
                .extracting(WayangProviderCapabilityDescriptor::id)
                .containsExactly("agentic-commerce.protocol");
        assertThat(registry.discover(WayangProviderCapabilityQuery.forSurface("coding-agent")))
                .extracting(WayangProviderCapabilityDescriptor::id)
                .contains("skills.dynamic", "mcp.bridge", "runtime.lifecycle")
                .doesNotContain("rag.retrieve", "a2ui.contracts");
    }

    @Test
    void discoveryEnvelopeReportsFacetsSearchAndJsonMaps() {
        WayangProviderCapabilityDiscovery discovery = WayangProviderCapabilityDiscoveryService.create()
                .discover(
                        WayangProviderCapabilityCatalog.defaultRegistry(),
                        new WayangProviderCapabilityQuery(
                                null,
                                null,
                                "wayang",
                                null,
                                null,
                                null,
                                "assistant-agent",
                                null,
                                null),
                        "commerce");

        assertThat(discovery.query().providerNamespace()).isEqualTo("wayang");
        assertThat(discovery.query().surfaceId()).isEqualTo("assistant-agent");
        assertThat(discovery.search()).isEqualTo("commerce");
        assertThat(discovery.totalCapabilities()).isEqualTo(8);
        assertThat(discovery.matchingCapabilities()).isOne();
        assertThat(discovery.capabilityIds()).containsExactly("agentic-commerce.protocol");
        assertThat(discovery.providerIds()).containsExactly("wayang-agentic-commerce");
        assertThat(discovery.providerIdCounts()).containsExactly(Map.entry("wayang-agentic-commerce", 1));
        assertThat(discovery.moduleIds()).containsExactly("agentic-commerce");
        assertThat(discovery.moduleIdCounts()).containsExactly(Map.entry("agentic-commerce", 1));
        assertThat(discovery.capabilityTypes()).containsExactly(WayangProviderCapabilityCatalog.TYPE_COMMERCE);
        assertThat(discovery.capabilityTypeCounts())
                .containsExactly(Map.entry(WayangProviderCapabilityCatalog.TYPE_COMMERCE, 1));
        assertThat(discovery.standardIds()).containsExactly(WayangStandardRegistry.AGENTIC_COMMERCE);
        assertThat(discovery.standardIdCounts()).containsExactly(Map.entry(WayangStandardRegistry.AGENTIC_COMMERCE, 1));
        assertThat(discovery.providerSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo("wayang-agentic-commerce");
                    assertThat(summary.count()).isOne();
                    assertThat(summary.capabilityIds()).containsExactly("agentic-commerce.protocol");
                });
        assertThat(discovery.standardSummaries())
                .singleElement()
                .satisfies(summary -> {
                    assertThat(summary.name()).isEqualTo(WayangStandardRegistry.AGENTIC_COMMERCE);
                    assertThat(summary.capabilityIds()).containsExactly("agentic-commerce.protocol");
                });
        assertThat(discovery.capabilityMaps())
                .singleElement()
                .satisfies(values -> assertThat(values)
                        .containsEntry("id", "agentic-commerce.protocol")
                        .containsEntry("providerId", "wayang-agentic-commerce"));
    }

    @Test
    void discoveryServiceSearchesMetadataAndHandlesNullRegistry() {
        WayangProviderCapabilityDiscoveryService service = WayangProviderCapabilityDiscoveryService.create();

        assertThat(service.discover(null, null, "anything").empty()).isTrue();
        assertThat(service.discover(
                        WayangProviderCapabilityCatalog.defaultRegistry(),
                        WayangProviderCapabilityQuery.all(),
                        "AgentRunLifecycleService")
                .capabilityIds()).containsExactly("runtime.lifecycle");
    }

    private static WayangProviderCapabilityDescriptor capability(
            String id,
            String providerId,
            String moduleId,
            String capabilityType,
            WayangProviderCapabilityState state,
            List<String> surfaceIds,
            List<String> standardIds,
            List<String> tags) {
        return new WayangProviderCapabilityDescriptor(
                id,
                providerId,
                "wayang",
                moduleId,
                capabilityType,
                id + " capability",
                "description",
                state,
                surfaceIds,
                standardIds,
                tags,
                Map.of());
    }
}
