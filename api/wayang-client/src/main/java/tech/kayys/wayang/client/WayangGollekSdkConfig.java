package tech.kayys.wayang.client;

import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileRegistryConfig;
import tech.kayys.wayang.storage.WayangStorageBackend;
import tech.kayys.wayang.storage.WayangStorageConfig;

public record WayangGollekSdkConfig(
        WayangGollekSdkProvider.Mode mode,
        String endpoint,
        String apiKey,
        String defaultTenantId,
        String defaultModelId,
        String runStorePath,
        WayangStorageConfig storage,
        WayangPlatformReadinessProfileRegistryConfig readinessProfileRegistry) {

    public WayangGollekSdkConfig(
            WayangGollekSdkProvider.Mode mode,
            String endpoint,
            String apiKey,
            String defaultTenantId,
            String defaultModelId) {
        this(mode, endpoint, apiKey, defaultTenantId, defaultModelId, "");
    }

    public WayangGollekSdkConfig(
            WayangGollekSdkProvider.Mode mode,
            String endpoint,
            String apiKey,
            String defaultTenantId,
            String defaultModelId,
            String runStorePath) {
        this(
                mode,
                endpoint,
                apiKey,
                defaultTenantId,
                defaultModelId,
                runStorePath,
                WayangStorageConfig.fromRunStorePath(runStorePath));
    }

    public WayangGollekSdkConfig(
            WayangGollekSdkProvider.Mode mode,
            String endpoint,
            String apiKey,
            String defaultTenantId,
            String defaultModelId,
            String runStorePath,
            WayangStorageConfig storage) {
        this(
                mode,
                endpoint,
                apiKey,
                defaultTenantId,
                defaultModelId,
                runStorePath,
                storage,
                WayangPlatformReadinessProfileRegistryConfig.builtin());
    }

    public WayangGollekSdkConfig {
        mode = mode == null ? WayangGollekSdkProvider.Mode.LOCAL : mode;
        endpoint = SdkText.trimToEmpty(endpoint);
        apiKey = SdkText.trimToEmpty(apiKey);
        defaultTenantId = SdkText.trimToDefault(defaultTenantId, "default");
        defaultModelId = SdkText.trimToEmpty(defaultModelId);
        runStorePath = SdkText.trimToEmpty(runStorePath);
        storage = storage == null ? WayangStorageConfig.fromRunStorePath(runStorePath) : storage;
        if (runStorePath.isBlank() && storage.backend() == WayangStorageBackend.FILE) {
            runStorePath = storage.filePath();
        }
        readinessProfileRegistry = readinessProfileRegistry == null
                ? WayangPlatformReadinessProfileRegistryConfig.builtin()
                : readinessProfileRegistry;
    }

    public static WayangGollekSdkConfig local() {
        return new WayangGollekSdkConfig(WayangGollekSdkProvider.Mode.LOCAL, "", "", "default", "", "");
    }

    public static WayangGollekSdkConfig remote(String endpoint, String apiKey) {
        return new WayangGollekSdkConfig(WayangGollekSdkProvider.Mode.REMOTE, endpoint, apiKey, "default", "", "");
    }

    public WayangGollekSdkConfig withStorage(WayangStorageConfig storage) {
        WayangStorageConfig resolved = storage == null ? WayangStorageConfig.memory() : storage;
        return new WayangGollekSdkConfig(
                mode,
                endpoint,
                apiKey,
                defaultTenantId,
                defaultModelId,
                resolved.effectiveFilePath(),
                resolved,
                readinessProfileRegistry);
    }

    public WayangGollekSdkConfig withReadinessProfileRegistry(
            WayangPlatformReadinessProfileRegistryConfig readinessProfileRegistry) {
        return new WayangGollekSdkConfig(
                mode,
                endpoint,
                apiKey,
                defaultTenantId,
                defaultModelId,
                runStorePath,
                storage,
                readinessProfileRegistry);
    }
}
