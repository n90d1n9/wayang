package tech.kayys.wayang.agent.skills.cli;

import java.util.List;

/**
 * Small JSON writer helpers shared by CLI renderers.
 */
final class SkillsJsonWriter {

    private SkillsJsonWriter() {
    }

    static void field(StringBuilder builder, String name, String value) {
        name(builder, name);
        string(builder, value);
        builder.append(',');
    }

    static void field(StringBuilder builder, String name, boolean value) {
        name(builder, name);
        builder.append(value).append(',');
    }

    static void field(StringBuilder builder, String name, int value) {
        name(builder, name);
        builder.append(value).append(',');
    }

    static void arrayField(StringBuilder builder, String name, List<String> values) {
        name(builder, name);
        builder.append('[');
        for (String value : values) {
            string(builder, value);
            builder.append(',');
        }
        trimComma(builder);
        builder.append("],");
    }

    static void name(StringBuilder builder, String name) {
        string(builder, name);
        builder.append(':');
    }

    static void string(StringBuilder builder, String value) {
        builder.append('"').append(escape(value)).append('"');
    }

    static void trimComma(StringBuilder builder) {
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) == ',') {
            builder.setLength(builder.length() - 1);
        }
    }

    private static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
