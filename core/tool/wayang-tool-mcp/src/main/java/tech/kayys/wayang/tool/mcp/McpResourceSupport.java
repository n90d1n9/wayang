package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import tech.kayys.wayang.tool.dto.ToolRequestContext;

import java.util.function.Supplier;

final class McpResourceSupport {

    private McpResourceSupport() {
    }

    static String currentRequestId(ToolRequestContext requestContext) {
        return requestContext == null ? null : requestContext.getCurrentRequestId();
    }

    static <T> T beanParam(
            T value,
            Supplier<T> fallback) {
        return value == null ? fallback.get() : value;
    }

    static <T> RestResponse<T> okOrNotFound(T entity) {
        return entity == null
                ? RestResponse.status(Response.Status.NOT_FOUND)
                : RestResponse.ok(entity);
    }

    static <T> Uni<RestResponse<T>> ok(Uni<T> entity) {
        return entity.map(RestResponse::ok);
    }

    static <T> Uni<RestResponse<T>> okOrNotFound(Uni<T> entity) {
        return entity.map(McpResourceSupport::okOrNotFound);
    }
}
