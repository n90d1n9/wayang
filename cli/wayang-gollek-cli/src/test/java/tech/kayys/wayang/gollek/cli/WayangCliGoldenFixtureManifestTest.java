package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliGoldenFixtureManifestTest {

    @Test
    void manifestCoversGoldenFixtureCatalogInOrder() {
        List<String> fixtureNames = WayangCliGoldenFixtures.all().stream()
                .map(WayangCliGoldenFixtures.GoldenFixture::name)
                .toList();
        List<String> manifestNames = WayangCliGoldenFixtureManifest.entries().stream()
                .map(WayangCliGoldenFixtureManifest.Entry::name)
                .toList();

        assertThat(manifestNames)
                .doesNotHaveDuplicates()
                .containsExactlyElementsOf(fixtureNames);
    }

    @Test
    void manifestCarriesExplicitSchemaMetadata() {
        WayangCliGoldenFixtureManifest.Entry entry = entry("status-json.golden");

        assertThat(entry.sdkSource()).isEqualTo("local");
        assertThat(entry.schemaMode()).isEqualTo("explicit");
        assertThat(entry.schema()).isEqualTo("wayang.platform.catalog");
        assertThat(entry.envelope()).isEqualTo("platform-status");
        assertThat(entry.jsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.platform.catalog:v1:platform-status");
        assertThat(entry.commandIds()).containsExactly("status-json");
        assertThat(entry.commandLine()).isEqualTo("status --json");
    }

    @Test
    void manifestCarriesLifecycleFixtureMetadata() {
        WayangCliGoldenFixtureManifest.Entry entry = entry("run-cancel-json.golden");

        assertThat(entry.sdkSource()).isEqualTo("contract-lifecycle");
        assertThat(entry.expectedExitCode()).isEqualTo(1);
        assertThat(entry.schemaMode()).isEqualTo("self_describing");
        assertThat(entry.schema()).isEmpty();
        assertThat(entry.envelope()).isEmpty();
        assertThat(entry.commandIds()).containsExactly("run-cancel-json");
        assertThat(entry.commandLine()).contains("run cancel", "--reason 'contract stop'", "--json");
    }

    @Test
    void manifestCarriesExplicitCommandIdOverrides() {
        assertThat(entry("commands-detail-json.golden").commandIds())
                .containsExactly("commands-id-json");
        assertThat(entry("run-preview-json.golden").commandIds())
                .containsExactly("run-dry-json");
        assertThat(entry("run-events-json.golden").commandIds())
                .containsExactly("run-events-filter-json");
        assertThat(entry("run-events-follow-json.golden").commandIds())
                .containsExactly("run-events-follow-result-json");
        assertThat(entry("run-list-json.golden").commandIds())
                .containsExactly("run-list-filter-json");
    }

    @Test
    void manifestExposesTypedSchemaModePredicates() {
        WayangCliGoldenFixtureManifest.Entry explicit = entry("status-json.golden");
        WayangCliGoldenFixtureManifest.Entry selfDescribing = entry("run-result-json.golden");
        WayangCliGoldenFixtureManifest.Entry unvalidated = entry("contracts-json.golden");

        assertThat(explicit.explicitSchema()).isTrue();
        assertThat(explicit.selfDescribingSchema()).isFalse();
        assertThat(explicit.schemaValidated()).isTrue();
        assertThat(selfDescribing.explicitSchema()).isFalse();
        assertThat(selfDescribing.selfDescribingSchema()).isTrue();
        assertThat(selfDescribing.schemaValidated()).isTrue();
        assertThat(unvalidated.explicitSchema()).isFalse();
        assertThat(unvalidated.selfDescribingSchema()).isFalse();
        assertThat(unvalidated.schemaValidated()).isFalse();
    }

    private static WayangCliGoldenFixtureManifest.Entry entry(String name) {
        return WayangCliGoldenFixtureManifest.entries().stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing fixture manifest entry: " + name));
    }
}
