package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.core.A2uiClientMessage;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.a2ui.wayang.action.ActionContextReader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Mutable continuity state for one A2UI session.
 */
public final class WayangA2uiSessionState {

    private final Map<String, Long> eventCursors = new LinkedHashMap<>();

    public synchronized A2uiClientMessage apply(A2uiClientMessage message) {
        if (message instanceof A2uiUserAction action
                && WayangA2uiActions.RUN_EVENTS.equals(action.name())
                && !ActionContextReader.hasValue(action, "afterSequence")) {
            String runId = ActionContextReader.text(action, "runId");
            OptionalLong cursor = eventCursor(runId);
            if (cursor.isPresent() && cursor.getAsLong() > 0) {
                return ActionContextReader.withValue(action, "afterSequence", cursor.getAsLong());
            }
        }
        return message;
    }

    public synchronized void observe(WayangA2uiActionResult result) {
        if (result == null
                || !result.handled()
                || !WayangA2uiActions.RUN_EVENTS.equals(result.actionName())
                || result.runId().isBlank()) {
            return;
        }
        Object next = result.metadata().get("nextAfterSequence");
        if (next instanceof Number number) {
            rememberEventCursor(result.runId(), number.longValue());
        } else if (next instanceof String text && !text.isBlank()) {
            rememberEventCursor(result.runId(), Long.parseLong(text.trim()));
        }
    }

    public synchronized void rememberEventCursor(String runId, long afterSequence) {
        String normalized = normalizeRunId(runId);
        if (!normalized.isBlank()) {
            eventCursors.merge(normalized, Math.max(0, afterSequence), Math::max);
        }
    }

    public synchronized OptionalLong eventCursor(String runId) {
        Long cursor = eventCursors.get(normalizeRunId(runId));
        return cursor == null ? OptionalLong.empty() : OptionalLong.of(cursor);
    }

    public synchronized Map<String, Long> eventCursors() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(eventCursors));
    }

    public synchronized void clearEventCursor(String runId) {
        eventCursors.remove(normalizeRunId(runId));
    }

    private static String normalizeRunId(String runId) {
        return runId == null ? "" : runId.trim();
    }
}
