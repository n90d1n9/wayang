package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Optional;

/**
 * Resolves trajectory export intent for observability and audit adapters.
 */
public final class HermesTrajectoryExportResolver {

    private static final List<String> EXPORT_KEYS = List.of(
            "hermes.trajectory.export",
            "hermes.trajectoryExport",
            "trajectory.export",
            "trajectoryExport",
            "exportTrajectory",
            "trace.export",
            "exportTrace");

    private static final List<String> FORMAT_KEYS = List.of(
            "hermes.trajectory.format",
            "trajectory.format",
            "trace.format",
            "export.format");

    private static final List<String> DESTINATION_KEYS = List.of(
            "hermes.trajectory.destination",
            "trajectory.destination",
            "trace.destination",
            "export.destination");

    private static final List<String> INCLUDE_PROMPTS_KEYS = List.of(
            "hermes.trajectory.includePrompts",
            "trajectory.includePrompts",
            "trace.includePrompts",
            "includePrompts");

    private static final List<String> INCLUDE_TOOL_CALLS_KEYS = List.of(
            "hermes.trajectory.includeToolCalls",
            "trajectory.includeToolCalls",
            "trace.includeToolCalls",
            "includeToolCalls");

    private static final List<String> REDACT_KEYS = List.of(
            "hermes.trajectory.redactSensitive",
            "trajectory.redactSensitive",
            "trace.redactSensitive",
            "redactSensitive");

    private final HermesAgentModeConfig config;

    public HermesTrajectoryExportResolver(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
    }

    public HermesTrajectoryExportPlan resolve(AgentRequest request) {
        HermesRequestValues values = HermesRequestValues.from(request);
        String prompt = values.prompt();
        Optional<Boolean> explicitExport = values.firstBoolean(EXPORT_KEYS, "trajectory export");
        Optional<String> explicitFormat = values.firstText(FORMAT_KEYS);
        Optional<String> explicitDestination = values.firstText(DESTINATION_KEYS);
        boolean includePrompts = values.firstBoolean(INCLUDE_PROMPTS_KEYS, "trajectory export").orElse(false);
        boolean includeToolCalls = values.firstBoolean(INCLUDE_TOOL_CALLS_KEYS, "trajectory export").orElse(true);
        boolean redactSensitive = values.firstBoolean(REDACT_KEYS, "trajectory export").orElse(true);
        String format = format(explicitFormat.orElse("jsonl"));
        String destination = destination(explicitDestination.orElse("local"));

        if (!config.trajectoryExportEnabled()) {
            return new HermesTrajectoryExportPlan(
                    false,
                    explicitExport.orElse(false) || explicitFormat.isPresent()
                            || explicitDestination.isPresent()
                            || HermesPromptSignals.suggestsTrajectoryExport(prompt),
                    false,
                    format,
                    "none",
                    includePrompts,
                    includeToolCalls,
                    redactSensitive,
                    "disabled",
                    "trajectory export disabled");
        }

        if (explicitExport.isPresent() && !explicitExport.orElseThrow()) {
            return new HermesTrajectoryExportPlan(
                    true,
                    false,
                    false,
                    format,
                    "none",
                    includePrompts,
                    includeToolCalls,
                    redactSensitive,
                    "explicit",
                    "trajectory export disabled for request");
        }

        if ("none".equals(destination)) {
            return new HermesTrajectoryExportPlan(
                    true,
                    false,
                    false,
                    format,
                    "none",
                    includePrompts,
                    includeToolCalls,
                    redactSensitive,
                    explicitDestination.isPresent() ? "explicit" : "none",
                    explicitDestination.isPresent()
                            ? "trajectory export destination disabled"
                            : "no trajectory export requested");
        }

        if (explicitExport.orElse(false) || explicitFormat.isPresent() || explicitDestination.isPresent()) {
            return new HermesTrajectoryExportPlan(
                    true,
                    true,
                    true,
                    format,
                    destination,
                    includePrompts,
                    includeToolCalls,
                    redactSensitive,
                    "explicit",
                    "explicit trajectory export requested");
        }

        if (HermesPromptSignals.suggestsTrajectoryExport(prompt)) {
            return new HermesTrajectoryExportPlan(
                    true,
                    true,
                    true,
                    format,
                    destination,
                    includePrompts,
                    includeToolCalls,
                    redactSensitive,
                    "prompt",
                    "trajectory export inferred from prompt");
        }

        return new HermesTrajectoryExportPlan(
                true,
                false,
                false,
                format,
                "none",
                includePrompts,
                includeToolCalls,
                redactSensitive,
                "none",
                "no trajectory export requested");
    }

    public HermesTrajectoryExportPlan defaultPlan() {
        return resolve(null);
    }

    private static String format(String value) {
        return switch (HermesRequestValues.normalize(value)) {
            case "json" -> "json";
            case "md", "markdown" -> "markdown";
            case "otel", "opentelemetry" -> "opentelemetry";
            default -> "jsonl";
        };
    }

    private static String destination(String value) {
        return switch (HermesRequestValues.normalize(value)) {
            case "none", "off", "disabled", "false" -> "none";
            case "db", "database" -> "database";
            case "object", "objectstore", "objectstorage", "s3", "rustfs" -> "object-storage";
            case "stdout", "console" -> "stdout";
            default -> "local";
        };
    }
}
