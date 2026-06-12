package tech.kayys.wayang.rag.runtime;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;

@Provider
@AdminProtected
@Priority(Priorities.AUTHENTICATION)
@ApplicationScoped
public class AdminApiKeyFilter implements ContainerRequestFilter {

    static final String ADMIN_KEY_HEADER = AdminApiKeyAccess.ADMIN_KEY_HEADER;
    static final String ADMIN_KEY_SLOT_HEADER = AdminApiKeyAccess.ADMIN_KEY_SLOT_HEADER;
    static final String ADMIN_KEY_SLOT_PROPERTY = AdminApiKeyAccess.ADMIN_KEY_SLOT_PROPERTY;
    static final String SLOT_PRIMARY = AdminApiKeyAccess.SLOT_PRIMARY;
    static final String SLOT_SECONDARY = AdminApiKeyAccess.SLOT_SECONDARY;

    @ConfigProperty(name = "rag.runtime.admin.api-key", defaultValue = "")
    String configuredAdminKey;

    @ConfigProperty(name = "rag.runtime.admin.api-key-secondary", defaultValue = "")
    String configuredAdminKeySecondary;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!AdminApiKeyAccess.hasConfiguredKeys(configuredAdminKey, configuredAdminKeySecondary)) {
            requestContext.abortWith(AdminApiKeyAccess.notConfiguredResponse());
            return;
        }

        String provided = requestContext.getHeaderString(ADMIN_KEY_HEADER);
        String slot = AdminApiKeyAccess.resolveSlot(provided, configuredAdminKey, configuredAdminKeySecondary);
        if (slot == null) {
            requestContext.abortWith(AdminApiKeyAccess.invalidKeyResponse());
            return;
        }
        requestContext.setProperty(ADMIN_KEY_SLOT_PROPERTY, slot);
    }
}
