package tech.kayys.wayang.agent.skills.builtin;

import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.InferenceResponse;
import tech.kayys.wayang.agent.spi.InferenceTypes;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BuiltinSkillSupport {

    private BuiltinSkillSupport() {
    }

    static String stringInput(Map<String, Object> inputs, String name) {
        return stringInput(inputs, name, null);
    }

    static String stringInput(Map<String, Object> inputs, String name, String defaultValue) {
        Object value = inputs.get(name);
        return value == null ? defaultValue : value.toString();
    }

    static int intInput(Map<String, Object> inputs, String name, int defaultValue) {
        Object value = inputs.get(name);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return defaultValue;
    }

    static double doubleInput(Map<String, Object> inputs, String name, double defaultValue) {
        Object value = inputs.get(name);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Double.parseDouble(text);
        }
        return defaultValue;
    }

    static Map<String, String> stringMapInput(Map<String, Object> inputs, String name) {
        Object value = inputs.get(name);
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        rawMap.forEach((key, mapValue) -> {
            if (key != null && mapValue != null) {
                result.put(key.toString(), mapValue.toString());
            }
        });
        return result;
    }

    static Map<String, Object> objectMapInput(Map<String, Object> inputs, String name) {
        Object value = inputs.get(name);
        if (!(value instanceof Map<?, ?> rawMap)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        rawMap.forEach((key, mapValue) -> {
            if (key != null && mapValue != null) {
                result.put(key.toString(), mapValue);
            }
        });
        return result;
    }

    static Map<String, Object> success(String observation, Map<String, Object> outputs) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("status", "SUCCESS");
        result.put("observation", observation == null ? "" : observation);
        if (outputs != null) {
            result.putAll(outputs);
        }
        return result;
    }

    static Map<String, Object> failure(String observation) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("status", "FAILURE");
        result.put("observation", observation == null ? "" : observation);
        return result;
    }

    static Map<String, Object> error(Throwable error) {
        Map<String, Object> result = failure(error == null ? "Unknown error" : error.getMessage());
        result.put("status", "ERROR");
        result.put("error", error == null ? null : error.getClass().getName());
        return result;
    }

    static InferenceRequest textRequest(
            String requestId,
            String model,
            String systemPrompt,
            String prompt,
            int maxTokens,
            double temperature,
            String tenantId) {
        return InferenceRequest.builder()
                .requestId(requestId)
                .model(model)
                .message(new InferenceTypes.SystemMessage(systemPrompt))
                .message(new InferenceTypes.UserMessage(prompt))
                .maxTokens(maxTokens)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(60))
                .metadata(tenantId == null ? Map.of() : Map.of("tenantId", tenantId))
                .build();
    }

    static String responseContent(InferenceResponse response) {
        if (response == null || response.message() == null || response.message().content() == null) {
            return "";
        }
        return response.message().content();
    }

    static int totalTokens(InferenceResponse response) {
        return response != null && response.usage() != null ? response.usage().totalTokens() : 0;
    }

    static String responseModel(InferenceResponse response, String fallback) {
        return response != null && response.model() != null ? response.model() : fallback;
    }

    static List<Float> boxedVector(float[] vector) {
        if (vector == null) {
            return List.of();
        }
        java.util.ArrayList<Float> boxed = new java.util.ArrayList<>(vector.length);
        for (float value : vector) {
            boxed.add(value);
        }
        return boxed;
    }
}
