package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic runner for replaying A2UI HTTP-shaped scenarios.
 */
public final class WayangA2uiHttpHarness {

    private final WayangA2uiHttpBridgeAdapter adapter;

    public WayangA2uiHttpHarness(WayangA2uiHttpBridgeAdapter adapter) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
    }

    public static WayangA2uiHttpHarness of(WayangA2uiHttpBridgeAdapter adapter) {
        return new WayangA2uiHttpHarness(adapter);
    }

    public WayangA2uiHttpScenarioResult run(WayangA2uiHttpScenario scenario) {
        WayangA2uiHttpScenario resolved = Objects.requireNonNull(scenario, "scenario");
        List<WayangA2uiHttpScenarioExchange> exchanges = new ArrayList<>();
        for (int index = 0; index < resolved.requests().size(); index++) {
            WayangA2uiHttpRequest request = resolved.requests().get(index);
            exchanges.add(new WayangA2uiHttpScenarioExchange(index + 1, request, adapter.handle(request)));
        }
        return new WayangA2uiHttpScenarioResult(resolved.id(), exchanges, resolved.attributes());
    }

    public WayangA2uiHttpScenarioResult run(String scenarioId, List<WayangA2uiHttpRequest> requests) {
        return run(WayangA2uiHttpScenario.of(scenarioId, requests));
    }

    public WayangA2uiHttpScenarioSuiteResult run(WayangA2uiHttpScenarioSuite suite) {
        WayangA2uiHttpScenarioSuite resolved = Objects.requireNonNull(suite, "suite");
        return new WayangA2uiHttpScenarioSuiteResult(
                resolved.id(),
                resolved.scenarios().stream()
                        .map(this::run)
                        .toList(),
                resolved.attributes());
    }
}
