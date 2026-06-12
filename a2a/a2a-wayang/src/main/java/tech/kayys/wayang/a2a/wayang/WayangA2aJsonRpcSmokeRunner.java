package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.Objects;

/**
 * Dependency-free smoke runner for A2A JSON-RPC bindings.
 */
public final class WayangA2aJsonRpcSmokeRunner {

    private final WayangA2aJsonRpcHarness harness;
    private final A2aSendMessageRequest sendMessageRequest;

    public WayangA2aJsonRpcSmokeRunner(
            WayangA2aJsonRpcHarness harness,
            A2aSendMessageRequest sendMessageRequest) {
        this.harness = Objects.requireNonNull(harness, "harness");
        this.sendMessageRequest = Objects.requireNonNull(sendMessageRequest, "sendMessageRequest");
    }

    public static WayangA2aJsonRpcSmokeRunner of(
            WayangA2aJsonRpcDispatcher dispatcher,
            A2aSendMessageRequest sendMessageRequest) {
        return new WayangA2aJsonRpcSmokeRunner(
                WayangA2aJsonRpcHarness.of(Objects.requireNonNull(dispatcher, "dispatcher")),
                sendMessageRequest);
    }

    public WayangA2aJsonRpcSmokeResult run() {
        WayangA2aJsonRpcScenario scenario = WayangA2aJsonRpcScenarios.smoke(sendMessageRequest);
        WayangA2aJsonRpcScenarioResult scenarioResult = harness.run(scenario);
        return new WayangA2aJsonRpcSmokeResult(
                scenarioResult,
                WayangA2aJsonRpcSmokeResultProjection.attributes(scenario));
    }
}
