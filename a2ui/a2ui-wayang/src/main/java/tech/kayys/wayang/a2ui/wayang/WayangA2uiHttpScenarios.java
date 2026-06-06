package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Built-in A2UI HTTP scenarios for smoke, discovery, and binding diagnostics.
 */
public final class WayangA2uiHttpScenarios {

    public static final String DISCOVERY_ID = "a2ui-http-discovery";
    public static final String ROUTE_OPTIONS_ID = "a2ui-http-route-options";
    public static final String DIAGNOSTICS_ID = "a2ui-http-diagnostics";
    public static final String SMOKE_SUITE_ID = "a2ui-http-smoke-suite";

    private static final String ATTRIBUTE_SCENARIO_KIND = "scenarioKind";
    private static final String ATTRIBUTE_SUITE_KIND = "suiteKind";
    private static final String ATTRIBUTE_ROUTE_COUNT = "routeCount";
    private static final String ATTRIBUTE_ROUTE_OPERATION = "routeOperation";
    private static final String ATTRIBUTE_ROUTE_METHOD = "routeMethod";

    private WayangA2uiHttpScenarios() {
    }

    public static WayangA2uiHttpScenario discovery() {
        return discovery(WayangA2uiHttpRouteCatalog.defaultCatalog());
    }

    public static WayangA2uiHttpScenario discovery(WayangA2uiHttpRouteCatalog catalog) {
        WayangA2uiHttpRouteCatalog resolved = resolveCatalog(catalog);
        return WayangA2uiHttpScenario.of(
                        DISCOVERY_ID,
                        WayangA2uiHttpRequest.routeCatalog(),
                        WayangA2uiHttpRequest.bindingReport())
                .withAttributes(attributes("discovery", resolved));
    }

    public static WayangA2uiHttpScenario routeOptions() {
        return routeOptions(WayangA2uiHttpRouteCatalog.defaultCatalog());
    }

    public static WayangA2uiHttpScenario routeOptions(WayangA2uiHttpRouteCatalog catalog) {
        WayangA2uiHttpRouteCatalog resolved = resolveCatalog(catalog);
        return WayangA2uiHttpScenario.of(
                        ROUTE_OPTIONS_ID,
                        resolved.routes().stream()
                                .map(WayangA2uiHttpScenarios::optionRequest)
                                .toList())
                .withAttributes(attributes("route-options", resolved));
    }

    public static WayangA2uiHttpScenario diagnostics() {
        return diagnostics(WayangA2uiHttpRouteCatalog.defaultCatalog());
    }

    public static WayangA2uiHttpScenario diagnostics(WayangA2uiHttpRouteCatalog catalog) {
        WayangA2uiHttpRouteCatalog resolved = resolveCatalog(catalog);
        List<WayangA2uiHttpRequest> requests = new ArrayList<>();
        requests.add(WayangA2uiHttpRequest.surfaceCatalog());
        requests.add(WayangA2uiHttpRequest.routeCatalog());
        requests.add(WayangA2uiHttpRequest.bindingReport());
        resolved.routes().stream()
                .map(WayangA2uiHttpScenarios::optionRequest)
                .forEach(requests::add);
        return WayangA2uiHttpScenario.of(DIAGNOSTICS_ID, requests)
                .withAttributes(attributes("diagnostics", resolved));
    }

    public static WayangA2uiHttpScenarioSuite smokeSuite() {
        return smokeSuite(WayangA2uiHttpRouteCatalog.defaultCatalog());
    }

    public static WayangA2uiHttpScenarioSuite smokeSuite(WayangA2uiHttpRouteCatalog catalog) {
        WayangA2uiHttpRouteCatalog resolved = resolveCatalog(catalog);
        return WayangA2uiHttpScenarioSuite.of(
                        SMOKE_SUITE_ID,
                        discovery(resolved),
                        routeOptions(resolved),
                        diagnostics(resolved))
                .withAttributes(Map.of(
                        ATTRIBUTE_SUITE_KIND, "smoke",
                        ATTRIBUTE_ROUTE_COUNT, resolved.routeCount()));
    }

    public static WayangA2uiHttpScenarioSuiteExpectation smokeSuiteExpectation() {
        return smokeSuiteExpectation(WayangA2uiHttpRouteCatalog.defaultCatalog());
    }

    public static WayangA2uiHttpScenarioSuiteExpectation smokeSuiteExpectation(WayangA2uiHttpRouteCatalog catalog) {
        return WayangA2uiHttpScenarioSuiteExpectation.pass()
                .withExpectedScenarioCount(3)
                .withExpectedScenarioIds(List.of(DISCOVERY_ID, ROUTE_OPTIONS_ID, DIAGNOSTICS_ID));
    }

    private static WayangA2uiHttpRequest optionRequest(WayangA2uiHttpRoute route) {
        return new WayangA2uiHttpRequest(
                "OPTIONS",
                route.path(),
                "",
                Map.of(),
                Map.of(
                        ATTRIBUTE_ROUTE_OPERATION, route.operation(),
                        ATTRIBUTE_ROUTE_METHOD, route.method()));
    }

    private static Map<String, Object> attributes(String scenarioKind, WayangA2uiHttpRouteCatalog catalog) {
        return Map.of(
                ATTRIBUTE_SCENARIO_KIND, scenarioKind,
                ATTRIBUTE_ROUTE_COUNT, catalog.routeCount());
    }

    private static WayangA2uiHttpRouteCatalog resolveCatalog(WayangA2uiHttpRouteCatalog catalog) {
        return catalog == null ? WayangA2uiHttpRouteCatalog.defaultCatalog() : catalog;
    }
}
