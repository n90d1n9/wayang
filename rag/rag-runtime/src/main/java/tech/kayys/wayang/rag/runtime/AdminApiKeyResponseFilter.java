package tech.kayys.wayang.rag.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@AdminProtected
@Priority(Priorities.HEADER_DECORATOR)
@ApplicationScoped
public class AdminApiKeyResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object slot = requestContext.getProperty(AdminApiKeyFilter.ADMIN_KEY_SLOT_PROPERTY);
        if (slot == null) {
            return;
        }
        String value = String.valueOf(slot);
        if (value.isBlank()) {
            return;
        }
        responseContext.getHeaders().putSingle(AdminApiKeyFilter.ADMIN_KEY_SLOT_HEADER, value);
    }
}
