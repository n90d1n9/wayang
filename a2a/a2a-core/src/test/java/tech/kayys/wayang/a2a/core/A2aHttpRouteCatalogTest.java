package tech.kayys.wayang.a2a.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class A2aHttpRouteCatalogTest {

    @Test
    void catalogsWellKnownDiscoveryAndStandardRestOperations() {
        A2aHttpRouteCatalog catalog = A2aHttpRouteCatalog.standard();

        assertThat(catalog.routes()).hasSize(12);
        assertThat(catalog.routeForOperation(A2aProtocol.OPERATION_DISCOVER_AGENT_CARD))
                .get()
                .extracting(A2aHttpRoute::path)
                .isEqualTo(A2aProtocol.WELL_KNOWN_AGENT_CARD_PATH);
        assertThat(catalog.routeForOperation(A2aProtocol.OPERATION_SEND_MESSAGE))
                .get()
                .extracting(A2aHttpRoute::path)
                .isEqualTo("/message:send");
        assertThat(catalog.routeForOperation(A2aProtocol.OPERATION_LIST_TASKS))
                .get()
                .extracting(A2aHttpRoute::path)
                .isEqualTo("/tasks");
        assertThat(catalog.routeForOperation(A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD))
                .get()
                .extracting(A2aHttpRoute::path)
                .isEqualTo("/extendedAgentCard");
    }

    @Test
    void marksOnlySseRoutesAsStreaming() {
        A2aHttpRouteCatalog catalog = A2aHttpRouteCatalog.standard();

        assertThat(catalog.routeForOperation(A2aProtocol.OPERATION_SEND_STREAMING_MESSAGE))
                .get()
                .extracting(A2aHttpRoute::streaming)
                .isEqualTo(true);
        assertThat(catalog.routeForOperation(A2aProtocol.OPERATION_SUBSCRIBE_TO_TASK))
                .get()
                .extracting(A2aHttpRoute::streaming)
                .isEqualTo(true);
        assertThat(catalog.routeForOperation(A2aProtocol.OPERATION_CANCEL_TASK))
                .get()
                .extracting(A2aHttpRoute::streaming)
                .isEqualTo(false);
    }
}
