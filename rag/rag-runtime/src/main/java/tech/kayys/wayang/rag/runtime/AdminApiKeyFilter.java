package tech.kayys.wayang.rag.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;

@Provider
@AdminProtected
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class AdminApiKeyFilter implements ContainerRequestFilter {

    static final String ADMIN_KEY_HEADER = "x-admin-key";
    static final String ADMIN_KEY_SLOT_HEADER = "X-Admin-Key-Slot";
    static final String ADMIN_KEY_SLOT_PROPERTY = "admin.key.slot";
    static final String SLOT_PRIMARY = "primary";
    static final String SLOT_SECONDARY = "secondary";

    @ConfigProperty(name = "rag.runtime.admin.api-key", defaultValue = "")
    String configuredAdminKey;

    @ConfigProperty(name = "rag.runtime.admin.api-key-secondary", defaultValue = "")
    String configuredAdminKeySecondary;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!hasConfiguredKeys()) {
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .entity("Admin API key is not configured")
                    .build());
            return;
        }

        String provided = requestContext.getHeaderString(ADMIN_KEY_HEADER);
        String slot = resolveSlot(provided);
        if (slot == null) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid admin API key")
                    .header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey")
                    .build());
            return;
        }
        requestContext.setProperty(ADMIN_KEY_SLOT_PROPERTY, slot);
    }

    private boolean hasConfiguredKeys() {
        return !isBlank(configuredAdminKey) || !isBlank(configuredAdminKeySecondary);
    }

    private String resolveSlot(String provided) {
        if (isBlank(provided)) {
            return null;
        }
        if (!isBlank(configuredAdminKey) && configuredAdminKey.equals(provided)) {
            return SLOT_PRIMARY;
        }
        if (!isBlank(configuredAdminKeySecondary) && configuredAdminKeySecondary.equals(provided)) {
            return SLOT_SECONDARY;
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
