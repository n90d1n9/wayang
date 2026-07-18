package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangStandardCatalogEnvelopesTest {

    @Test
    void catalogEnvelopeOwnsPublishedStandardCatalogShape() {
        Map<String, Object> values = WayangStandardCatalogEnvelopes.catalog(
                " Wayang ",
                WayangStandardCatalog.defaultCatalog());

        assertThat(values)
                .containsEntry("product", "Wayang")
                .containsEntry("totalStandards", 3)
                .containsEntry("standardIds", List.of("a2a", "a2ui", "agentic-commerce"))
                .containsEntry("names", List.of(
                        "Agent2Agent Protocol",
                        "Agent-to-User Interface",
                        "Agentic Commerce Protocol"))
                .containsEntry("versions", List.of("1.0", "v0.8", "2026-01-30"))
                .containsEntry("bindings", List.of("JSONRPC", "HTTP", "HTTP+JSON"))
                .containsEntry("bindingCounts", Map.of("JSONRPC", 1, "HTTP", 1, "HTTP+JSON", 1));
        assertThat(list(values.get("standards")))
                .hasSize(3)
                .first()
                .satisfies(standard -> assertThat(objectMap(standard))
                        .containsEntry("standardId", "a2a")
                        .containsEntry("name", "Agent2Agent Protocol")
                        .containsEntry("attributes", Map.of()));
    }

    @Test
    void standardEnvelopeKeepsAttributesNestedAndSorted() {
        WayangStandardDefinition definition = new WayangStandardDefinition(
                " custom ",
                " Custom Standard ",
                " 1.0 ",
                " HTTP ",
                " https://example.test/spec ",
                List.of("custom-standard"),
                Map.of("zeta", 2, "alpha", 1));

        Map<String, Object> values = WayangStandardCatalogEnvelopes.standard(definition);

        assertThat(values)
                .containsEntry("standardId", "custom")
                .containsEntry("name", "Custom Standard")
                .containsEntry("version", "1.0")
                .containsEntry("binding", "HTTP")
                .containsEntry("specUrl", "https://example.test/spec")
                .containsEntry("aliases", List.of("custom-standard"));
        assertThat(objectMap(values.get("attributes")).keySet())
                .containsExactly("alpha", "zeta");
    }

    @Test
    void nullCatalogProducesEmptyCatalogEnvelope() {
        Map<String, Object> values = WayangStandardCatalogEnvelopes.catalog(null, null);

        assertThat(values)
                .containsEntry("product", "")
                .containsEntry("totalStandards", 0)
                .containsEntry("standardIds", List.of())
                .containsEntry("standards", List.of());
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void normalizeProvidesEmptyStandardCatalogModel() {
        WayangStandardCatalog model = WayangStandardCatalogEnvelopes.normalize(null);

        assertThat(model.totalStandards()).isZero();
        assertThat(model.standardIds()).isEmpty();
        assertThat(model.standards()).isEmpty();
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
