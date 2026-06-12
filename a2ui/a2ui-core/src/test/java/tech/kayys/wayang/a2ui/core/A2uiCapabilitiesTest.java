package tech.kayys.wayang.a2ui.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class A2uiCapabilitiesTest {

    @Test
    void serverCapabilitiesDefaultToStandardCatalog() {
        A2uiServerCapabilities capabilities = new A2uiServerCapabilities(null, true);

        assertThat(capabilities.toParams())
                .containsEntry("acceptsInlineCatalogs", true);
        assertThat(capabilities.supportedCatalogIds())
                .containsExactly(A2uiProtocol.STANDARD_CATALOG_ID);
    }

    @Test
    void clientCapabilitiesSupportInlineCatalogs() {
        A2uiClientCapabilities capabilities = A2uiClientCapabilities.fromMap(Map.of(
                "supportedCatalogIds", List.of(A2uiProtocol.STANDARD_CATALOG_ID),
                "inlineCatalogs", List.of(Map.of("catalogId", "https://example.test/a2ui/custom"))));

        assertThat(capabilities.supports(A2uiProtocol.STANDARD_CATALOG_ID)).isTrue();
        assertThat(capabilities.supports("https://example.test/a2ui/custom")).isTrue();
        assertThat(capabilities.toMetadata()).containsKey("inlineCatalogs");
    }
}
