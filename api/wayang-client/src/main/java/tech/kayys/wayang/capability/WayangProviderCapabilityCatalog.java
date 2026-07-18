package tech.kayys.wayang.capability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SDK-owned catalog of Wayang provider capabilities exposed to product surfaces.
 *
 * <p>The catalog keeps community and add-on capability metadata in one stable
 * contract so CLIs, dashboards, and admin surfaces can discover optional modules
 * without loading those modules into the default community reactor.</p>
 */
public final class WayangProviderCapabilityCatalog {

    public static final String TYPE_SKILL = "skill";
    public static final String TYPE_MCP = "mcp";
    public static final String TYPE_RAG = "rag";
    public static final String TYPE_STORAGE = "storage";
    public static final String TYPE_STANDARD = "standard";
    public static final String TYPE_COMMERCE = "commerce";
    public static final String TYPE_RUNTIME = "runtime";

    private static final List<WayangProviderCapabilityDescriptor> DEFAULT_CAPABILITIES = List.of(
            capability(
                    "skills.dynamic",
                    "wayang-skills",
                    "skills",
                    TYPE_SKILL,
                    "Dynamic Skills",
                    "Discover, route, and govern skills through the shared Wayang skill registry.",
                    WayangProviderCapabilityState.AVAILABLE,
                    List.of("coding-agent", "assistant-agent", "workflow-platform", "platform-admin"),
                    List.of(),
                    List.of("skills", "routing", "governance"),
                    metadata("catalog", "WayangSkillCatalog")),
            capability(
                    "mcp.bridge",
                    "wayang-mcp",
                    "tools",
                    TYPE_MCP,
                    "MCP Bridge",
                    "Expose MCP servers and tools through Wayang provider boundaries.",
                    WayangProviderCapabilityState.PREVIEW,
                    List.of("coding-agent", "assistant-agent", "workflow-platform"),
                    List.of(),
                    List.of("mcp", "tools", "dynamic-tools"),
                    metadata("skillId", "mcp.bridge")),
            capability(
                    "rag.retrieve",
                    "wayang-rag",
                    "rag",
                    TYPE_RAG,
                    "RAG Retrieval",
                    "Retrieve grounded context chunks and citations for agent answers.",
                    WayangProviderCapabilityState.AVAILABLE,
                    List.of("assistant-agent", "workflow-platform", "platform-admin"),
                    List.of(),
                    List.of("rag", "retrieval", "citations"),
                    metadata("skillId", "rag.retrieve")),
            capability(
                    "storage.hybrid-persistence",
                    "wayang-storage",
                    "storage",
                    TYPE_STORAGE,
                    "Hybrid Persistence",
                    "Coordinate database, object storage, and file fallback persistence policies.",
                    WayangProviderCapabilityState.PREVIEW,
                    List.of("assistant-agent", "workflow-platform", "platform-admin"),
                    List.of(),
                    List.of("database", "object-storage", "file-fallback", "rustfs", "s3"),
                    metadata("fallback", "files")),
            capability(
                    "a2a.alignment",
                    "wayang-a2a",
                    "a2a",
                    TYPE_STANDARD,
                    "A2A Alignment",
                    "Track Agent2Agent protocol readiness through the shared standards registry.",
                    WayangProviderCapabilityState.AVAILABLE,
                    List.of("assistant-agent", "workflow-platform", "platform-admin"),
                    List.of(WayangStandardRegistry.A2A),
                    List.of("a2a", "standard", "interop"),
                    metadata("registry", "WayangStandardRegistry")),
            capability(
                    "a2ui.contracts",
                    "wayang-a2ui",
                    "a2ui",
                    TYPE_STANDARD,
                    "A2UI Contracts",
                    "Expose Agent-to-User Interface alignment as a pro/enterprise add-on capability.",
                    WayangProviderCapabilityState.PREVIEW,
                    List.of("assistant-agent", "workflow-platform", "platform-admin"),
                    List.of(WayangStandardRegistry.A2UI),
                    List.of("a2ui", "standard", "ui-contracts", "pro", "enterprise", "addon"),
                    metadata(
                            "activationProfile", "pro-enterprise-addons",
                            "modulePath", "a2ui",
                            "standardId", WayangStandardRegistry.A2UI,
                            "defaultCommunity", false,
                            "edition", "pro-enterprise")),
            capability(
                    "agentic-commerce.protocol",
                    "wayang-agentic-commerce",
                    "agentic-commerce",
                    TYPE_COMMERCE,
                    "Agentic Commerce Protocol",
                    "Expose Agentic Commerce readiness, connector, and persistence capability metadata.",
                    WayangProviderCapabilityState.PREVIEW,
                    List.of("assistant-agent", "workflow-platform", "platform-admin"),
                    List.of(WayangStandardRegistry.AGENTIC_COMMERCE),
                    List.of("agentic-commerce", "commerce", "connector", "persistence"),
                    metadata("standardId", WayangStandardRegistry.AGENTIC_COMMERCE)),
            capability(
                    "runtime.lifecycle",
                    "wayang-runtime",
                    "runtime",
                    TYPE_RUNTIME,
                    "Run Lifecycle",
                    "Track run status, events, inspection, cancellation, and audit metadata.",
                    WayangProviderCapabilityState.AVAILABLE,
                    List.of("coding-agent", "assistant-agent", "workflow-platform", "platform-admin"),
                    List.of(),
                    List.of("runtime", "runs", "events", "observability"),
                    metadata("service", "AgentRunLifecycleService")));

    private WayangProviderCapabilityCatalog() {
    }

    public static List<WayangProviderCapabilityDescriptor> defaultCapabilities() {
        return DEFAULT_CAPABILITIES;
    }

    public static WayangProviderCapabilityRegistry defaultRegistry() {
        return WayangProviderCapabilityRegistry.of(DEFAULT_CAPABILITIES);
    }

    public static int defaultAvailableCapabilityCount() {
        return (int) DEFAULT_CAPABILITIES.stream()
                .filter(WayangProviderCapabilityDescriptor::available)
                .count();
    }

    private static WayangProviderCapabilityDescriptor capability(
            String id,
            String providerId,
            String moduleId,
            String capabilityType,
            String name,
            String description,
            WayangProviderCapabilityState state,
            List<String> surfaceIds,
            List<String> standardIds,
            List<String> tags,
            Map<String, Object> metadata) {
        return new WayangProviderCapabilityDescriptor(
                id,
                providerId,
                "wayang",
                moduleId,
                capabilityType,
                name,
                description,
                state,
                surfaceIds,
                standardIds,
                tags,
                metadata);
    }

    private static Map<String, Object> metadata(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Map.of();
        }
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Provider metadata requires key/value pairs.");
        }
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            if (!(keyValues[index] instanceof String key)) {
                throw new IllegalArgumentException("Provider metadata keys must be strings.");
            }
            values.put(key, keyValues[index + 1]);
        }
        return Collections.unmodifiableMap(values);
    }
}
