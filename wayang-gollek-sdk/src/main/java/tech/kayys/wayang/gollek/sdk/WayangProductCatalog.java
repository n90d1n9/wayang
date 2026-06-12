package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WayangProductCatalog {

    public static final String DEFAULT_SURFACE_ID = "coding-agent";

    private static final List<ProductSurface> DEFAULT_SURFACES = List.of(
            new ProductSurface(
                    DEFAULT_SURFACE_ID,
                    "Coding Agent",
                    "Workspace-aware terminal agent in the style of Gemini CLI or Claude Code.",
                    List.of("planning", "tool-use", "repo-context", "patching", "harness-checks"),
                    List.of("tools-spi", "agent-orchestration", "agent-shaker")),
            new ProductSurface(
                    "assistant-agent",
                    "Assistant Agent",
                    "Task and conversation agent with memory, RAG, MCP, and skill routing.",
                    List.of("chat", "memory", "rag", "skills", "mcp"),
                    List.of("agent-spi", "rag-runtime", "memory-runtime", "agent-mcp")),
            new ProductSurface(
                    "workflow-platform",
                    "Low-Code Agentic Workflow",
                    "Workflow product surface for Gamelan or n8n-style agent nodes and automations.",
                    List.of("workflow-runs", "signals", "human-in-loop", "agent-nodes", "observability"),
                    List.of("workflow-backend", "agent-backend-gamelan", "hitl", "runtime-quarkus")),
            new ProductSurface(
                    "platform-admin",
                    "Platform Admin",
                    "Operator surface for tenants, skills, tools, plugins, RAG, SLOs, and runtime health.",
                    List.of("tenancy", "skills", "plugins", "rag-admin", "slo-admin"),
                    List.of("agent-api", "skill-management", "rag-config", "wayang-rag-slo")));

    private static final List<ProductSurfacePolicy> DEFAULT_POLICIES = List.of(
            new ProductSurfacePolicy(
                    DEFAULT_SURFACE_ID,
                    true,
                    true,
                    true,
                    false,
                    List.of("repo", "tools", "patching"),
                    List.of("surfaceId", "workspace"),
                    List.of("inspect-workspace", "plan-harness", "prefer-tool-use")),
            new ProductSurfacePolicy(
                    "assistant-agent",
                    true,
                    false,
                    false,
                    false,
                    List.of("memory", "rag", "mcp"),
                    List.of("surfaceId"),
                    List.of("prefer-memory", "prefer-rag", "allow-mcp-skills")),
            new ProductSurfacePolicy(
                    "workflow-platform",
                    true,
                    false,
                    false,
                    true,
                    List.of("workflow", "hitl", "observability"),
                    List.of("surfaceId", "workflowId"),
                    List.of("prefer-gamelan-workflow", "preserve-human-in-loop", "emit-run-metadata")),
            new ProductSurfacePolicy(
                    "platform-admin",
                    false,
                    false,
                    false,
                    false,
                    List.of("skills", "rag-admin", "slo-admin"),
                    List.of("surfaceId", "tenantId"),
                    List.of("prefer-admin-audit", "require-tenant-scope", "avoid-implicit-tool-execution")));

    private static final List<ProductProfile> DEFAULT_PROFILES = List.of(
            profile(
                    "coding-agent",
                    "Coding Agent",
                    DEFAULT_SURFACE_ID,
                    "Workspace-aware coding agent with repo context, tool runtime, patching, and harness checks.",
                    "Describe the code change or investigation.",
                    "",
                    List.of("repo", "tools", "patching"),
                    true,
                    true,
                    true,
                    false,
                    true,
                    12,
                    80,
                    8,
                    List.of("Gemini CLI and Claude Code style shell profile.")),
            profile(
                    "openclaw-agent",
                    "OpenClaw Coding Agent",
                    DEFAULT_SURFACE_ID,
                    "OpenClaw-style coding agent profile on the shared Wayang engine.",
                    "Describe the OpenClaw coding task.",
                    "",
                    List.of("repo", "tools", "patching", "mcp"),
                    true,
                    true,
                    true,
                    false,
                    true,
                    16,
                    120,
                    10,
                    List.of("Use when a coding-agent product wants richer MCP-backed tool discovery.")),
            profile(
                    "assistant-agent",
                    "Assistant Agent",
                    "assistant-agent",
                    "Conversation assistant with memory, RAG, and MCP skill routing.",
                    "Describe the assistant task.",
                    "",
                    List.of("memory", "rag", "mcp"),
                    true,
                    false,
                    false,
                    true,
                    true,
                    12,
                    80,
                    8,
                    List.of("Use for chat, support, and knowledge assistant products.")),
            profile(
                    "workflow-agent",
                    "Gamelan Workflow Agent",
                    "workflow-platform",
                    "Agent profile for Gamelan workflow execution with HITL and observability skills.",
                    "Describe the workflow objective.",
                    "gamelan-workflow",
                    List.of("workflow", "hitl", "observability"),
                    true,
                    false,
                    false,
                    true,
                    true,
                    12,
                    80,
                    8,
                    List.of("Use for direct Gamelan-backed agent workflows.")),
            profile(
                    "low-code-agent",
                    "Low-Code Agentic Workflow",
                    "workflow-platform",
                    "Low-code agentic workflow profile for n8n-style product shells.",
                    "Describe the low-code workflow objective.",
                    "gamelan-low-code-workflow",
                    List.of("workflow", "hitl", "observability"),
                    true,
                    false,
                    false,
                    true,
                    true,
                    12,
                    80,
                    8,
                    List.of("Use for visual workflow builders and automation nodes.")),
            profile(
                    "platform-admin",
                    "Platform Admin",
                    "platform-admin",
                    "Operator profile for tenants, skills, RAG administration, SLOs, and runtime health.",
                    "Describe the platform administration task.",
                    "",
                    List.of("skills", "rag-admin", "slo-admin"),
                    false,
                    false,
                    false,
                    true,
                    true,
                    10,
                    80,
                    8,
                    List.of("Use for operator consoles and admin automation.")));

    private WayangProductCatalog() {
    }

    public static List<ProductSurface> defaultSurfaces() {
        return DEFAULT_SURFACES;
    }

    public static List<ProductSurfacePolicy> defaultPolicies() {
        return DEFAULT_POLICIES;
    }

    public static List<ProductProfile> defaultProfiles() {
        return DEFAULT_PROFILES;
    }

    public static List<String> knownSurfaceIds() {
        return DEFAULT_SURFACES.stream()
                .map(ProductSurface::id)
                .toList();
    }

    public static List<String> knownProfileIds() {
        return DEFAULT_PROFILES.stream()
                .map(ProductProfile::id)
                .toList();
    }

    public static String normalizeSurfaceId(String surfaceId) {
        return SdkText.trimToDefault(surfaceId, DEFAULT_SURFACE_ID);
    }

    public static Optional<ProductSurface> findSurface(String surfaceId) {
        String normalized = normalizeSurfaceId(surfaceId);
        return DEFAULT_SURFACES.stream()
                .filter(surface -> surface.id().equals(normalized))
                .findFirst();
    }

    public static boolean isKnownSurface(String surfaceId) {
        return findSurface(surfaceId).isPresent();
    }

    public static String requireKnownSurfaceId(String surfaceId) {
        String normalized = normalizeSurfaceId(surfaceId);
        if (isKnownSurface(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException(
                "Unknown Wayang product surface '" + normalized + "'. Known surfaces: "
                        + String.join(", ", knownSurfaceIds()));
    }

    public static ProductSurfacePolicy policyFor(String surfaceId) {
        String normalized = requireKnownSurfaceId(surfaceId);
        return DEFAULT_POLICIES.stream()
                .filter(policy -> policy.surfaceId().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing policy for known Wayang product surface " + normalized));
    }

    public static List<ProductProfile> profilesForSurface(String surfaceId) {
        String normalized = requireKnownSurfaceId(surfaceId);
        return DEFAULT_PROFILES.stream()
                .filter(profile -> profile.surfaceId().equals(normalized))
                .toList();
    }

    public static Optional<ProductProfile> findProfile(String profileId) {
        String normalized = SdkText.trimToEmpty(profileId);
        return DEFAULT_PROFILES.stream()
                .filter(profile -> profile.id().equals(normalized))
                .findFirst();
    }

    public static ProductProfile profileFor(String profileId) {
        String normalized = SdkText.trimToEmpty(profileId);
        return findProfile(normalized)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Wayang product profile '" + normalized + "'. Known profiles: "
                                + String.join(", ", knownProfileIds())));
    }

    private static ProductProfile profile(
            String id,
            String name,
            String surfaceId,
            String description,
            String starterPrompt,
            String workflowId,
            List<String> skills,
            boolean memoryEnabled,
            boolean workspaceEnabled,
            boolean harnessEnabled,
            boolean harnessIncludeOptional,
            boolean requireReady,
            int maxSteps,
            int workspaceMaxEntries,
            int harnessMaxChecks,
            List<String> notes) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("wayang.profile", id);
        context.put("wayang.surface", surfaceId);
        return new ProductProfile(
                id,
                name,
                surfaceId,
                description,
                starterPrompt,
                workflowId,
                skills,
                memoryEnabled,
                workspaceEnabled,
                harnessEnabled,
                harnessIncludeOptional,
                requireReady,
                maxSteps,
                workspaceMaxEntries,
                harnessMaxChecks,
                context,
                notes);
    }
}
