package tech.kayys.wayang.rag.embedding;

import java.util.Objects;

public record EmbeddingSchemaContract(
        String model,
        int dimension,
        String version) {

    public EmbeddingSchemaContract {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (dimension <= 0) {
            throw new IllegalArgumentException("dimension must be > 0");
        }
        if (version == null || version.isBlank()) {
            version = "v1";
        }
        model = model.trim();
        version = version.trim();
    }

    public static EmbeddingSchemaContract fromDefaults(String model, int dimension, String version) {
        return new EmbeddingSchemaContract(
                Objects.requireNonNull(model, "model must not be null"),
                dimension,
                version);
    }
}
