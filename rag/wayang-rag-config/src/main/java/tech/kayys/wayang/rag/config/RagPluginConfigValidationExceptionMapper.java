package tech.kayys.wayang.rag.config;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import tech.kayys.wayang.rag.runtime.RagValidationErrorResponse;

@Provider
public class RagPluginConfigValidationExceptionMapper implements ExceptionMapper<RagPluginConfigValidationException> {

    @Override
    public Response toResponse(RagPluginConfigValidationException exception) {
        RagValidationErrorResponse payload = new RagValidationErrorResponse(
                exception.getCode(),
                exception.getField(),
                exception.getTenantId(),
                exception.getValue(),
                exception.getMessage());
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .build();
    }
}
