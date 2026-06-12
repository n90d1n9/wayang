package tech.kayys.wayang.agent.hermes;

import java.util.List;

/**
 * Stable identifiers for Hermes runtime extension ports.
 */
public final class HermesRuntimePortCatalog {

    public static final String EXECUTION = "execution";
    public static final String GATEWAY = "gateway";
    public static final String AUTOMATION = "automation";
    public static final String DELEGATION = "delegation";
    public static final String PROVIDER_ROUTING = "provider-routing";
    public static final String MEMORY_REFLECTION = "memory-reflection";
    public static final String TRAJECTORY_EXPORT = "trajectory-export";
    public static final String SKILL_PERSISTENCE = "skill-persistence";
    public static final String RUNTIME_JOURNAL = "runtime-journal";
    public static final String LEARNING_AUDIT = "learning-audit";
    public static final String SKILL_LINEAGE = "skill-lineage";
    public static final String RUNTIME_DIAGNOSTICS = "runtime-diagnostics";

    public static final List<String> REQUEST_DIRECTIVE_PORTS = List.of(
            EXECUTION,
            GATEWAY,
            AUTOMATION,
            DELEGATION,
            PROVIDER_ROUTING,
            MEMORY_REFLECTION,
            TRAJECTORY_EXPORT,
            SKILL_LINEAGE);

    public static final List<String> ADAPTER_BUNDLE_PORTS = List.of(
            EXECUTION,
            GATEWAY,
            AUTOMATION,
            DELEGATION,
            PROVIDER_ROUTING,
            MEMORY_REFLECTION,
            TRAJECTORY_EXPORT,
            SKILL_PERSISTENCE,
            RUNTIME_JOURNAL,
            LEARNING_AUDIT,
            SKILL_LINEAGE);

    public static final List<String> SUPPORT_PORTS = List.of(
            SKILL_PERSISTENCE,
            RUNTIME_JOURNAL,
            LEARNING_AUDIT,
            RUNTIME_DIAGNOSTICS,
            SKILL_LINEAGE);

    public static final List<String> ALL_PORTS = List.of(
            EXECUTION,
            GATEWAY,
            AUTOMATION,
            DELEGATION,
            PROVIDER_ROUTING,
            MEMORY_REFLECTION,
            TRAJECTORY_EXPORT,
            SKILL_PERSISTENCE,
            RUNTIME_JOURNAL,
            LEARNING_AUDIT,
            SKILL_LINEAGE,
            RUNTIME_DIAGNOSTICS);

    private HermesRuntimePortCatalog() {
    }

    public static boolean contains(String port) {
        return ALL_PORTS.contains(HermesDirectiveSupport.clean(port, ""));
    }
}
