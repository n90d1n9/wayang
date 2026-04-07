package tech.kayys.wayang.embedding.provider;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.embedding.EmbeddingException;
import tech.kayys.wayang.embedding.EmbeddingProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class CharNgramEmbeddingProvider implements EmbeddingProvider {

    public static final String NAME = "chargram";
    private static final int DEFAULT_DIMENSION = 512;
    private static final int MAX_DIMENSION = 8192;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(String model) {
        return model != null && (model.equals(NAME) || model.matches("^chargram-\\d+$"));
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

        String normalized = " " + input.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ") + " ";
        int emitted = 0;

        for (int n = 3; n <= 5; n++) {
            if (normalized.length() < n) {
                continue;
            }
            for (int i = 0; i <= normalized.length() - n; i++) {
                String gram = normalized.substring(i, i + n);
                int h = fnv1a32(gram);
                int index = Math.floorMod(h, dim);
                float weight = n == 3 ? 0.8f : (n == 4 ? 1.0f : 1.2f);
                vector[index] += ((h & 1) == 0) ? weight : -weight;
                emitted++;
            }
        }

        if (emitted == 0) {
            int h = fnv1a32(normalized);
            vector[Math.floorMod(h, dim)] = 1.0f;
        }
        return vector;
    }

    private static int parseDimension(String model) {
        if (model == null || model.isBlank() || model.equals(NAME)) {
            return DEFAULT_DIMENSION;
        }
        if (!model.startsWith("chargram-")) {
            throw new EmbeddingException("Unsupported chargram model: " + model);
        }
        String suffix = model.substring("chargram-".length());
        int dim;
        try {
            dim = Integer.parseInt(suffix);
        } catch (NumberFormatException ex) {
            throw new EmbeddingException("Invalid chargram dimension: " + model, ex);
        }
        if (dim <= 0 || dim > MAX_DIMENSION) {
            throw new EmbeddingException("chargram model dimension must be between 1 and " + MAX_DIMENSION);
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
