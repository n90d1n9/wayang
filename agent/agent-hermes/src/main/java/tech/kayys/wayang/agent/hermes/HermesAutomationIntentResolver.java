package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Extracts Hermes cron/background-work intent from request metadata and prompt text.
 */
public final class HermesAutomationIntentResolver {

    private static final List<String> SCHEDULE_KEYS = List.of(
            "hermes.automation.schedule",
            "automation.schedule",
            "automationSchedule",
            "schedule",
            "cron",
            "rrule",
            "interval",
            "repeat");

    private static final List<String> TASK_KEYS = List.of(
            "hermes.automation.task",
            "automation.task",
            "automationTask",
            "scheduledTask",
            "task");

    private static final List<String> TIMEZONE_KEYS = List.of(
            "hermes.automation.timezone",
            "automation.timezone",
            "timezone",
            "timeZone",
            "tz");

    private static final List<String> ENABLED_KEYS = List.of(
            "hermes.automation.enabled",
            "automation.enabled",
            "schedule.enabled",
            "scheduled",
            "recurring");

    private final HermesAgentModeConfig config;

    public HermesAutomationIntentResolver(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
    }

    public HermesAutomationIntent resolve(AgentRequest request) {
        HermesRequestValues values = HermesRequestValues.from(request);
        String prompt = values.prompt();
        String task = values.firstText(TASK_KEYS).orElse(prompt);
        String timezone = values.firstText(TIMEZONE_KEYS).orElse("");
        Optional<Boolean> requestedEnabled = values.firstBoolean(ENABLED_KEYS, "automation");
        Optional<String> explicitSchedule = values.firstText(SCHEDULE_KEYS);

        if (!config.cronEnabled()) {
            String schedule = explicitSchedule.orElseGet(() -> HermesPromptSignals.inferAutomationSchedule(prompt).orElse(""));
            String scheduleType = schedule.isBlank() ? "none" : scheduleType(schedule);
            return new HermesAutomationIntent(
                    false,
                    false,
                    schedule,
                    scheduleType,
                    task,
                    timezone,
                    "disabled",
                    recurring(schedule, scheduleType),
                    "cron automation disabled");
        }

        if (requestedEnabled.isPresent() && !requestedEnabled.orElseThrow()) {
            String schedule = explicitSchedule.orElse("");
            String scheduleType = schedule.isBlank() ? "none" : scheduleType(schedule);
            return new HermesAutomationIntent(
                    true,
                    false,
                    schedule,
                    scheduleType,
                    task,
                    timezone,
                    "explicit",
                    recurring(schedule, scheduleType),
                    "automation disabled for request");
        }

        if (explicitSchedule.isPresent()) {
            String schedule = explicitSchedule.orElseThrow();
            String scheduleType = scheduleType(schedule);
            return new HermesAutomationIntent(
                    true,
                    true,
                    schedule,
                    scheduleType,
                    task,
                    timezone,
                    "explicit",
                    recurring(schedule, scheduleType),
                    "explicit schedule provided");
        }

        Optional<String> inferred = HermesPromptSignals.inferAutomationSchedule(prompt);
        if (inferred.isPresent()) {
            String schedule = inferred.orElseThrow();
            String scheduleType = scheduleType(schedule);
            return new HermesAutomationIntent(
                    true,
                    true,
                    schedule,
                    scheduleType,
                    task,
                    timezone,
                    "prompt",
                    true,
                    "recurring schedule inferred from prompt");
        }

        return new HermesAutomationIntent(
                true,
                false,
                "",
                "none",
                task,
                timezone,
                "none",
                false,
                "no automation schedule detected");
    }

    private static String scheduleType(String schedule) {
        String trimmed = schedule == null ? "" : schedule.trim();
        String normalized = HermesRequestValues.normalizeText(trimmed);
        if (trimmed.isBlank()) {
            return "none";
        }
        if (trimmed.toUpperCase(Locale.ROOT).startsWith("FREQ=")) {
            return "rrule";
        }
        if (isCronExpression(trimmed)) {
            return "cron";
        }
        if (trimmed.toUpperCase(Locale.ROOT).matches("P(T)?\\d+.*")) {
            return "interval";
        }
        if (normalized.startsWith("every ")) {
            return "interval";
        }
        return "natural-language";
    }

    private static boolean recurring(String schedule, String scheduleType) {
        String normalized = HermesRequestValues.normalizeText(schedule);
        if (normalized.isBlank()
                || HermesRequestValues.containsAny(normalized, "once", "one time", "one-time", "tomorrow")) {
            return false;
        }
        if ("cron".equals(scheduleType) || "rrule".equals(scheduleType) || "interval".equals(scheduleType)) {
            return true;
        }
        return HermesRequestValues.containsAny(normalized, "daily", "nightly", "weekly", "monthly", "hourly", "every ");
    }

    private static boolean isCronExpression(String schedule) {
        String trimmed = schedule.trim();
        if (trimmed.startsWith("@")) {
            return true;
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length != 5 && parts.length != 6) {
            return false;
        }
        for (String part : parts) {
            if (!part.matches("[A-Za-z0-9*/?,#L\\-]+")) {
                return false;
            }
        }
        return true;
    }
}
