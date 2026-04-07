package tech.kayys.wayang.embedding;

import java.util.List;

public record EmbeddingRequest(
        List<String> inputs,
        String model,
        String provider,
        Boolean normalize) {

    public EmbeddingRequest {
        if (inputs == null || inputs.isEmpty()) {
            throw new IllegalArgumentException("inputs must not be null or empty");
        }
        if (inputs.stream().anyMatch(s -> s == null)) {
            throw new IllegalArgumentException("inputs must not contain null values");
        }
    }

    public static EmbeddingRequest single(String input) {
        return new EmbeddingRequest(List.of(input), null, null, null);
    }
}
