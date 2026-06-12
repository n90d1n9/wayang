package tech.kayys.wayang.agent.api;

import tech.kayys.wayang.agent.hermes.HermesRuntimeDiagnosticsDirective;

import java.util.Locale;

/**
 * Maps Hermes diagnostics request parameters to normalized diagnostics views.
 */
final class HermesDiagnosticsViewMapper {

    String view(HermesDiagnosticsRequest request) {
        HermesDiagnosticsRequest resolved = request == null ? new HermesDiagnosticsRequest() : request;
        if (resolved.view() == null || resolved.view().isBlank()) {
            return HermesRuntimeDiagnosticsDirective.VIEW_SUMMARY;
        }
        return normalize(resolved.view());
    }

    String capabilities() {
        return HermesRuntimeDiagnosticsDirective.VIEW_CAPABILITIES;
    }

    String lifecycle() {
        return HermesRuntimeDiagnosticsDirective.VIEW_LIFECYCLE;
    }

    String runtimePorts() {
        return HermesRuntimeDiagnosticsDirective.VIEW_RUNTIME_PORTS;
    }

    String skillPersistence() {
        return HermesRuntimeDiagnosticsDirective.VIEW_SKILL_PERSISTENCE;
    }

    String learningAudit() {
        return HermesRuntimeDiagnosticsDirective.VIEW_LEARNING_AUDIT;
    }

    private String normalize(String value) {
        String normalized = value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
        return switch (normalized) {
            case HermesRuntimeDiagnosticsDirective.VIEW_SUMMARY,
                    HermesRuntimeDiagnosticsDirective.VIEW_CAPABILITIES,
                    HermesRuntimeDiagnosticsDirective.VIEW_LIFECYCLE,
                    HermesRuntimeDiagnosticsDirective.VIEW_RUNTIME_PORTS,
                    HermesRuntimeDiagnosticsDirective.VIEW_SKILL_PERSISTENCE,
                    HermesRuntimeDiagnosticsDirective.VIEW_LEARNING_AUDIT ->
                    normalized;
            default -> HermesRuntimeDiagnosticsDirective.VIEW_FULL;
        };
    }
}
