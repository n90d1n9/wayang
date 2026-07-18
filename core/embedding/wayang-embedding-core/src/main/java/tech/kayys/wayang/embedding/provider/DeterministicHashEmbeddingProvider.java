package tech.kayys.wayang.embedding.provider;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.embedding.EmbeddingException;
import tech.kayys.wayang.embedding.EmbeddingProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class DeterministicHashEmbeddingProvider implements EmbeddingProvider {

    public static final String NAME = "hash";
    private static final int DEFAULT_DIMENSION = 384;
    private static final int MAX_DIMENSION = 8192;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(String model) {
        return model != null && (model.equals(NAME) || model.matches("^hash-\\d+$"));
    }

    @Override
    public List<float[]> embedAll(List<String> inputs, String model) {
        int dim = parseDimension(model);
        List<float[]> out = new ArrayList<>(inputs.size());
        for (String input : inputs) {
            out.add(embedSingle(input, dim));
        }
        return out;
    }

    private static float[] embedSingle(String input, int dim) {
        float[] vector = new float[dim];
        if (input == null || input.isBlank()) {
            return vector;
        }

        String normalized = input.toLowerCase(Locale.ROOT).trim();
        String[] tokens = normalized.split("\\s+");

        for (int tokenIndex = 0; tokenIndex < tokens.length; tokenIndex++) {
            String token = tokens[tokenIndex];
            int h1 = fnv1a32(token);
            int h2 = fnv1a32(token + "#" + tokenIndex);

            int i1 = Math.floorMod(h1, dim);
            int i2 = Math.floorMod(h2, dim);

            float weight = 1.0f + Math.min(token.length(), 24) / 24.0f;
            vector[i1] += ((h1 & 1) == 0) ? weight : -weight;
            vector[i2] += ((h2 & 1) == 0) ? weight * 0.5f : -weight * 0.5f;
        }

        if (normalized.length() >= 3) {
            for (int i = 0; i <= normalized.length() - 3; i++) {
                String gram = normalized.substring(i, i + 3);
                int h = fnv1a32(gram);
                int idx = Math.floorMod(h, dim);
                vector[idx] += ((h >>> 1) & 1) == 0 ? 0.15f : -0.15f;
            }
        }

        return vector;
    }

    private static int parseDimension(String model) {
        if (model == null || model.isBlank() || model.equals(NAME)) {
            return DEFAULT_DIMENSION;
        }
        if (!model.startsWith("hash-")) {
            throw new EmbeddingException("Unsupported hash model: " + model);
        }
        String suffix = model.substring("hash-".length());
        int dim;
        try {
            dim = Integer.parseInt(suffix);
        } catch (NumberFormatException ex) {
            throw new EmbeddingException("Invalid hash dimension: " + model, ex);
        }

        if (dim <= 0 || dim > MAX_DIMENSION) {
            throw new EmbeddingException("hash model dimension must be between 1 and " + MAX_DIMENSION);
        }
        return dim;
    }

    private static int fnv1a32(String value) {
        int hash = 0x811C9DC5;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }
}
