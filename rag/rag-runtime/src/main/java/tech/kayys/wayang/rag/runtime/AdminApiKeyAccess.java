package tech.kayys.wayang.rag.runtime;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

final class AdminApiKeyAccess {

    static final String ADMIN_KEY_HEADER = "x-admin-key";
    static final String ADMIN_KEY_SLOT_HEADER = "X-Admin-Key-Slot";
    static final String ADMIN_KEY_SLOT_PROPERTY = "admin.key.slot";
    static final String SLOT_PRIMARY = "primary";
    static final String SLOT_SECONDARY = "secondary";

    private AdminApiKeyAccess() {
    }

    static boolean hasConfiguredKeys(String primaryKey, String secondaryKey) {
        return !isBlank(primaryKey) || !isBlank(secondaryKey);
    }

    static String resolveSlot(String provided, String primaryKey, String secondaryKey) {
        if (isBlank(provided)) {
            return null;
        }
        if (!isBlank(primaryKey) && primaryKey.equals(provided)) {
            return SLOT_PRIMARY;
        }
        if (!isBlank(secondaryKey) && secondaryKey.equals(provided)) {
            return SLOT_SECONDARY;
        }
        return null;
    }

    static String responseSlot(Object slot) {
        if (slot == null) {
            return null;
        }
        String value = String.valueOf(slot);
        return value.isBlank() ? null : value;
    }

    static Response notConfiguredResponse() {
        return Response.status(Response.Status.FORBIDDEN)
                .entity("Admin API key is not configured")
                .build();
    }

    static Response invalidKeyResponse() {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity("Invalid admin API key")
                .header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey")
                .build();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
