package tech.kayys.wayang.rag.config;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.rag.runtime.RagValidationErrorResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RagPluginConfigValidationExceptionMapperTest {

    @Test
    void shouldMapValidationExceptionToStructuredBadRequest() {
        RagPluginConfigValidationExceptionMapper mapper = new RagPluginConfigValidationExceptionMapper();
        RagPluginConfigValidationException ex = new RagPluginConfigValidationException(
                "invalid_entry",
                "tenantOrderOverrides",
                "tenant-a",
                "tenant-a",
                "Expected format `tenant=value`.");

        Response response = mapper.toResponse(ex);

        assertEquals(400, response.getStatus());
        RagValidationErrorResponse payload = (RagValidationErrorResponse) response.getEntity();
        assertEquals("invalid_entry", payload.code());
        assertEquals("tenantOrderOverrides", payload.field());
        assertEquals("tenant-a", payload.tenantId());
        assertEquals("tenant-a", payload.value());
    }
}
