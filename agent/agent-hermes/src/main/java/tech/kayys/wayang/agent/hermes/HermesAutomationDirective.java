package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter-neutral command for scheduler services to register Hermes automation.
 */
public record HermesAutomationDirective(
        boolean schedulerEnabled,
        boolean scheduled,
        boolean active,
        String operation,
        String taskId,
        String task,
        String schedule,
        String scheduleType,
        String timezone,
        boolean recurring,
        String requestId,
        String tenantId,
        String sessionId,
        String userId,
        String source,
        String reason) {

    public HermesAutomationDirective {
        operation = HermesDirectiveSupport.clean(operation, active ? "register" : "none");
        taskId = HermesDirectiveSupport.clean(taskId, "");
        task = HermesDirectiveSupport.clean(task, "");
        schedule = HermesDirectiveSupport.clean(schedule, "");
        scheduleType = HermesDirectiveSupport.clean(scheduleType, "none");
        timezone = HermesDirectiveSupport.clean(timezone, "");
        requestId = HermesDirectiveSupport.clean(requestId, "");
        tenantId = HermesDirectiveSupport.clean(tenantId, "default");
        sessionId = HermesDirectiveSupport.clean(sessionId, "");
        userId = HermesDirectiveSupport.clean(userId, "");
        source = HermesDirectiveSupport.clean(source, "none");
        reason = HermesDirectiveSupport.clean(
                reason,
                active ? "automation schedule requested" : "no automation schedule detected");
    }

    public static HermesAutomationDirective from(HermesAutomationIntent intent, AgentRequest request) {
        HermesAutomationIntent effectiveIntent = intent == null
                ? new HermesAutomationIntent(false, false, "", "none", "", "", "none", false, "")
                : intent;
        HermesDirectiveSupport.Identity identity = HermesDirectiveSupport.identity(request);
        boolean active = effectiveIntent.active();
        return new HermesAutomationDirective(
                effectiveIntent.schedulerEnabled(),
                effectiveIntent.scheduled(),
                active,
                active ? "register" : "none",
                active ? taskId(effectiveIntent, identity) : "",
                effectiveIntent.task(),
                effectiveIntent.schedule(),
                effectiveIntent.scheduleType(),
                effectiveIntent.timezone(),
                effectiveIntent.recurring(),
                identity.requestId(),
                identity.tenantId(),
                identity.sessionId(),
                identity.userId(),
                effectiveIntent.source(),
                effectiveIntent.reason());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("schedulerEnabled", schedulerEnabled);
        metadata.put("scheduled", scheduled);
        metadata.put("active", active);
        metadata.put("operation", operation);
        metadata.put("taskId", taskId);
        metadata.put("task", task);
        metadata.put("schedule", schedule);
        metadata.put("scheduleType", scheduleType);
        metadata.put("timezone", timezone);
        metadata.put("recurring", recurring);
        metadata.put("requestId", requestId);
        metadata.put("tenantId", tenantId);
        metadata.put("sessionId", sessionId);
        metadata.put("userId", userId);
        metadata.put("source", source);
        metadata.put("reason", reason);
        return Map.copyOf(metadata);
    }

    private static String taskId(HermesAutomationIntent intent, HermesDirectiveSupport.Identity identity) {
        String base = identity.requestId();
        if (base.isBlank()) {
            base = HermesDirectiveSupport.hashBase(
                    intent.task(),
                    intent.schedule(),
                    identity.tenantId());
        }
        return HermesDirectiveSupport.prefixedId("hermes-automation", base, "task");
    }
}
