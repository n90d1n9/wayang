package tech.kayys.wayang.storage.spi;

import io.smallrye.mutiny.Uni;

/**
 * Provider-neutral storage contract for model and artifact payloads.
 */
public interface ModelStorageService {

    /**
     * Store a versioned model payload and return a provider-specific storage URI.
     */
    Uni<String> uploadModel(String namespace, String modelId, String version, byte[] data);

    /**
     * Load a model payload from a storage URI returned by a provider.
     */
    Uni<byte[]> downloadModel(String storageUri);

    /**
     * Delete a model payload from a storage URI returned by a provider.
     */
    Uni<Void> deleteModel(String storageUri);
}
