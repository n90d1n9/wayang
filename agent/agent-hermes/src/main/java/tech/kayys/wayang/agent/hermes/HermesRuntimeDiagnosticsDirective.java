package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter-neutral instruction for inspecting Hermes runtime diagnostics.
 */
public record HermesRuntimeDiagnosticsDirective(
        boolean active,
        String operation,
        String target,
        String view,
        String reason) {

    public static final String VIEW_FULL = "full";
    public static final String VIEW_SUMMARY = "summary";
    public static final String VIEW_CAPABILITIES = "capabilities";
    public static final String VIEW_LIFECYCLE = "lifecycle";
    public static final String VIEW_RUNTIME_PORTS = "runtime-ports";
    public static final String VIEW_SKILL_PERSISTENCE = "skill-persistence";
    public static final String VIEW_LEARNING_AUDIT = "learning-audit";

    public HermesRuntimeDiagnosticsDirective {
        view = normalizeView(view);
        operation = HermesDirectiveSupport.clean(operation, active ? "inspect" : "none");
        target = HermesDirectiveSupport.clean(target, active ? "runtime-diagnostics:" + view : "");
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "runtime diagnostics inspection requested" : "runtime diagnostics inspection inactive");
    }

    public static HermesRuntimeDiagnosticsDirective full() {
        return inspect(VIEW_FULL);
    }

    public static HermesRuntimeDiagnosticsDirective summary() {
        return inspect(VIEW_SUMMARY);
    }

    public static HermesRuntimeDiagnosticsDirective capabilities() {
        return inspect(VIEW_CAPABILITIES);
    }

    public static HermesRuntimeDiagnosticsDirective lifecycle() {
        return inspect(VIEW_LIFECYCLE);
    }

    public static HermesRuntimeDiagnosticsDirective runtimePorts() {
        return inspect(VIEW_RUNTIME_PORTS);
    }

    public static HermesRuntimeDiagnosticsDirective skillPersistence() {
        return inspect(VIEW_SKILL_PERSISTENCE);
    }

    public static HermesRuntimeDiagnosticsDirective learningAudit() {
        return inspect(VIEW_LEARNING_AUDIT);
    }

    public static HermesRuntimeDiagnosticsDirective inspect(String view) {
        String normalized = normalizeView(view);
        return new HermesRuntimeDiagnosticsDirective(
                true,
                "inspect",
                "runtime-diagnostics:" + normalized,
                normalized,
                "runtime diagnostics inspection requested");
    }

    public static HermesRuntimeDiagnosticsDirective none() {
        return new HermesRuntimeDiagnosticsDirective(
                false,
                "none",
                "",
                VIEW_SUMMARY,
                "runtime diagnostics inspection inactive");
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("target", target);
        metadata.put("view", view);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }

    private static String normalizeView(String value) {
        String normalized = HermesDirectiveSupport.clean(value, VIEW_FULL)
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
        return switch (normalized) {
            case VIEW_SUMMARY,
                    VIEW_CAPABILITIES,
                    VIEW_LIFECYCLE,
                    VIEW_RUNTIME_PORTS,
                    VIEW_SKILL_PERSISTENCE,
                    VIEW_LEARNING_AUDIT ->
                    normalized;
            default -> VIEW_FULL;
        };
    }
}
