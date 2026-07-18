package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangProviderCapabilityEnvelopesTest {

    @Test
    void discoveryEnvelopeOwnsPublishedProviderCapabilityShape() {
        WayangProviderCapabilityDiscovery discovery = WayangProviderCapabilityDiscoveryService.create()
                .discover(
                        WayangProviderCapabilityCatalog.defaultRegistry(),
                        WayangProviderCapabilityQuery.forModule("a2ui"),
                        "");

        Map<String, Object> values = WayangProviderCapabilityEnvelopes.discovery(" Wayang ", discovery);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("search", null)
                .containsEntry("totalCapabilities", 8)
                .containsEntry("matchingCapabilities", 1)
                .containsEntry("capabilityIds", List.of("a2ui.contracts"))
                .containsEntry("providerIds", List.of("wayang-a2ui"))
                .containsEntry("moduleIds", List.of("a2ui"))
                .containsEntry("standardIds", List.of(WayangStandardRegistry.A2UI));

        Map<String, Object> query = objectMap(values.get("query"));
        assertThat(query)
                .containsEntry("capabilityId", null)
                .containsEntry("providerId", null)
                .containsEntry("providerNamespace", null)
                .containsEntry("moduleId", "a2ui")
                .containsEntry("capabilityType", null)
                .containsEntry("state", null)
                .containsEntry("surfaceId", null)
                .containsEntry("standardId", null)
                .containsEntry("tag", null)
                .containsEntry("filtered", true);

        assertThat(list(values.get("providerSummaries")))
                .singleElement()
                .satisfies(summary -> assertThat(objectMap(summary))
                        .containsEntry("name", "wayang-a2ui")
                        .containsEntry("count", 1)
                        .containsEntry("capabilityIds", List.of("a2ui.contracts")));
        assertThat(list(values.get("capabilities")))
                .singleElement()
                .satisfies(capability -> assertThat(objectMap(capability))
                        .containsEntry("id", "a2ui.contracts")
                        .containsEntry("providerId", "wayang-a2ui")
                        .containsEntry("state", "preview")
                        .containsEntry("available", true));
    }

    @Test
    void detailEnvelopeUsesDescriptorMap() {
        WayangProviderCapabilityDescriptor capability = WayangProviderCapabilityCatalog.defaultRegistry()
                .require("storage.hybrid-persistence");

        Map<String, Object> values = WayangProviderCapabilityEnvelopes.detail("Wayang", capability);

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("capabilityId", "storage.hybrid-persistence");
        assertThat(objectMap(values.get("capability")))
                .containsEntry("id", "storage.hybrid-persistence")
                .containsEntry("providerId", "wayang-storage")
                .containsEntry("moduleId", "storage")
                .containsEntry("metadata", Map.of("fallback", "files"));
    }

    @Test
    void queryAndFacetSummaryNormalizeForJsonContracts() {
        Map<String, Object> query = WayangProviderCapabilityEnvelopes.query(new WayangProviderCapabilityQuery(
                null,
                " WAYANG-RAG ",
                null,
                null,
                WayangProviderCapabilityCatalog.TYPE_RAG,
                WayangProviderCapabilityState.PREVIEW,
                "",
                " AgenticCommerce ",
                " Retrieval "));
        Map<String, Object> summary = WayangProviderCapabilityEnvelopes.facetSummary(
                new WayangProviderCapabilityFacetSummary(" rag ", 2, List.of("one", "two")));

        assertThat(query)
                .containsEntry("capabilityId", null)
                .containsEntry("providerId", "wayang-rag")
                .containsEntry("providerNamespace", null)
                .containsEntry("moduleId", null)
                .containsEntry("capabilityType", WayangProviderCapabilityCatalog.TYPE_RAG)
                .containsEntry("state", "preview")
                .containsEntry("surfaceId", null)
                .containsEntry("standardId", WayangStandardRegistry.AGENTIC_COMMERCE)
                .containsEntry("tag", "retrieval")
                .containsEntry("filtered", true);
        assertThat(summary)
                .containsEntry("name", "rag")
                .containsEntry("count", 2)
                .containsEntry("capabilityIds", List.of("one", "two"));
    }

    @Test
    void nullDiscoveryProducesEmptyDiscoveryEnvelope() {
        Map<String, Object> values = WayangProviderCapabilityEnvelopes.discovery(null, null);

        assertThat(values)
                .containsEntry("product", "")
                .containsEntry("search", null)
                .containsEntry("totalCapabilities", 0)
                .containsEntry("matchingCapabilities", 0)
                .containsEntry("capabilityIds", List.of())
                .containsEntry("capabilities", List.of());
        assertThat(objectMap(values.get("query"))).containsEntry("filtered", false);
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void normalizeProvidesEmptyProviderCapabilityDiscoveryModel() {
        WayangProviderCapabilityDiscovery model = WayangProviderCapabilityEnvelopes.normalize(null);

        assertThat(model.query()).isEqualTo(WayangProviderCapabilityQuery.all());
        assertThat(model.search()).isEmpty();
        assertThat(model.totalCapabilities()).isZero();
        assertThat(model.matchingCapabilities()).isZero();
        assertThat(model.capabilities()).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        return (List<Object>) value;
    }
}
