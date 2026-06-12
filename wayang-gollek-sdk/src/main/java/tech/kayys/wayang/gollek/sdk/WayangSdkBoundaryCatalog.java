package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Optional;

/**
 * SDK-owned boundary map for separating the current flat public package into
 * stable domains without forcing consumers to change imports yet.
 */
public final class WayangSdkBoundaryCatalog {

    public static final String SDK_ROOT_PACKAGE = "tech.kayys.wayang.gollek.sdk";
    public static final String DEFAULT_BOUNDARY_ID = "core";

    private static final List<WayangSdkBoundary> DEFAULT_BOUNDARIES = List.of(
            boundary(
                    DEFAULT_BOUNDARY_ID,
                    "SDK Core",
                    ".core",
                    "Client bootstrap, provider discovery, local SDK facade, and shared value helpers.",
                    List.of(
                            "WayangClient",
                            "WayangGollekSdk",
                            "WayangWireApi",
                            "LocalWayangGollekSdk",
                            "LocalWayangGollekSdkProvider",
                            "ComponentStatus",
                            "Sdk"),
                    List.of(),
                    List.of()),
            boundary(
                    "run",
                    "Run Lifecycle",
                    ".run",
                    "Agent run requests, planning, lifecycle snapshots, event timelines, and run-store operations.",
                    List.of(
                            "AgentRun",
                            "FileAgentRunStore",
                            "InMemoryAgentRunStore",
                            "WayangAgentRequestMapper",
                            "WayangRun",
                            "SurfacePolicy"),
                    List.of(AgentRunPlanningContract.SCHEMA, AgentRunLifecycleContract.SCHEMA),
                    List.of("core", "context", "platform", "storage")),
            boundary(
                    "context",
                    "Context And Harness",
                    ".context",
                    "Workspace inspection, harness planning, and contextual inputs for coding-agent products.",
                    List.of(
                            "Workspace",
                            "LocalWorkspaceInspector",
                            "Harness",
                            "LocalHarnessPlanner",
                            "WayangContext"),
                    List.of(),
                    List.of("core")),
            boundary(
                    "capability",
                    "Capabilities",
                    ".capability",
                    "Dynamic skills, provider capability discovery, MCP/RAG/tool affordances, and capability facets.",
                    List.of(
                            "AgentSkill",
                            "RegisteredSkill",
                            "SkillRegistry",
                            "WayangSkill",
                            "WayangProvider",
                            "WayangProviderCapability"),
                    List.of(WayangSkillContract.SCHEMA, WayangProviderCapabilityContract.SCHEMA),
                    List.of("core", "platform")),
            boundary(
                    "platform",
                    "Platform",
                    ".platform",
                    "Product surfaces, profiles, readiness, diagnostics, standards, redaction, and operator policy.",
                    List.of(
                            "Product",
                            "WayangProduct",
                            "WayangPlatform",
                            "WayangReadiness",
                            "WayangDiagnostics",
                            "WayangReport",
                            "WayangSecret",
                            "WayangStandard"),
                    List.of(
                            WayangPlatformContract.SCHEMA,
                            WayangReadinessContract.SCHEMA,
                            WayangStandardAlignmentContract.SCHEMA,
                            WayangStandardCatalogContract.SCHEMA),
                    List.of("core")),
            boundary(
                    "contract",
                    "Contracts",
                    ".contract",
                    "JSON contracts, schema generation, command coverage, integrity checks, and wire envelopes.",
                    List.of(
                            "WayangContract",
                            "WayangJson",
                            "AgentRunLifecycleContract",
                            "AgentRunPlanningContract",
                            "WayangCommandDiscoveryContract",
                            "WayangPlatformContract",
                            "WayangReadinessContract",
                            "WayangSkillContract",
                            "WayangProviderCapabilityContract",
                            "WayangStandardAlignmentContract",
                            "WayangStandardCatalogContract",
                            "WayangWorkbenchContract"),
                    List.of(
                            AgentRunPlanningContract.SCHEMA,
                            AgentRunLifecycleContract.SCHEMA,
                            WayangCommandDiscoveryContract.SCHEMA,
                            WayangContractCoverageContract.SCHEMA,
                            WayangPlatformContract.SCHEMA,
                            WayangReadinessContract.SCHEMA,
                            WayangSkillContract.SCHEMA,
                            WayangProviderCapabilityContract.SCHEMA,
                            WayangStandardAlignmentContract.SCHEMA,
                            WayangStandardCatalogContract.SCHEMA,
                            WayangWorkbenchContract.SCHEMA),
                    List.of("core")),
            boundary(
                    "workbench",
                    "Workbench",
                    ".workbench",
                    "Command discovery, product workbench models, command metadata, and shell-facing action catalogs.",
                    List.of("Workbench", "WayangWorkbench", "WayangCommand"),
                    List.of(WayangCommandDiscoveryContract.SCHEMA, WayangWorkbenchContract.SCHEMA),
                    List.of("core", "contract", "platform", "capability")),
            boundary(
                    "storage",
                    "Storage",
                    ".storage",
                    "Storage backend selection, object-storage configuration, and persistence readiness policy.",
                    List.of("WayangStorage", "WayangObjectStorage"),
                    List.of(),
                    List.of("core")),
            boundary(
                    "remote",
                    "Remote SDK Adapter",
                    ".remote",
                    "HTTP transport and remote provider bindings for API-backed Wayang products.",
                    List.of("Remote", "HttpWayangRemoteTransport", "WayangRemoteTransport"),
                    List.of(),
                    List.of("core", "run", "contract", "platform", "capability", "workbench", "storage")));

    private WayangSdkBoundaryCatalog() {
    }

    public static List<WayangSdkBoundary> defaultBoundaries() {
        return DEFAULT_BOUNDARIES;
    }

    public static List<String> knownBoundaryIds() {
        return DEFAULT_BOUNDARIES.stream()
                .map(WayangSdkBoundary::id)
                .toList();
    }

    public static String intendedPackage(String boundaryId) {
        return require(boundaryId).intendedPackage();
    }

    public static Optional<WayangSdkBoundary> find(String boundaryId) {
        String normalized = normalizeBoundaryId(boundaryId);
        return DEFAULT_BOUNDARIES.stream()
                .filter(boundary -> boundary.id().equals(normalized))
                .findFirst();
    }

    public static WayangSdkBoundary require(String boundaryId) {
        String normalized = normalizeBoundaryId(boundaryId);
        return find(normalized)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Wayang SDK boundary '" + normalized + "'. Known boundaries: "
                                + String.join(", ", knownBoundaryIds())));
    }

    public static Optional<WayangSdkBoundary> boundaryForClassName(String simpleClassName) {
        return DEFAULT_BOUNDARIES.stream()
                .filter(boundary -> boundary.ownsClassName(simpleClassName))
                .findFirst();
    }

    public static Optional<WayangSdkBoundary> boundaryForContractSchema(String schema) {
        return DEFAULT_BOUNDARIES.stream()
                .filter(boundary -> boundary.ownsContractSchema(schema))
                .findFirst();
    }

    public static String normalizeBoundaryId(String boundaryId) {
        return SdkText.trimToDefault(boundaryId, DEFAULT_BOUNDARY_ID);
    }

    private static WayangSdkBoundary boundary(
            String id,
            String name,
            String packageSuffix,
            String responsibility,
            List<String> classPrefixes,
            List<String> contractSchemas,
            List<String> dependsOn) {
        return new WayangSdkBoundary(
                id,
                name,
                SDK_ROOT_PACKAGE + packageSuffix,
                responsibility,
                classPrefixes,
                contractSchemas,
                dependsOn);
    }
}
