package tech.kayys.wayang.embedding;

import java.util.Locale;
import java.util.OptionalInt;

public final class EmbeddingModelSpec {

    private EmbeddingModelSpec() {
    }

    public static OptionalInt parseDimension(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return OptionalInt.empty();
        }

        String normalized = modelName.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.lastIndexOf('-');
        if (separator < 0 || separator == normalized.length() - 1) {
            return OptionalInt.empty();
        }

        String suffix = normalized.substring(separator + 1);
        try {
            int dimension = Integer.parseInt(suffix);
            return dimension > 0 ? OptionalInt.of(dimension) : OptionalInt.empty();
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }
}
