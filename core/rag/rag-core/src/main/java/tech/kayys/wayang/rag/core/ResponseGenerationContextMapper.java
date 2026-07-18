package tech.kayys.wayang.rag.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ResponseGenerationContextMapper {

    private static final String DEFAULT_TEMPLATE_ID = "default";
    private static final String DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant.";

    private ResponseGenerationContextMapper() {
    }

    record Defaults(
            String provider,
            String model,
            double temperature,
            int maxTokens,
            boolean includeCitations,
            boolean useCache) {
    }

    static ResponseGenerationExecutor.GenerationContext from(Map<String, Object> context, Defaults defaults) {
        Map<String, Object> safeContext = context == null ? Map.of() : context;
        ContextInputs contextInputs = contextInputs(safeContext.get("contexts"), safeContext.get("metadata"));
        boolean includeCitations = booleanOrDefault(safeContext.get("includeCitations"),
                defaults.includeCitations());

        GenerationConfig config = new GenerationConfig(
                stringOrDefault(safeContext.get("provider"), defaults.provider()),
                stringOrDefault(safeContext.get("model"), defaults.model()),
                (float) doubleOrDefault(safeContext.get("temperature"), defaults.temperature()),
                intOrDefault(safeContext.get("maxTokens"), defaults.maxTokens()),
                1.0f,
                0.0f,
                0.0f,
                List.of(),
                DEFAULT_SYSTEM_PROMPT,
                Map.of(),
                includeCitations,
                false,
                CitationStyle.INLINE_NUMBERED,
                false,
                false,
                Map.of());

        return new ResponseGenerationExecutor.GenerationContext(
                stringOrNull(safeContext.get("query")),
                contextInputs.contexts(),
                contextInputs.metadata(),
                conversationHistory(safeContext.get("conversationHistory")),
                config,
                includeCitations,
                booleanOrDefault(safeContext.get("useCache"), defaults.useCache()),
                stringOrDefault(safeContext.get("templateId"), DEFAULT_TEMPLATE_ID),
                apiKeyFromContext(safeContext));
    }

    private static ContextInputs contextInputs(Object rawContexts, Object rawMetadata) {
        List<?> contextValues = listOrSingleString(rawContexts);
        if (contextValues.isEmpty()) {
            return new ContextInputs(List.of(), List.of());
        }

        List<?> metadataValues = metadataRows(rawMetadata);
        List<String> contexts = new ArrayList<>();
        List<Map<String, Object>> metadata = new ArrayList<>();
        for (int i = 0; i < contextValues.size(); i++) {
            String context = stringOrNull(contextValues.get(i));
            if (context == null || context.isBlank()) {
                continue;
            }
            contexts.add(context);
            metadata.add(metadataAt(metadataValues, i));
        }
        return new ContextInputs(List.copyOf(contexts), List.copyOf(metadata));
    }

    private static List<ConversationTurn> conversationHistory(Object rawHistory) {
        if (!(rawHistory instanceof List<?> historyValues) || historyValues.isEmpty()) {
            return List.of();
        }

        List<ConversationTurn> history = new ArrayList<>();
        for (Object historyValue : historyValues) {
            ConversationTurn turn = conversationTurn(historyValue);
            if (turn != null) {
                history.add(turn);
            }
        }
        return List.copyOf(history);
    }

    private static ConversationTurn conversationTurn(Object historyValue) {
        if (historyValue instanceof ConversationTurn turn) {
            return turn.hasContent() ? turn : null;
        }

        Map<String, Object> turn = metadataMap(historyValue);
        String role = stringOrNull(turn.get("role"));
        String content = stringOrNull(turn.get("content"));
        ConversationTurn conversationTurn = new ConversationTurn(role, content, Instant.now());
        return conversationTurn.hasContent() ? conversationTurn : null;
    }

    private static List<?> listOrSingleString(Object value) {
        if (value instanceof List<?> values) {
            return values;
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return List.of(stringValue);
        }
        return List.of();
    }

    private static List<?> metadataRows(Object value) {
        if (value instanceof List<?> values) {
            return values;
        }
        if (value instanceof Map<?, ?>) {
            return List.of(value);
        }
        return List.of();
    }

    private static Map<String, Object> metadataAt(List<?> metadataValues, int index) {
        if (index >= metadataValues.size()) {
            return Map.of();
        }
        return metadataMap(metadataValues.get(index));
    }

    private static Map<String, Object> metadataMap(Object value) {
        if (!(value instanceof Map<?, ?> raw) || raw.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        raw.forEach((key, mapValue) -> {
            if (key instanceof String stringKey && !stringKey.isBlank()) {
                metadata.put(stringKey, mapValue);
            }
        });
        return RagMetadata.copy(metadata);
    }

    private static String apiKeyFromContext(Map<String, Object> context) {
        Object apiKey = context.containsKey("apiKey") ? context.get("apiKey") : context.get("api_key");
        if (apiKey instanceof String value && !value.isBlank()) {
            return value;
        }
        return null;
    }

    private static String stringOrNull(Object value) {
        return value instanceof String stringValue ? stringValue : null;
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        return defaultValue;
    }

    private static double doubleOrDefault(Object value, double defaultValue) {
        if (value instanceof Number numberValue) {
            return numberValue.doubleValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Double.parseDouble(stringValue);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static int intOrDefault(Object value, int defaultValue) {
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static boolean booleanOrDefault(Object value, boolean defaultValue) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            if ("true".equalsIgnoreCase(stringValue.trim())) {
                return true;
            }
            if ("false".equalsIgnoreCase(stringValue.trim())) {
                return false;
            }
        }
        return defaultValue;
    }

    private record ContextInputs(List<String> contexts, List<Map<String, Object>> metadata) {
    }
}
