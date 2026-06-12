package tech.kayys.wayang.agent.api;

/**
 * Shared user-facing Hermes operational messages.
 */
final class HermesOperationalMessages {

    static final String MISSING_DIAGNOSTICS_PORT =
            "Hermes runtime diagnostics port is not configured";

    static final String MISSING_JOURNAL_PORT =
            "Hermes runtime journal port is not configured";

    static final String MISSING_LEARNING_AUDIT_PORT =
            "Hermes learning audit port is not configured";

    private HermesOperationalMessages() {
    }
}
