package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.tool.dto.ToolRequestContext;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class McpResourceSupportTest {

    @Test
    void currentRequestIdHandlesMissingContext() {
        assertNull(McpResourceSupport.currentRequestId(null));
    }

    @Test
    void currentRequestIdReadsRequestContext() {
        ToolRequestContext context = new ToolRequestContext();
        context.setRequestId("tenant-1");

        assertEquals("tenant-1", McpResourceSupport.currentRequestId(context));
    }

    @Test
    void beanParamKeepsProvidedValueOrCreatesFallback() {
        McpToolCallHistoryQueryParams provided = new McpToolCallHistoryQueryParams();

        assertSame(provided, McpResourceSupport.beanParam(
                provided,
                McpToolCallHistoryQueryParams::new));
        assertEquals(McpToolCallHistoryQueryParams.class, McpResourceSupport.beanParam(
                        null,
                        McpToolCallHistoryQueryParams::new)
                .getClass());
    }

    @Test
    void okMapsUniItemToOkResponse() {
        RestResponse<String> response = McpResourceSupport.ok(Uni.createFrom().item("done"))
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, response.getStatus());
        assertEquals("done", response.getEntity());
    }

    @Test
    void okOrNotFoundMapsUniItemToOkOrNotFoundResponse() {
        RestResponse<String> found = McpResourceSupport.okOrNotFound(Uni.createFrom().item("done"))
                .await().atMost(Duration.ofSeconds(3));
        RestResponse<String> missing = McpResourceSupport.okOrNotFound(Uni.createFrom().<String>nullItem())
                .await().atMost(Duration.ofSeconds(3));

        assertEquals(200, found.getStatus());
        assertEquals("done", found.getEntity());
        assertEquals(404, missing.getStatus());
    }
}
