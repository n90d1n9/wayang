package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in A2A HTTP smoke scenario definitions.
 */
public final class WayangA2aHttpScenarios {

    private WayangA2aHttpScenarios() {
    }

    public static WayangA2aHttpScenario smoke(A2aSendMessageRequest sendMessageRequest) {
        String taskId = WayangA2aSendMessageIdentity.requiredMessageTaskId(
                sendMessageRequest,
                "Smoke scenario requires a request message taskId");
        String sendJson = sendMessageRequest.toJson();
        return new WayangA2aHttpScenario(
                "a2a.http.smoke",
                "A2A HTTP discovery, send, task read, subscribe, and push config smoke scenario",
                List.of(
                        WayangA2aHttpScenarioExchange.of(WayangA2aHttpRequest.get(
                                A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH)),
                        WayangA2aHttpScenarioExchange.of(WayangA2aHttpRequest.sendMessage(sendJson)),
                        WayangA2aHttpScenarioExchange.of(WayangA2aHttpRequest.get("/tasks/" + taskId)),
                        subscribeExchange(taskId),
                        WayangA2aHttpScenarioExchange.of(new WayangA2aHttpRequest(
                                "POST",
                                "/tasks/" + taskId + "/pushNotificationConfigs",
                                "{\"configId\":\"smoke\",\"url\":\"https://hooks.example/a2a\"}",
                                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                                Map.of())),
                        WayangA2aHttpScenarioExchange.of(WayangA2aHttpRequest.get(
                                "/tasks/" + taskId + "/pushNotificationConfigs/smoke")),
                        WayangA2aHttpScenarioExchange.of(new WayangA2aHttpRequest(
                                "DELETE",
                                "/tasks/" + taskId + "/pushNotificationConfigs/smoke",
                                "",
                                Map.of(),
                                Map.of()))),
                smokeAttributes(taskId, sendMessageRequest));
    }

    public static WayangA2aHttpScenario routeError(String id, WayangA2aHttpRequest request) {
        return new WayangA2aHttpScenario(
                id,
                "A2A HTTP route error scenario",
                List.of(WayangA2aHttpScenarioExchange.of(request)),
                Map.of());
    }

    private static Map<String, Object> subscriptionAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("afterSequence", 0);
        attributes.put("limit", 50);
        return WayangA2aMaps.copyMap(attributes);
    }

    private static WayangA2aHttpScenarioExchange subscribeExchange(String taskId) {
        return WayangA2aHttpScenarioExchange.of(new WayangA2aHttpRequest(
                "POST",
                "/tasks/" + taskId + ":subscribe",
                "",
                Map.of(WayangA2aHttpResponse.HEADER_ACCEPT, A2aProtocol.EVENT_STREAM_MEDIA_TYPE),
                subscriptionAttributes()));
    }

    private static Map<String, Object> smokeAttributes(
            String taskId,
            A2aSendMessageRequest sendMessageRequest) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("taskId", taskId);
        attributes.put("messageId", sendMessageRequest.message().messageId());
        return WayangA2aMaps.copyMap(attributes);
    }

}
