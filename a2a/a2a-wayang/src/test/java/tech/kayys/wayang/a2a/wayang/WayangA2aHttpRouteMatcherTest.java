package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpRouteMatcherTest {

    private final WayangA2aHttpRouteMatcher matcher =
            new WayangA2aHttpRouteMatcher(A2aHttpRouteCatalog.standard());

    @Test
    void matchesStaticAndTemplatedRoutes() {
        WayangA2aHttpRouteMatch card = matcher.match(WayangA2aHttpRequest.get(
                A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH)).orElseThrow();
        WayangA2aHttpRouteMatch task = matcher.match(WayangA2aHttpRequest.get(
                "/tasks/task-1?includeHistory=true")).orElseThrow();
        WayangA2aHttpRouteMatch cancel = matcher.match(new WayangA2aHttpRequest(
                "POST", "/tasks/task-2:cancel", "", Map.of(), Map.of())).orElseThrow();
        WayangA2aHttpRouteMatch config = matcher.match(new WayangA2aHttpRequest(
                "DELETE", "/tasks/task%203/pushNotificationConfigs/config-1", "", Map.of(), Map.of()))
                .orElseThrow();

        assertThat(card.operation()).isEqualTo(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD);
        assertThat(task.operation()).isEqualTo(A2aProtocol.OPERATION_GET_TASK);
        assertThat(task.pathParameter("id")).contains("task-1");
        assertThat(cancel.operation()).isEqualTo(A2aProtocol.OPERATION_CANCEL_TASK);
        assertThat(cancel.pathParameter("id")).contains("task-2");
        assertThat(config.operation()).isEqualTo(A2aProtocol.OPERATION_DELETE_TASK_PUSH_NOTIFICATION_CONFIG);
        assertThat(config.pathParameter("id")).contains("task 3");
        assertThat(config.pathParameter("configId")).contains("config-1");
        assertThat(config.pathParameters().keySet()).containsExactly("id", "configId");
    }

    @Test
    void separatesPathMatchesFromMethodMatches() {
        WayangA2aHttpRequest wrongMethod = WayangA2aHttpRequest.get("/message:send");

        assertThat(matcher.match(wrongMethod)).isEmpty();
        assertThat(matcher.routesForPath("/message:send"))
                .singleElement()
                .extracting(route -> route.operation())
                .isEqualTo(A2aProtocol.OPERATION_SEND_MESSAGE);
    }
}
