package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliGoldenFixtureContractsTest {

    @Test
    void resolvesExplicitAndSelfDescribingManifestContractIds() throws IOException {
        Set<String> jsonSchemaIds = WayangCliGoldenFixtureContracts.jsonSchemaIds(List.of(
                entry("status-json.golden"),
                entry("run-result-json.golden")));

        assertThat(jsonSchemaIds)
                .containsExactlyInAnyOrder(
                        "urn:wayang:contract:wayang.platform.catalog:v1:platform-status",
                        "urn:wayang:contract:wayang.run.lifecycle:v1:run-result");
    }

    @Test
    void resolvesMultiEnvelopeSelfDescribingFixtures() throws IOException {
        assertThat(WayangCliGoldenFixtureContracts.selfDescribingJsonSchemaIds("run-events-follow-json.golden"))
                .containsExactlyInAnyOrder(
                        "urn:wayang:contract:wayang.run.lifecycle:v1:run-events",
                        "urn:wayang:contract:wayang.run.lifecycle:v1:run-events-follow");
    }

    private static WayangCliGoldenFixtureManifest.Entry entry(String name) {
        return WayangCliGoldenFixtureManifest.entries().stream()
                .filter(candidate -> candidate.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing fixture manifest entry: " + name));
    }
}
