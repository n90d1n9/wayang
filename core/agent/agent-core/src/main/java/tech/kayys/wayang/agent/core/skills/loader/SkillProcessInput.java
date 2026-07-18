package tech.kayys.wayang.agent.core.skills.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Array;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Process command projection for filesystem skills.
 */
final class SkillProcessInput {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern PARAMETER_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,127}");

    private final List<String> command;

    private SkillProcessInput(List<String> command) {
        this.command = List.copyOf(command);
    }

    static SkillProcessInput of(Path executable, Map<String, Object> parameters) {
        Objects.requireNonNull(executable, "executable");
        List<String> command = new ArrayList<>(interpreterCommand(executable));
        command.addAll(argumentList(parameters));
        return new SkillProcessInput(command);
    }

    List<String> command() {
        return command;
    }

    private static List<String> interpreterCommand(Path executable) {
        String execName = executable.getFileName().toString();

        if (execName.endsWith(".sh")) {
            return List.of("bash", executable.toString());
        }
        if (execName.endsWith(".py")) {
            return List.of("python3", executable.toString());
        }
        if (execName.endsWith(".js")) {
            return List.of("node", executable.toString());
        }
        if (execName.endsWith(".go")) {
            return List.of("go", "run", executable.toString());
        }
        return List.of(executable.toString());
    }

    private static List<String> argumentList(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return List.of();
        }

        List<Map.Entry<String, Object>> entries = parameters.entrySet().stream()
                .sorted(Comparator.comparing(entry -> normalizeName(entry.getKey())))
                .toList();

        List<String> arguments = new ArrayList<>(entries.size() * 2);
        for (Map.Entry<String, Object> entry : entries) {
            String name = normalizeName(entry.getKey());
            arguments.add("--" + name);
            arguments.add(formatValue(name, entry.getValue()));
        }
        return arguments;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Skill parameter name must not be blank");
        }
        String trimmed = name.trim();
        if (!PARAMETER_NAME.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    "Invalid skill parameter name '" + name + "'. Use letters, numbers, dot, underscore, or hyphen.");
        }
        return trimmed;
    }

    private static String formatValue(String name, Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> || value instanceof Collection<?> || value.getClass().isArray()) {
            try {
                return JSON.writeValueAsString(jsonValue(value));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException(
                        "Failed to serialize skill parameter '" + name + "': " + e.getOriginalMessage(), e);
            }
        }
        return String.valueOf(value);
    }

    private static Object jsonValue(Object value) {
        if (value != null && value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> values = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                values.add(Array.get(value, i));
            }
            return values;
        }
        return value;
    }
}
