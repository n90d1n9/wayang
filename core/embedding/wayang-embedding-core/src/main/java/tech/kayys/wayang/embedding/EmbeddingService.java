package tech.kayys.wayang.embedding;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

@ApplicationScoped
public class EmbeddingService {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingProviderRegistry registry;
    private final EmbeddingConfigRuntime runtimeConfig;
    private final EmbeddingModuleConfig config;
    private final EmbeddingVectorCache vectorCache;

    @Inject
    public EmbeddingService(Instance<EmbeddingProvider> providers, EmbeddingConfigRuntime runtimeConfig) {

        List<EmbeddingProvider> providerList = new ArrayList<>();
        for (EmbeddingProvider provider : providers) {
            providerList.add(provider);
        }
        this.registry = new EmbeddingProviderRegistry(providerList);
        this.runtimeConfig = runtimeConfig;
        this.config = null;
        this.vectorCache = new EmbeddingVectorCache(runtimeConfig.current().getCacheMaxEntries());
    }

    public EmbeddingService(EmbeddingProviderRegistry registry, EmbeddingModuleConfig config) {
        this.registry = registry;
        this.runtimeConfig = null;
        this.config = config == null ? new EmbeddingModuleConfig() : config;
        this.vectorCache = new EmbeddingVectorCache(this.config.getCacheMaxEntries());
    }

    public Uni<EmbeddingResponse> embed(EmbeddingRequest request) {
        LOG.debug("Request: ", request.toString());
        return embedForTenant(null, request);
    }

    public Uni<EmbeddingResponse> embedForTenant(String tenantId, EmbeddingRequest request) {
        return Uni.createFrom().deferred(() -> {
            EmbeddingModuleConfig activeConfig = activeConfig();
            String modelName = resolveModel(tenantId, request);
            String providerName = resolveProviderName(tenantId, request);
            boolean normalize = request.normalize() == null ? activeConfig.isNormalize() : request.normalize();

            EmbeddingProvider provider = resolveProvider(providerName, modelName);
            if (!provider.supports(modelName)) {
                return Uni.createFrom().failure(new EmbeddingException(
                        "Provider '" + provider.name() + "' does not support model '" + modelName + "'"));
            }

            return embedWithCacheAndDedup(
                    tenantId,
                    request.inputs(),
                    provider,
                    modelName,
                    normalize,
                    activeConfig)
                    .map(vectors -> {
                        int dimension = vectors.isEmpty() ? 0 : vectors.get(0).length;
                        return new EmbeddingResponse(vectors, dimension, provider.name(), modelName,
                                activeConfig.getEmbeddingVersion());
                    });
        });
    }

    public Uni<float[]> embedOne(String input) {
        return embed(EmbeddingRequest.single(input)).map(EmbeddingResponse::first);
    }

    public void reloadConfiguration() {
        if (runtimeConfig != null) {
            runtimeConfig.reload();
            vectorCache.clear();
        }
    }

    private Uni<List<float[]>> embedWithCacheAndDedup(
            String tenantId,
            List<String> inputs,
            EmbeddingProvider provider,
            String modelName,
            boolean normalize,
            EmbeddingModuleConfig activeConfig) {

        boolean cacheEnabled = activeConfig.isCacheEnabled();
        Map<String, List<Integer>> missingByKey = new LinkedHashMap<>();
        Map<Integer, float[]> resolved = new LinkedHashMap<>();
        for (int i = 0; i < inputs.size(); i++) {
            String input = inputs.get(i);
            String key = buildCacheKey(tenantId, provider.name(), modelName, normalize, input);
            if (cacheEnabled) {
                float[] cached = vectorCache.get(key);
                if (cached != null) {
                    resolved.put(i, copyVector(cached));
                    continue;
                }
            }
            missingByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i);
        }

        if (missingByKey.isEmpty()) {
            return Uni.createFrom().item(assembleResults(inputs.size(), resolved));
        }

        List<String> missingInputs = missingByKey.values().stream()
                .map(indexes -> inputs.get(indexes.get(0)))
                .toList();

        // Assuming provider.embedAll is blocking, wrapping in Uni for now.
        // If provider can be reactive, even better.
        return Uni.createFrom().item(() -> provider.embedAll(missingInputs, modelName))
                .map(generated -> {
                    validateDimensions(modelName, generated);
                    if (normalize) {
                        generated = generated.stream().map(EmbeddingService::l2Normalize).toList();
                    }

                    int generatedIndex = 0;
                    for (Map.Entry<String, List<Integer>> entry : missingByKey.entrySet()) {
                        float[] vector = generated.get(generatedIndex++);
                        if (cacheEnabled) {
                            vectorCache.put(entry.getKey(), copyVector(vector));
                        }
                        for (Integer outputIndex : entry.getValue()) {
                            resolved.put(outputIndex, copyVector(vector));
                        }
                    }
                    return assembleResults(inputs.size(), resolved);
                });
    }

    private List<float[]> assembleResults(int size, Map<Integer, float[]> resolved) {
        List<float[]> ordered = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            float[] vector = resolved.get(i);
            if (vector == null) {
                throw new EmbeddingException("Failed to resolve embedding at index " + i);
            }
            ordered.add(vector);
        }
        return ordered;
    }

    private EmbeddingProvider resolveProvider(String requestedProvider, String modelName) {
        if (!isBlank(requestedProvider)) {
            return registry.required(requestedProvider);
        }
        EmbeddingModuleConfig activeConfig = activeConfig();
        return registry.findByModel(modelName)
                .orElseGet(() -> registry.required(activeConfig.getDefaultProvider()));
    }

    private String resolveModel(String tenantId, EmbeddingRequest request) {
        if (!isBlank(request.model())) {
            return request.model();
        }
        return activeConfig().tenantStrategies()
                .find(tenantId)
                .map(TenantEmbeddingStrategyRegistry.TenantEmbeddingStrategy::model)
                .filter(model -> !isBlank(model))
                .orElse(activeConfig().getDefaultModel());
    }

    private String resolveProviderName(String tenantId, EmbeddingRequest request) {
        if (!isBlank(request.provider())) {
            return request.provider();
        }
        return activeConfig().tenantStrategies()
                .find(tenantId)
                .map(TenantEmbeddingStrategyRegistry.TenantEmbeddingStrategy::provider)
                .filter(provider -> !isBlank(provider))
                .orElse(null);
    }

    private EmbeddingModuleConfig activeConfig() {
        if (runtimeConfig != null) {
            return runtimeConfig.current();
        }
        return config;
    }

    private static void validateDimensions(String modelName, List<float[]> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return;
        }
        int observed = vectors.get(0).length;
        for (int i = 1; i < vectors.size(); i++) {
            if (vectors.get(i).length != observed) {
                throw new EmbeddingException(
                        "Embedding provider returned inconsistent vector dimensions: first="
                                + observed + ", index=" + i + ", value=" + vectors.get(i).length);
            }
        }
        OptionalInt expected = EmbeddingModelSpec.parseDimension(modelName);
        if (expected.isPresent() && expected.getAsInt() != observed) {
            throw new EmbeddingException(
                    "Embedding model dimension mismatch: model=" + modelName
                            + ", expected=" + expected.getAsInt()
                            + ", observed=" + observed);
        }
    }

    private static float[] l2Normalize(float[] vector) {
        double sumSquares = 0.0;
        for (float v : vector) {
            sumSquares += (double) v * v;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm == 0.0) {
            return vector;
        }

        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String buildCacheKey(
            String tenantId,
            String provider,
            String model,
            boolean normalize,
            String input) {
        String normalizedTenant = isBlank(tenantId) ? "default" : tenantId.trim().toLowerCase(Locale.ROOT);
        String fingerprint = normalizedTenant + "|" + provider + "|" + model + "|" + normalize + "|" + sha256(input);
        return fingerprint;
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(input).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(Character.forDigit((b >>> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new EmbeddingException("Unable to initialize SHA-256 hasher", e);
        }
    }

    private static float[] copyVector(float[] vector) {
        return java.util.Arrays.copyOf(vector, vector.length);
    }
}
