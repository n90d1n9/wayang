package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic runner for replaying A2UI bridge scenarios.
 */
public final class WayangA2uiBridgeHarness {

    private final WayangA2uiBridge bridge;

    public WayangA2uiBridgeHarness(WayangA2uiBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public static WayangA2uiBridgeHarness of(WayangA2uiBridge bridge) {
        return new WayangA2uiBridgeHarness(bridge);
    }

    public WayangA2uiBridgeScenarioResult run(WayangA2uiBridgeScenario scenario) {
        WayangA2uiBridgeScenario resolved = Objects.requireNonNull(scenario, "scenario");
        List<WayangA2uiBridgeScenarioExchange> exchanges = new ArrayList<>();
        for (int index = 0; index < resolved.requests().size(); index++) {
            WayangA2uiBridgeRequest request = resolved.requests().get(index);
            exchanges.add(new WayangA2uiBridgeScenarioExchange(index + 1, request, bridge.exchange(request)));
        }
        return new WayangA2uiBridgeScenarioResult(resolved.id(), exchanges, resolved.attributes());
    }

    public WayangA2uiBridgeScenarioResult run(String scenarioId, List<WayangA2uiBridgeRequest> requests) {
        return run(WayangA2uiBridgeScenario.of(scenarioId, requests));
    }
}
