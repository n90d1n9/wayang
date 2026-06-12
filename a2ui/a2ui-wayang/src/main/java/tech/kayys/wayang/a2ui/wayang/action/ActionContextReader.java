package tech.kayys.wayang.a2ui.wayang.action;

import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Typed accessors for inbound A2UI user-action context values.
 */
public final class ActionContextReader {

    private ActionContextReader() {
    }

    public static String text(A2uiUserAction action, String key) {
        Object value = value(action, key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public static String firstText(A2uiUserAction action, String... keys) {
        if (keys == null || keys.length == 0) {
            return "";
        }
        for (String key : keys) {
            String value = text(action, key);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    public static Integer integer(A2uiUserAction action, String key) {
        Object value = value(action, key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text.trim());
        }
        return null;
    }

    public static Long longValue(A2uiUserAction action, String key) {
        Object value = value(action, key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text.trim());
        }
        return null;
    }

    public static boolean hasValue(A2uiUserAction action, String key) {
        Object value = value(action, key);
        return value != null && !String.valueOf(value).isBlank();
    }

    public static A2uiUserAction withValue(A2uiUserAction action, String key, Object value) {
        Objects.requireNonNull(action, "action");
        Map<String, Object> context = new LinkedHashMap<>(action.context());
        context.put(key, value);
        return new A2uiUserAction(
                action.name(),
                action.surfaceId(),
                action.sourceComponentId(),
                action.timestamp(),
                context);
    }

    private static Object value(A2uiUserAction action, String key) {
        Objects.requireNonNull(action, "action");
        return action.context().get(key);
    }
}
