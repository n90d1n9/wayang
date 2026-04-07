package tech.kayys.wayang.embedding.provider;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.embedding.EmbeddingException;
import tech.kayys.wayang.embedding.EmbeddingProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class TfIdfHashEmbeddingProvider implements EmbeddingProvider {

    public static final String NAME = "tfidf";
    private static final int DEFAULT_DIMENSION = 512;
    private static final int MAX_DIMENSION = 8192;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(String model) {
        return model != null && (model.equals(NAME) || model.matches("^tfidf-\\d+$"));
    }

    @Override
    public List<float[]> embedAll(List<String> inputs, String model) {
        int dim = parseDimension(model);
        List<Map<String, Integer>> tokenFrequencies = new ArrayList<>(inputs.size());
        Map<String, Integer> documentFrequencies = new HashMap<>();

        for (String input : inputs) {
            Map<String, Integer> tf = termFrequency(input);
            tokenFrequencies.add(tf);
            for (String token : tf.keySet()) {
                documentFrequencies.merge(token, 1, Integer::sum);
            }
        }

        int docs = Math.max(1, inputs.size());
        List<float[]> vectors = new ArrayList<>(inputs.size());
        for (Map<String, Integer> tf : tokenFrequencies) {
            float[] vector = new float[dim];
            int totalTokens = Math.max(1, tf.values().stream().mapToInt(i -> i).sum());

            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                String token = entry.getKey();
                int frequency = entry.getValue();
                int df = documentFrequencies.getOrDefault(token, 1);

                double termFrequencyWeight = (double) frequency / totalTokens;
                double inverseDocumentFrequency = Math.log((docs + 1.0) / (df + 1.0)) + 1.0;
                float weight = (float) (termFrequencyWeight * inverseDocumentFrequency);

                int h = fnv1a32(token);
                int index = Math.floorMod(h, dim);
                vector[index] += ((h & 1) == 0) ? weight : -weight;
            }

            vectors.add(vector);
        }
        return vectors;
    }

    private static Map<String, Integer> termFrequency(String input) {
        Map<String, Integer> tf = new LinkedHashMap<>();
        if (input == null || input.isBlank()) {
            return tf;
        }
        String normalized = input.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{Alnum}\\s]", " ")
                .trim();
        if (normalized.isEmpty()) {
            return tf;
        }
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 2) {
                continue;
            }
            tf.merge(token, 1, Integer::sum);
        }
        return tf;
    }

    private static int parseDimension(String model) {
        if (model == null || model.isBlank() || model.equals(NAME)) {
            return DEFAULT_DIMENSION;
        }
        if (!model.startsWith("tfidf-")) {
            throw new EmbeddingException("Unsupported tfidf model: " + model);
        }
        String suffix = model.substring("tfidf-".length());
        int dim;
        try {
            dim = Integer.parseInt(suffix);
        } catch (NumberFormatException ex) {
            throw new EmbeddingException("Invalid tfidf dimension: " + model, ex);
        }
        if (dim <= 0 || dim > MAX_DIMENSION) {
            throw new EmbeddingException("tfidf model dimension must be between 1 and " + MAX_DIMENSION);
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
