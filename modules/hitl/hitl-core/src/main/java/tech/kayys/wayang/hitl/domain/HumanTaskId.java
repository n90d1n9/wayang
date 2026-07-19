package tech.kayys.wayang.hitl.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * HumanTaskId - Strong-typed ID
 */
public record HumanTaskId(String value) {
    public HumanTaskId {
        Objects.requireNonNull(value, "HumanTaskId cannot be null");
    }

    public static HumanTaskId of(String value) {
        return new HumanTaskId(value);
    }

    public static HumanTaskId generate() {
        return new HumanTaskId("TASK-" + UUID.randomUUID());
    }
}