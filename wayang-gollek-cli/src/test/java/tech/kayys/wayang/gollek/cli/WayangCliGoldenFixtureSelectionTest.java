package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WayangCliGoldenFixtureSelectionTest {

    @Test
    void defaultsToAllFixturesWhenNoIncludePropertyIsSet() {
        String previous = System.getProperty(WayangCliGoldenFixtureSelection.UPDATE_INCLUDE_PROPERTY);
        try {
            System.clearProperty(WayangCliGoldenFixtureSelection.UPDATE_INCLUDE_PROPERTY);

            assertThat(WayangCliGoldenFixtureSelection.fromSystemProperties()
                    .selected("status-json.golden")).isTrue();
        } finally {
            restoreProperty(WayangCliGoldenFixtureSelection.UPDATE_INCLUDE_PROPERTY, previous);
        }
    }

    @Test
    void acceptsGoldenNamesAndBareNames() {
        WayangCliGoldenFixtureSelection selection = WayangCliGoldenFixtureSelection.from(
                "status-json.golden, readiness-profiles-check-json");

        assertThat(selection.selected("status-json.golden")).isTrue();
        assertThat(selection.selected("readiness-profiles-check-json.golden")).isTrue();
        assertThat(selection.selected("commands-index-json.golden")).isFalse();
    }

    @Test
    void reportsUnknownSelectionsInInputOrder() {
        WayangCliGoldenFixtureSelection selection = WayangCliGoldenFixtureSelection.from(
                "missing-a, status-json, missing-b.golden");

        assertThat(selection.unknownIncludes(Set.of("status-json.golden")))
                .containsExactly("missing-a", "missing-b.golden");
    }

    private static void restoreProperty(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }
}
