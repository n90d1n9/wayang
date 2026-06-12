package tech.kayys.wayang.agent.core.registry;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.gollek.sdk.core.GollekSdk;
import tech.kayys.gollek.sdk.exception.SdkException;
import tech.kayys.gollek.spi.provider.ProviderCapabilities;
import tech.kayys.gollek.spi.provider.ProviderInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provider management for agents with tenant-aware configuration and routing.
 *
 * <p>This registry manages provider configurations, capabilities, and routing rules
 * specifically for agent workloads. It provides:
 * <ul>
 *   <li>Provider discovery and registration</li>
 *   <li>Capability-based provider selection</li>
 *   <li>Tenant-specific provider configurations</li>
 *   <li>Provider health monitoring</li>
 *   <li>Dynamic provider enablement/disablement</li>
 * </ul>
 *
 * <h2>Provider Categories:</h2>
 * <ul>
 *   <li><b>Local:</b> GGUF, ONNX, SafeTensors - for cost-effective inference</li>
 *   <li><b>Cloud:</b> OpenAI, Anthropic, Mistral - for complex reasoning</li>
 *   <li><b>Specialized:</b> Embedding, Reranking - for specific tasks</li>
 * </ul>
 *
 * @author Wayang AI Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class AgentProviderRegistry {

    private static final Logger LOG = Logger.getLogger(AgentProviderRegistry.class);

    @Inject
    GollekSdk gollekSdk;

    private final Map<String, ProviderConfig> providerConfigs = new ConcurrentHashMap<>();
    private final Map<String, ProviderCapabilities> capabilitiesCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> healthCache = new ConcurrentHashMap<>();
    private final Map<String, List<String>> tenantProviderOverrides = new ConcurrentHashMap<>();

    // Provider type constants
    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_CLOUD = "cloud";
    public static final String TYPE_SPECIALIZED = "specialized";

    private static final Set<String> LOCAL_PROVIDER_IDS = Set.of("gguf", "onnx", "safetensor", "triton", "llama-cpp");
    private static final Set<String> CLOUD_PROVIDER_IDS = Set.of("openai", "anthropic", "mistral", "gemini", "cerebras", "deepseek", "ollama");

    /**
     * Initialize the registry on application startup.
     */
    void onStart(@Observes StartupEvent ev) {
        LOG.info("Initializing AgentProviderRegistry");
        refreshProviders();
    }

    /**
     * Refresh the provider list from Gollek SDK.
     */
    public void refreshProviders() {
        try {
            List<ProviderInfo> providers = gollekSdk.listAvailableProviders();
            for (ProviderInfo provider : providers) {
                ProviderConfig config = new ProviderConfig(
                    provider.id(),
                    provider.name(),
                    categorizeProvider(provider.id()),
                    true, // enabled by default
                    provider.capabilities()
                );
                providerConfigs.put(provider.id(), config);
                capabilitiesCache.put(provider.id(), provider.capabilities());
                LOG.debugf("Registered provider: %s (%s)", provider.id(), config.type);
            }
            LOG.infof("Registered %d providers", providers.size());
        } catch (SdkException e) {
            LOG.errorf(e, "Failed to refresh providers");
        }
    }

    /**
     * Get all available providers.
     *
     * @return list of provider IDs
     */
    public List<String> getAllProviders() {
        return new ArrayList<>(providerConfigs.keySet());
    }

    /**
     * Get enabled providers.
     *
     * @return list of enabled provider IDs
     */
    public List<String> getEnabledProviders() {
        return providerConfigs.entrySet().stream()
            .filter(e -> e.getValue().enabled)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Get providers by type.
     *
     * @param type the provider type (local, cloud, specialized)
     * @return list of provider IDs
     */
    public List<String> getProvidersByType(String type) {
        return providerConfigs.entrySet().stream()
            .filter(e -> e.getValue().enabled && type.equals(e.getValue().type))
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Get local providers (GGUF, ONNX, etc.).
     *
     * @return list of local provider IDs
     */
    public List<String> getLocalProviders() {
        return getProvidersByType(TYPE_LOCAL);
    }

    /**
     * Get cloud providers (OpenAI, Anthropic, etc.).
     *
     * @return list of cloud provider IDs
     */
    public List<String> getCloudProviders() {
        return getProvidersByType(TYPE_CLOUD);
    }

    /**
     * Get providers that support tool calling.
     *
     * @return list of provider IDs with tool calling support
     */
    public List<String> getToolCallingProviders() {
        return providerConfigs.entrySet().stream()
            .filter(e -> e.getValue().enabled)
            .filter(e -> {
                ProviderCapabilities caps = capabilitiesCache.get(e.getKey());
                return caps != null && caps.isToolCalling();
            })
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Get providers that support streaming.
     *
     * @return list of provider IDs with streaming support
     */
    public List<String> getStreamingProviders() {
        return providerConfigs.entrySet().stream()
            .filter(e -> e.getValue().enabled)
            .filter(e -> {
                ProviderCapabilities caps = capabilitiesCache.get(e.getKey());
                return caps != null && caps.isStreaming();
            })
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Check if a provider is available and healthy.
     *
     * @param providerId the provider ID
     * @return true if provider is available and healthy
     */
    public boolean isProviderAvailable(String providerId) {
        ProviderConfig config = providerConfigs.get(providerId);
        if (config == null || !config.enabled) {
            return false;
        }

        // Check cached health status
        Boolean healthy = healthCache.get(providerId);
        if (healthy != null) {
            return healthy;
        }

        // Check actual health
        try {
            ProviderInfo info = gollekSdk.getProviderInfo(providerId);
            boolean isHealthy = info.healthStatus() != null &&
                info.healthStatus() ==
                    tech.kayys.gollek.spi.provider.ProviderHealth.Status.HEALTHY;
            healthCache.put(providerId, isHealthy);
            return isHealthy;
        } catch (SdkException e) {
            LOG.debugf("Provider %s health check failed: %s", providerId, e.getMessage());
            healthCache.put(providerId, false);
            return false;
        }
    }

    /**
     * Enable or disable a provider.
     *
     * @param providerId the provider ID
     * @param enabled whether to enable or disable
     */
    public void setProviderEnabled(String providerId, boolean enabled) {
        ProviderConfig config = providerConfigs.get(providerId);
        if (config != null) {
            config.enabled = enabled;
            LOG.infof("Provider %s %s", providerId, enabled ? "enabled" : "disabled");
        }
    }

    /**
     * Set tenant-specific provider overrides.
     *
     * @param tenantId the tenant ID
     * @param providerIds list of allowed provider IDs for this tenant
     */
    public void setTenantProviderOverrides(String tenantId, List<String> providerIds) {
        tenantProviderOverrides.put(tenantId, providerIds);
        LOG.infof("Set tenant %s provider overrides: %s", tenantId, providerIds);
    }

    /**
     * Get allowed providers for a tenant.
     *
     * @param tenantId the tenant ID
     * @return list of allowed provider IDs (or all if no overrides)
     */
    public List<String> getTenantAllowedProviders(String tenantId) {
        List<String> overrides = tenantProviderOverrides.get(tenantId);
        if (overrides != null) {
            return overrides.stream()
                .filter(this::isProviderAvailable)
                .toList();
        }
        return getEnabledProviders();
    }

    /**
     * Get provider capabilities.
     *
     * @param providerId the provider ID
     * @return provider capabilities or null if unknown
     */
    public ProviderCapabilities getCapabilities(String providerId) {
        return capabilitiesCache.get(providerId);
    }

    /**
     * Select best provider for a given capability requirement.
     *
     * @param requireToolCalling whether tool calling is required
     * @param requireStreaming whether streaming is required
     * @param preferLocal whether to prefer local providers
     * @return best matching provider ID or null
     */
    public String selectBestProvider(
            boolean requireToolCalling,
            boolean requireStreaming,
            boolean preferLocal) {

        List<String> candidates = getEnabledProviders().stream()
            .filter(this::isProviderAvailable)
            .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        // Filter by capabilities
        if (requireToolCalling) {
            candidates = candidates.stream()
                .filter(id -> {
                    ProviderCapabilities caps = capabilitiesCache.get(id);
                    return caps != null && caps.isToolCalling();
                })
                .toList();
        }

        if (requireStreaming) {
            candidates = candidates.stream()
                .filter(id -> {
                    ProviderCapabilities caps = capabilitiesCache.get(id);
                    return caps != null && caps.isStreaming();
                })
                .toList();
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Prefer local if requested
        if (preferLocal) {
            Optional<String> localProvider = candidates.stream()
                .filter(LOCAL_PROVIDER_IDS::contains)
                .findFirst();
            if (localProvider.isPresent()) {
                return localProvider.get();
            }
        }

        // Return first available
        return candidates.get(0);
    }

    /**
     * Clear health cache for a provider.
     *
     * @param providerId the provider ID
     */
    public void clearHealthCache(String providerId) {
        healthCache.remove(providerId);
    }

    /**
     * Clear all caches.
     */
    public void clearCaches() {
        healthCache.clear();
        capabilitiesCache.clear();
        refreshProviders();
    }

    /**
     * Categorize a provider by type.
     */
    private String categorizeProvider(String providerId) {
        String lower = providerId.toLowerCase();
        if (LOCAL_PROVIDER_IDS.contains(lower)) {
            return TYPE_LOCAL;
        } else if (CLOUD_PROVIDER_IDS.contains(lower)) {
            return TYPE_CLOUD;
        } else {
            return TYPE_SPECIALIZED;
        }
    }

    /**
     * Provider configuration.
     */
    public static class ProviderConfig {
        public final String id;
        public final String name;
        public final String type;
        public boolean enabled;
        public final ProviderCapabilities capabilities;

        public ProviderConfig(
                String id,
                String name,
                String type,
                boolean enabled,
                ProviderCapabilities capabilities) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.enabled = enabled;
            this.capabilities = capabilities;
        }

        @Override
        public String toString() {
            return String.format("ProviderConfig{id=%s, name=%s, type=%s, enabled=%b}",
                id, name, type, enabled);
        }
    }
}
