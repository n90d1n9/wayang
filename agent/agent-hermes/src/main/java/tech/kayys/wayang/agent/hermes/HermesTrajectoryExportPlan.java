package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Advisory trace/export contract for Hermes trajectories.
 */
public record HermesTrajectoryExportPlan(
        boolean exportEnabled,
        boolean requested,
        boolean export,
        String format,
        String destination,
        boolean includePrompts,
        boolean includeToolCalls,
        boolean redactSensitive,
        String source,
        String reason) {

    public HermesTrajectoryExportPlan {
        format = HermesText.trimOr(format, "jsonl");
        destination = HermesText.trimOr(destination, export ? "local" : "none");
        source = HermesText.trimOr(source, "none");
        reason = HermesText.trimOr(reason, "no trajectory export requested");
    }

    public boolean active() {
        return exportEnabled && export && !"none".equalsIgnoreCase(destination);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("exportEnabled", exportEnabled);
        metadata.put("requested", requested);
        metadata.put("export", export);
        metadata.put("active", active());
        metadata.put("format", format);
        metadata.put("destination", destination);
        metadata.put("includePrompts", includePrompts);
        metadata.put("includeToolCalls", includeToolCalls);
        metadata.put("redactSensitive", redactSensitive);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }
}
