package tech.kayys.wayang.a2a.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client preferences for SendMessage and SendStreamingMessage requests.
 */
public record A2aSendMessageConfiguration(
        List<String> acceptedOutputModes,
        Map<String, Object> taskPushNotificationConfig,
        Integer historyLength,
        Boolean returnImmediately) {

    public A2aSendMessageConfiguration {
        acceptedOutputModes = A2aValues.stringList(acceptedOutputModes, "acceptedOutputModes");
        taskPushNotificationConfig = A2aValues.copyMap(taskPushNotificationConfig);
        if (historyLength != null && historyLength < 0) {
            throw new IllegalArgumentException("historyLength must be >= 0");
        }
    }

    public static A2aSendMessageConfiguration textOutput() {
        return new A2aSendMessageConfiguration(List.of("text/plain"), Map.of(), null, null);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        A2aValues.putOptional(payload, "acceptedOutputModes", acceptedOutputModes);
        A2aValues.putOptional(payload, "taskPushNotificationConfig", taskPushNotificationConfig);
        A2aValues.putOptional(payload, "historyLength", historyLength);
        A2aValues.putOptional(payload, "returnImmediately", returnImmediately);
        return A2aValues.copyMap(payload);
    }

    public static A2aSendMessageConfiguration fromMap(Map<?, ?> payload) {
        Map<String, Object> source = A2aValues.copyMap(payload);
        return new A2aSendMessageConfiguration(
                A2aValues.stringList(source.get("acceptedOutputModes"), "acceptedOutputModes"),
                A2aValues.objectOrEmpty(source, "taskPushNotificationConfig"),
                A2aValues.optionalInteger(source, "historyLength"),
                A2aValues.optionalBoolean(source, "returnImmediately"));
    }
}
