package tech.kayys.wayang.gollek.sdk.remote;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class RemoteJson {

    private RemoteJson() {
    }

    static String runRequest(String prompt, String tenantId, String modelId, String workflowId, String surfaceId,
            List<String> skills, boolean memoryEnabled, int maxSteps, String workspacePath,
            boolean workspaceEnabled, int workspaceMaxEntries, boolean harnessEnabled,
            int harnessMaxChecks, boolean harnessIncludeOptional, String sessionId, String userId,
            Map<String, Object> context, String systemPrompt) {
        return "{"
                + "\"prompt\":\"" + escape(prompt) + "\","
                + "\"systemPrompt\":\"" + escape(systemPrompt) + "\","
                + "\"tenantId\":\"" + escape(tenantId) + "\","
                + "\"modelId\":\"" + escape(modelId) + "\","
                + "\"workflowId\":\"" + escape(workflowId) + "\","
                + "\"surfaceId\":\"" + escape(surfaceId) + "\","
                + "\"sessionId\":\"" + escape(sessionId) + "\","
                + "\"userId\":\"" + escape(userId) + "\","
                + "\"context\":" + object(context) + ","
                + "\"skills\":" + stringArray(skills) + ","
                + "\"memoryEnabled\":" + memoryEnabled + ","
                + "\"maxSteps\":" + maxSteps + ","
                + "\"workspacePath\":\"" + escape(workspacePath) + "\","
                + "\"workspaceEnabled\":" + workspaceEnabled + ","
                + "\"workspaceMaxEntries\":" + workspaceMaxEntries + ","
                + "\"harnessEnabled\":" + harnessEnabled + ","
                + "\"harnessMaxChecks\":" + harnessMaxChecks + ","
                + "\"harnessIncludeOptional\":" + harnessIncludeOptional
                + "}";
    }

    static String workspaceRequest(String rootPath, int maxEntries, boolean includeHidden) {
        return "{"
                + "\"rootPath\":\"" + escape(rootPath) + "\","
                + "\"maxEntries\":" + maxEntries + ","
                + "\"includeHidden\":" + includeHidden
                + "}";
    }

    static String harnessPlanRequest(String rootPath, int maxChecks, boolean includeOptional) {
        return "{"
                + "\"rootPath\":\"" + escape(rootPath) + "\","
                + "\"maxChecks\":" + maxChecks + ","
                + "\"includeOptional\":" + includeOptional
                + "}";
    }

    static String cancelRequest(String reason) {
        return "{"
                + "\"reason\":\"" + escape(reason) + "\""
                + "}";
    }

    private static String stringArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder output = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                output.append(',');
            }
            output.append('"').append(escape(values.get(i))).append('"');
        }
        output.append(']');
        return output.toString();
    }

    private static String object(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        StringBuilder output = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            if (!first) {
                output.append(',');
            }
            output.append('"').append(escape(entry.getKey())).append('"')
                    .append(':')
                    .append(value(entry.getValue()));
            first = false;
        }
        output.append('}');
        return output.toString();
    }

    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder output = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                if (!first) {
                    output.append(',');
                }
                output.append('"').append(escape(String.valueOf(entry.getKey()))).append('"')
                        .append(':')
                        .append(value(entry.getValue()));
                first = false;
            }
            output.append('}');
            return output.toString();
        }
        if (value instanceof Iterable<?> iterable) {
            StringBuilder output = new StringBuilder("[");
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    output.append(',');
                }
                output.append(value(item));
                first = false;
            }
            output.append(']');
            return output.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    static String escape(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder output = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\' -> output.append("\\\\");
                case '"' -> output.append("\\\"");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> output.append(ch);
            }
        }
        return output.toString();
    }

    static String stringField(String json, String field) {
        int colon = fieldColonIndex(json, field);
        if (colon < 0) {
            return "";
        }
        int start = skipWhitespace(json, colon + 1);
        if (start >= json.length() || json.charAt(start) != '"') {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                value.append(unescape(ch));
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                return value.toString();
            } else {
                value.append(ch);
            }
        }
        return "";
    }

    static boolean booleanField(String json, String field, boolean fallback) {
        String raw = rawField(json, field);
        return raw.isBlank() ? fallback : Boolean.parseBoolean(raw);
    }

    static int intField(String json, String field, int fallback) {
        String raw = rawField(json, field);
        try {
            return raw.isBlank() ? fallback : Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static List<String> objectArrayField(String json, String field) {
        int colon = fieldColonIndex(json, field);
        if (colon < 0) {
            return List.of();
        }
        int start = skipWhitespace(json, colon + 1);
        if (start >= json.length() || json.charAt(start) != '[') {
            return List.of();
        }
        List<String> objects = new java.util.ArrayList<>();
        int depth = 0;
        int objectStart = -1;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(json.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            } else if (ch == ']' && depth == 0) {
                return List.copyOf(objects);
            }
        }
        return List.copyOf(objects);
    }

    static Map<String, Object> objectField(String json, String field) {
        int colon = fieldColonIndex(json, field);
        if (colon < 0) {
            return Map.of();
        }
        int start = skipWhitespace(json, colon + 1);
        if (start >= json.length() || json.charAt(start) != '{') {
            return Map.of();
        }
        int end = matchingObjectEnd(json, start);
        if (end <= start) {
            return Map.of();
        }
        return parseObject(json, start + 1, end);
    }

    static Map<String, Object> object(String json) {
        String normalized = json == null ? "" : json.trim();
        if (normalized.isEmpty() || normalized.charAt(0) != '{') {
            return Map.of();
        }
        int end = matchingObjectEnd(normalized, 0);
        if (end <= 0) {
            return Map.of();
        }
        return parseObject(normalized, 1, end);
    }

    private static String rawField(String json, String field) {
        int colon = fieldColonIndex(json, field);
        if (colon < 0) {
            return "";
        }
        int start = skipWhitespace(json, colon + 1);
        int end = start;
        while (end < json.length() && ",}\n\r\t ".indexOf(json.charAt(end)) < 0) {
            end++;
        }
        return json.substring(start, end).trim();
    }

    private static int fieldColonIndex(String json, String field) {
        String needle = "\"" + escape(field) + "\"";
        int keyIndex = json == null ? -1 : json.indexOf(needle);
        while (keyIndex >= 0) {
            int afterKey = skipWhitespace(json, keyIndex + needle.length());
            if (afterKey < json.length() && json.charAt(afterKey) == ':') {
                return afterKey;
            }
            keyIndex = json.indexOf(needle, keyIndex + needle.length());
        }
        return -1;
    }

    private static Map<String, Object> parseObject(String json, int start, int end) {
        Map<String, Object> values = new LinkedHashMap<>();
        int index = start;
        while (index < end) {
            index = skipObjectSeparators(json, index, end);
            if (index >= end || json.charAt(index) != '"') {
                break;
            }
            ParsedValue key = quotedValue(json, index);
            index = skipWhitespace(json, key.nextIndex());
            if (index >= end || json.charAt(index) != ':') {
                break;
            }
            ParsedValue value = parseValue(json, skipWhitespace(json, index + 1), end);
            String normalizedKey = key.value() instanceof String string ? string.trim() : "";
            if (!normalizedKey.isEmpty() && value.value() != null) {
                values.put(normalizedKey, value.value());
            }
            index = value.nextIndex();
        }
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }

    private static ParsedValue parseValue(String json, int start, int end) {
        if (start >= end) {
            return new ParsedValue(null, start);
        }
        char ch = json.charAt(start);
        if (ch == '"') {
            return quotedValue(json, start);
        }
        if (ch == '{') {
            int objectEnd = matchingObjectEnd(json, start);
            if (objectEnd > start) {
                return new ParsedValue(parseObject(json, start + 1, objectEnd), objectEnd + 1);
            }
        }
        if (ch == '[') {
            int arrayEnd = matchingArrayEnd(json, start);
            if (arrayEnd > start) {
                return new ParsedValue(parseArray(json, start + 1, arrayEnd), arrayEnd + 1);
            }
        }
        int valueEnd = scalarEnd(json, start, end);
        String raw = json.substring(start, valueEnd).trim();
        return new ParsedValue(scalarValue(raw), valueEnd);
    }

    private static List<Object> parseArray(String json, int start, int end) {
        List<Object> values = new java.util.ArrayList<>();
        int index = start;
        while (index < end) {
            index = skipObjectSeparators(json, index, end);
            if (index >= end) {
                break;
            }
            ParsedValue value = parseValue(json, index, end);
            if (value.value() != null) {
                values.add(value.value());
            }
            index = value.nextIndex();
        }
        return values.isEmpty() ? List.of() : List.copyOf(values);
    }

    private static ParsedValue quotedValue(String json, int start) {
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                value.append(unescape(ch));
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                return new ParsedValue(value.toString(), i + 1);
            } else {
                value.append(ch);
            }
        }
        return new ParsedValue("", json.length());
    }

    private static Object scalarValue(String raw) {
        if (raw.isBlank() || "null".equals(raw)) {
            return null;
        }
        if ("true".equals(raw) || "false".equals(raw)) {
            return Boolean.parseBoolean(raw);
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignoredLong) {
                try {
                    return Double.parseDouble(raw);
                } catch (NumberFormatException ignoredDouble) {
                    return raw;
                }
            }
        }
    }

    private static int matchingObjectEnd(String json, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int matchingArrayEnd(String json, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int scalarEnd(String json, int start, int end) {
        int current = start;
        while (current < end && json.charAt(current) != ',') {
            current++;
        }
        return current;
    }

    private static int skipObjectSeparators(String json, int start, int end) {
        int current = start;
        while (current < end) {
            char ch = json.charAt(current);
            if (ch != ',' && !Character.isWhitespace(ch)) {
                break;
            }
            current++;
        }
        return current;
    }

    private static int skipWhitespace(String value, int index) {
        int current = index;
        while (current < value.length() && Character.isWhitespace(value.charAt(current))) {
            current++;
        }
        return current;
    }

    private static char unescape(char ch) {
        return switch (ch) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> ch;
        };
    }

    private record ParsedValue(Object value, int nextIndex) {
    }
}
