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
        String value = AdminApiKeyAccess.responseSlot(
                requestContext.getProperty(AdminApiKeyFilter.ADMIN_KEY_SLOT_PROPERTY));
        if (value == null) {
            return;
        }
        responseContext.getHeaders().putSingle(AdminApiKeyFilter.ADMIN_KEY_SLOT_HEADER, value);
    }
}
