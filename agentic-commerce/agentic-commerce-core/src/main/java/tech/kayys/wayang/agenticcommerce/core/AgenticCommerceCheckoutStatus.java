package tech.kayys.wayang.agenticcommerce.core;

import java.util.Locale;
import java.util.Set;

/**
 * Checkout status vocabulary used by Agentic Commerce checkout session DTOs.
 */
public final class AgenticCommerceCheckoutStatus {

    public static final String OPEN = "open";
    public static final String REQUIRES_ACTION = "requires_action";
    public static final String INCOMPLETE = "incomplete";
    public static final String NOT_READY_FOR_PAYMENT = "not_ready_for_payment";
    public static final String REQUIRES_ESCALATION = "requires_escalation";
    public static final String AUTHENTICATION_REQUIRED = "authentication_required";
    public static final String READY_FOR_PAYMENT = "ready_for_payment";
    public static final String PENDING_APPROVAL = "pending_approval";
    public static final String COMPLETE_IN_PROGRESS = "complete_in_progress";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String CANCELED = "canceled";
    public static final String EXPIRED = "expired";
    public static final String UNKNOWN = "unknown";

    private static final Set<String> KNOWN = Set.of(
            OPEN,
            REQUIRES_ACTION,
            INCOMPLETE,
            NOT_READY_FOR_PAYMENT,
            REQUIRES_ESCALATION,
            AUTHENTICATION_REQUIRED,
            READY_FOR_PAYMENT,
            PENDING_APPROVAL,
            COMPLETE_IN_PROGRESS,
            IN_PROGRESS,
            COMPLETED,
            CANCELED,
            EXPIRED);

    private AgenticCommerceCheckoutStatus() {
    }

    public static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[\\s-]+", "_").replaceAll("_+", "_");
        if ("cancelled".equals(normalized)) {
            return CANCELED;
        }
        return normalized.isBlank() ? UNKNOWN : normalized;
    }

    public static String normalizeOptional(String value) {
        String normalized = normalize(value);
        return UNKNOWN.equals(normalized) && (value == null || value.trim().isBlank()) ? "" : normalized;
    }

    public static boolean known(String value) {
        return KNOWN.contains(normalize(value));
    }
}
