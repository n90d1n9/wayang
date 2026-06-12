package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Advisory scheduling contract for Hermes background automation.
 */
public record HermesAutomationIntent(
        boolean schedulerEnabled,
        boolean scheduled,
        String schedule,
        String scheduleType,
        String task,
        String timezone,
        String source,
        boolean recurring,
        String reason) {

    public HermesAutomationIntent {
        schedule = HermesText.trimOr(schedule, "");
        scheduleType = HermesText.trimOr(scheduleType, "none");
        task = HermesText.trimOr(task, "");
        timezone = HermesText.trimOr(timezone, "");
        source = HermesText.trimOr(source, "none");
        reason = HermesText.trimOr(reason, "no automation schedule detected");
    }

    public boolean active() {
        return schedulerEnabled && scheduled && !schedule.isBlank();
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("schedulerEnabled", schedulerEnabled);
        metadata.put("scheduled", scheduled);
        metadata.put("active", active());
        metadata.put("schedule", schedule);
        metadata.put("scheduleType", scheduleType);
        metadata.put("task", task);
        metadata.put("timezone", timezone);
        metadata.put("source", source);
        metadata.put("recurring", recurring);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }
}
