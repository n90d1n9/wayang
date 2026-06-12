package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aHttpOperationHandlerExecutorTest {

    @Test
    void exposesRegisteredOperationsInOrder() {
        Map<String, WayangA2aHttpOperationHandler> handlers = new LinkedHashMap<>();
        handlers.put(A2aProtocol.OPERATION_GET_TASK, okHandler());
        handlers.put(A2aProtocol.OPERATION_CANCEL_TASK, okHandler());
        WayangA2aHttpOperationHandlerExecutor executor = executor(handlers);

        assertThat(executor.operations()).containsExactly(
                A2aProtocol.OPERATION_GET_TASK,
                A2aProtocol.OPERATION_CANCEL_TASK);
        assertThat(executor.supports(A2aProtocol.OPERATION_GET_TASK)).isTrue();
        assertThat(executor.supports(A2aProtocol.OPERATION_SEND_MESSAGE)).isFalse();
    }

    @Test
    void executesRegisteredHandlerWithRoutedResponse() {
        AtomicReference<String> taskId = new AtomicReference<>();
        WayangA2aHttpOperationHandlerExecutor executor = executor(Map.of(
                A2aProtocol.OPERATION_GET_TASK,
                (request, match) -> {
                    taskId.set(match.pathParameter("id").orElse(""));
                    return WayangA2aHttpResponse.object(200, Map.of("ok", true));
                }));

        WayangA2aHttpResponse response = executor.execute(
                WayangA2aHttpRequest.get("/tasks/task-1"),
                match(A2aProtocol.OPERATION_GET_TASK, Map.of("id", "task-1")));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(taskId.get()).isEqualTo("task-1");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_GET_TASK);
    }

    @Test
    void returnsUnsupportedOperationWhenHandlerIsMissing() {
        WayangA2aHttpOperationHandlerExecutor executor = executor(Map.of());

        WayangA2aHttpResponse response = executor.execute(
                WayangA2aHttpRequest.get("/tasks/task-1"),
                match(A2aProtocol.OPERATION_GET_TASK, Map.of("id", "task-1")));

        assertThat(response.statusCode()).isEqualTo(501);
        assertThat(errorCode(response)).isEqualTo("unsupported_route_operation");
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        A2aProtocol.OPERATION_GET_TASK);
    }

    @Test
    void mapsTaskLifecycleExceptionToUnsupportedOperation() {
        WayangA2aHttpOperationHandlerExecutor executor = executor(Map.of(
                A2aProtocol.OPERATION_CANCEL_TASK,
                (request, match) -> {
                    throw new WayangA2aTaskLifecycleException("Task cannot be canceled.");
                }));

        WayangA2aHttpResponse response = executor.execute(
                WayangA2aHttpRequest.postJson("/tasks/task-1:cancel", "{}"),
                match(A2aProtocol.OPERATION_CANCEL_TASK, Map.of("id", "task-1")));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(errorCode(response)).isEqualTo("unsupported_operation");
        assertThat(errorMessage(response)).isEqualTo("Task cannot be canceled.");
    }

    @Test
    void mapsIllegalArgumentExceptionToInvalidRequest() {
        WayangA2aHttpOperationHandlerExecutor executor = executor(Map.of(
                A2aProtocol.OPERATION_GET_TASK,
                (request, match) -> {
                    throw new IllegalArgumentException();
                }));

        WayangA2aHttpResponse response = executor.execute(
                WayangA2aHttpRequest.get("/tasks/task-1"),
                match(A2aProtocol.OPERATION_GET_TASK, Map.of("id", "task-1")));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(errorCode(response)).isEqualTo("invalid_request");
        assertThat(errorMessage(response)).isEqualTo("Invalid A2A HTTP request.");
    }

    @SuppressWarnings("unchecked")
    private static String errorCode(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return String.valueOf(((Map<String, Object>) payload.get("error")).get("code"));
    }

    @SuppressWarnings("unchecked")
    private static String errorMessage(WayangA2aHttpResponse response) {
        Map<String, Object> payload = WayangA2aHttpJson.read(response.body());
        return String.valueOf(((Map<String, Object>) payload.get("error")).get("message"));
    }

    private static WayangA2aHttpOperationHandlerExecutor executor(
            Map<String, ? extends WayangA2aHttpOperationHandler> handlers) {
        return WayangA2aHttpOperationHandlerExecutor.fromHandlers(
                handlers,
                WayangA2aHttpRouteGuard.strict());
    }

    private static WayangA2aHttpOperationHandler okHandler() {
        return (request, match) -> WayangA2aHttpResponse.object(200, Map.of("ok", true));
    }

    private static WayangA2aHttpRouteMatch match(String operation, Map<String, String> pathParameters) {
        return new WayangA2aHttpRouteMatch(route(operation), pathParameters);
    }

    private static A2aHttpRoute route(String operation) {
        return A2aHttpRouteCatalog.standard().routeForOperation(operation).orElseThrow();
    }
}
