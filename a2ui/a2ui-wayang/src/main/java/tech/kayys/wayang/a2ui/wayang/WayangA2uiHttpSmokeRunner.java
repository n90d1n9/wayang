package tech.kayys.wayang.a2ui.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * Dependency-free smoke runner for A2UI HTTP bindings.
 */
public final class WayangA2uiHttpSmokeRunner {

    private final WayangA2uiHttpHarness harness;
    private final WayangA2uiHttpRouteCatalog routeCatalog;

    public WayangA2uiHttpSmokeRunner(
            WayangA2uiHttpHarness harness,
            WayangA2uiHttpRouteCatalog routeCatalog) {
        this.harness = Objects.requireNonNull(harness, "harness");
        this.routeCatalog = routeCatalog == null ? WayangA2uiHttpRouteCatalog.defaultCatalog() : routeCatalog;
    }

    public static WayangA2uiHttpSmokeRunner of(WayangA2uiHttpBridgeAdapter adapter) {
        WayangA2uiHttpBridgeAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        return new WayangA2uiHttpSmokeRunner(WayangA2uiHttpHarness.of(resolved), resolved.routeCatalog());
    }

    public WayangA2uiHttpSmokeResult run() {
        WayangA2uiHttpScenarioSuite suite = WayangA2uiHttpScenarios.smokeSuite(routeCatalog);
        WayangA2uiHttpScenarioSuiteResult suiteResult = harness.run(suite);
        WayangA2uiHttpExpectationResult expectationResult = suiteResult.validate(
                WayangA2uiHttpScenarios.smokeSuiteExpectation(routeCatalog));
        return new WayangA2uiHttpSmokeResult(
                suiteResult,
                expectationResult,
                Map.of(
                        "suiteId",
                        suite.id(),
                        "routeCount",
                        routeCatalog.routeCount()));
    }
}
