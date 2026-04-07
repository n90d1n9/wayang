package tech.kayys.wayang.agent.shaker;

import java.util.*;

/**
 * Report of required components identified by workflow analysis.
 *
 * @param requiredExecutors Executor modules needed (prompt, tool, RAG, etc.)
 * @param requiredOrchestrators Orchestrator strategies needed (react, plan-execute, etc.)
 * @param requiredTools Tool implementations needed
 * @param requiredSkills Skill implementations needed
 * @param requiredMemoryTiers Memory tiers needed (short, long, episodic, etc.)
 * @param requiredBackends Backend adapters needed (gollek, gamelan, etc.)
 * @param requiredGuardrails Guardrail policies needed
 * @param requiredVectorStores Vector store implementations needed
 */
public record DependencyReport(
        Set<String> requiredExecutors,
        Set<String> requiredOrchestrators,
        Set<String> requiredTools,
        Set<String> requiredSkills,
        Set<String> requiredMemoryTiers,
        Set<String> requiredBackends,
        Set<String> requiredGuardrails,
        Set<String> requiredVectorStores) {

    /**
     * Create empty dependency report.
     */
    public static DependencyReport empty() {
        return new DependencyReport(
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>(),
            new LinkedHashSet<>()
        );
    }

    /**
     * Merge two dependency reports.
     */
    public DependencyReport merge(DependencyReport other) {
        Set<String> executors = new LinkedHashSet<>(requiredExecutors);
        executors.addAll(other.requiredExecutors);

        Set<String> orchestrators = new LinkedHashSet<>(requiredOrchestrators);
        orchestrators.addAll(other.requiredOrchestrators);

        Set<String> tools = new LinkedHashSet<>(requiredTools);
        tools.addAll(other.requiredTools);

        Set<String> skills = new LinkedHashSet<>(requiredSkills);
        skills.addAll(other.requiredSkills);

        Set<String> memory = new LinkedHashSet<>(requiredMemoryTiers);
        memory.addAll(other.requiredMemoryTiers);

        Set<String> backends = new LinkedHashSet<>(requiredBackends);
        backends.addAll(other.requiredBackends);

        Set<String> guardrails = new LinkedHashSet<>(requiredGuardrails);
        guardrails.addAll(other.requiredGuardrails);

        Set<String> vectorStores = new LinkedHashSet<>(requiredVectorStores);
        vectorStores.addAll(other.requiredVectorStores);

        return new DependencyReport(
            executors, orchestrators, tools, skills,
            memory, backends, guardrails, vectorStores
        );
    }

    /**
     * Get estimated JAR size based on required components.
     *
     * @return estimated size in MB
     */
    public int estimatedJarSizeMB() {
        int baseSize = 5;  // Base framework size
        int executorSize = requiredExecutors.size() * 2;  // ~2MB per executor
        int toolSize = requiredTools.size() * 1;  // ~1MB per tool
        int skillSize = requiredSkills.size() * 1;  // ~1MB per skill
        int memorySize = requiredMemoryTiers.size() * 2;  // ~2MB per memory tier
        int vectorSize = requiredVectorStores.size() * 3;  // ~3MB per vector store
        int backendSize = requiredBackends.size() * 10;  // ~10MB per backend

        return baseSize + executorSize + toolSize + skillSize + memorySize + vectorSize + backendSize;
    }

    /**
     * Get list of Maven dependencies required.
     */
    public List<MavenDependency> requiredMavenDependencies() {
        List<MavenDependency> deps = new ArrayList<>();

        // Backend dependencies
        for (String backend : requiredBackends) {
            deps.add(new MavenDependency(
                "tech.kayys.wayang",
                "wayang-backend-" + backend,
                "1.0.0-SNAPSHOT"
            ));
        }

        // Executor modules (these are typically included via parent POM)
        // Tools (typically included via skill definitions)
        // Memory tiers (included via parent POM)

        return deps;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Dependency Report ===\n");
        sb.append("Executors: ").append(requiredExecutors).append("\n");
        sb.append("Orchestrators: ").append(requiredOrchestrators).append("\n");
        sb.append("Tools: ").append(requiredTools).append("\n");
        sb.append("Skills: ").append(requiredSkills).append("\n");
        sb.append("Memory Tiers: ").append(requiredMemoryTiers).append("\n");
        sb.append("Backends: ").append(requiredBackends).append("\n");
        sb.append("Guardrails: ").append(requiredGuardrails).append("\n");
        sb.append("Vector Stores: ").append(requiredVectorStores).append("\n");
        sb.append("Estimated JAR Size: ").append(estimatedJarSizeMB()).append(" MB\n");
        return sb.toString();
    }

    /**
     * Maven dependency coordinate.
     */
    public record MavenDependency(String groupId, String artifactId, String version) {
        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }
}
