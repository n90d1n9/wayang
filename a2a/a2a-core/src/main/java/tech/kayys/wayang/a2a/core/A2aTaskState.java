package tech.kayys.wayang.a2a.core;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * ProtoJSON task lifecycle state values.
 */
public enum A2aTaskState {
    TASK_STATE_UNSPECIFIED,
    TASK_STATE_SUBMITTED,
    TASK_STATE_WORKING,
    TASK_STATE_COMPLETED,
    TASK_STATE_FAILED,
    TASK_STATE_CANCELED,
    TASK_STATE_INPUT_REQUIRED,
    TASK_STATE_REJECTED,
    TASK_STATE_AUTH_REQUIRED;

    private static final Set<A2aTaskState> TERMINAL = EnumSet.of(
            TASK_STATE_COMPLETED,
            TASK_STATE_FAILED,
            TASK_STATE_CANCELED,
            TASK_STATE_REJECTED);

    private static final Set<A2aTaskState> INTERRUPTED = EnumSet.of(
            TASK_STATE_INPUT_REQUIRED,
            TASK_STATE_AUTH_REQUIRED);

    public String value() {
        return name();
    }

    public boolean terminal() {
        return TERMINAL.contains(this);
    }

    public boolean interrupted() {
        return INTERRUPTED.contains(this);
    }

    public static A2aTaskState fromValue(String value) {
        if (value == null || value.isBlank()) {
            return TASK_STATE_UNSPECIFIED;
        }
        return Arrays.stream(values())
                .filter(state -> state.name().equals(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported A2A task state: " + value));
    }
}
