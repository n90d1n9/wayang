package tech.kayys.wayang.agent.skills.management;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Best-effort event recorder that keeps observability hooks non-invasive.
 */
final class SkillManagementEventRecorder {

    private final SkillManagementEventSink sink;

    SkillManagementEventRecorder(SkillManagementEventSink sink) {
        this.sink = Objects.requireNonNullElseGet(sink, SkillManagementEventSink::noop);
    }

    void success(
            SkillManagementEventOperation operation,
            String skillId) {
        success(operation, skillId, Map.of());
    }

    void success(
            SkillManagementEventOperation operation,
            String skillId,
            SkillManagementOperationContext context) {
        success(operation, skillId, Map.of(), context);
    }

    void success(
            SkillManagementEventOperation operation,
            String skillId,
            Map<String, String> attributes) {
        record(operation, skillId, true, attributes);
    }

    void success(
            SkillManagementEventOperation operation,
            String skillId,
            Map<String, String> attributes,
            SkillManagementOperationContext context) {
        record(operation, skillId, true, SkillManagementEventAttributes.withContext(attributes, context));
    }

    void failure(
            SkillManagementEventOperation operation,
            String skillId,
            RuntimeException error) {
        failure(operation, skillId, error, Map.of());
    }

    void failure(
            SkillManagementEventOperation operation,
            String skillId,
            RuntimeException error,
            Map<String, String> attributes) {
        record(operation, skillId, false, SkillManagementEventAttributes.failure(error, attributes));
    }

    void failure(
            SkillManagementEventOperation operation,
            String skillId,
            RuntimeException error,
            SkillManagementOperationContext context) {
        failure(operation, skillId, error, Map.of(), context);
    }

    void failure(
            SkillManagementEventOperation operation,
            String skillId,
            RuntimeException error,
            Map<String, String> attributes,
            SkillManagementOperationContext context) {
        record(operation, skillId, false, SkillManagementEventAttributes.failure(error, attributes, context));
    }

    <T> T recordOperation(
            SkillManagementEventOperation operation,
            String skillId,
            SkillManagementOperationContext context,
            Supplier<T> action,
            Function<T, Map<String, String>> successAttributes) {
        return recordOperation(
                operation,
                skillId,
                context,
                action,
                successAttributes,
                error -> Map.of(),
                Function.identity());
    }

    <T> T recordOperation(
            SkillManagementEventOperation operation,
            String skillId,
            SkillManagementOperationContext context,
            Supplier<T> action,
            Function<T, Map<String, String>> successAttributes,
            Function<RuntimeException, Map<String, String>> failureAttributes) {
        return recordOperation(
                operation,
                skillId,
                context,
                action,
                successAttributes,
                failureAttributes,
                Function.identity());
    }

    <T> T recordOperation(
            SkillManagementEventOperation operation,
            String skillId,
            SkillManagementOperationContext context,
            Supplier<T> action,
            Function<T, Map<String, String>> successAttributes,
            Function<RuntimeException, Map<String, String>> failureAttributes,
            Function<RuntimeException, RuntimeException> errorMapper) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(successAttributes, "successAttributes");
        Objects.requireNonNull(failureAttributes, "failureAttributes");
        Objects.requireNonNull(errorMapper, "errorMapper");
        try {
            T result = action.get();
            success(operation, skillId, attributesOrEmpty(successAttributes.apply(result)), context);
            return result;
        } catch (RuntimeException error) {
            failure(operation, skillId, error, attributesOrEmpty(failureAttributes.apply(error)), context);
            RuntimeException mapped = errorMapper.apply(error);
            throw mapped == null ? error : mapped;
        }
    }

    private void record(
            SkillManagementEventOperation operation,
            String skillId,
            boolean success,
            Map<String, String> attributes) {
        try {
            sink.record(new SkillManagementEvent(Instant.now(), operation, skillId, success, attributes));
        } catch (RuntimeException ignored) {
            // Observability must never change skill-management behavior.
        }
    }

    private static Map<String, String> attributesOrEmpty(Map<String, String> attributes) {
        return attributes == null ? Map.of() : attributes;
    }

}
