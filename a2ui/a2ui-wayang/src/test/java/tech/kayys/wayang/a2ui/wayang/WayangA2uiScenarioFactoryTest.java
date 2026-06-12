package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiScenarioFactoryTest {

    @Test
    void normalizesNullableHttpScenarioVarargs() {
        WayangA2uiHttpRequest request = WayangA2uiHttpRequest.routeCatalog();

        WayangA2uiHttpScenario empty =
                WayangA2uiHttpScenario.of("http", (WayangA2uiHttpRequest[]) null);
        WayangA2uiHttpScenario scenario = WayangA2uiHttpScenario.of("http", null, request);

        assertThat(empty.requests()).isEmpty();
        assertThat(scenario.requests()).containsExactly(request);
    }

    @Test
    void normalizesNullableHttpScenarioSuiteVarargs() {
        WayangA2uiHttpScenario scenario =
                WayangA2uiHttpScenario.of("http", WayangA2uiHttpRequest.routeCatalog());

        WayangA2uiHttpScenarioSuite empty =
                WayangA2uiHttpScenarioSuite.of("suite", (WayangA2uiHttpScenario[]) null);
        WayangA2uiHttpScenarioSuite suite = WayangA2uiHttpScenarioSuite.of("suite", null, scenario);

        assertThat(empty.scenarios()).isEmpty();
        assertThat(suite.scenarios()).containsExactly(scenario);
    }

    @Test
    void normalizesNullableBridgeScenarioVarargs() {
        WayangA2uiBridgeRequest request =
                WayangA2uiBridgeRequest.of(WayangA2uiTransportRequest.surfaceCatalog());

        WayangA2uiBridgeScenario empty =
                WayangA2uiBridgeScenario.of("bridge", (WayangA2uiBridgeRequest[]) null);
        WayangA2uiBridgeScenario scenario = WayangA2uiBridgeScenario.of("bridge", null, request);

        assertThat(empty.requests()).isEmpty();
        assertThat(scenario.requests()).containsExactly(request);
    }
}
