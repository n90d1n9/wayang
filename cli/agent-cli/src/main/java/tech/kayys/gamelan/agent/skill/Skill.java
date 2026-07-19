package tech.kayys.gamelan.agent.skill;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Represents a loaded Agent Skill conforming to the
 * <a href="https://agentskills.io/specification">agentskills.io specification</a>.
 *
 * @param name           skill identifier (lowercase alphanumeric + hyphens, ≤64 chars)
 * @param description    what the skill does and when to use it
 * @param version        optional version string
 * @param source         where the skill was loaded from (builtin, local, global)
 * @param enabled        whether the skill is active
 * @param path           filesystem path to the skill directory
 * @param license        optional license name or file reference
 * @param compatibility  optional environment requirements
 * @param metadata       arbitrary key-value metadata
 * @param allowedTools   pre-approved tool names
 * @param keywords       keywords for skill selection matching
 * @param commands       list of commands this skill provides
 * @param dependencies   list of skill dependencies
 * @param instructions   the Markdown body of SKILL.md
 * @param rawContent     the full SKILL.md content as-is
 * @param references     name → content map of files in the references/ directory
 * @param scriptPaths    names of scripts in the scripts/ directory
 */
public record Skill(
        String name,
        String description,
        String version,
        String source,
        boolean enabled,
        Path path,
        String license,
        String compatibility,
        Map<String, String> metadata,
        List<String> allowedTools,
        List<String> keywords,
        List<String> commands,
        List<String> dependencies,
        String instructions,
        String rawContent,
        Map<String, String> references,
        List<String> scriptPaths
) {

    public int scriptCount() { return scriptPaths != null ? scriptPaths.size() : 0; }
    public int referenceCount() { return references != null ? references.size() : 0; }

    /**
     * Invokes the skill directly (not via LLM routing).
     */
    public String invoke(Map<String, String> args) {
        String mainScript = scriptPaths.stream()
                .filter(s -> s.startsWith("main."))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Skill '" + name + "' has no main script in scripts/"));

        ProcessBuilder pb = new ProcessBuilder();
        pb.environment().putAll(args.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> "SKILL_" + e.getKey().toUpperCase().replace("-", "_"),
                        Map.Entry::getValue)));

        String interpreter = mainScript.endsWith(".py") ? "python3"
                : mainScript.endsWith(".js") ? "node"
                : "bash";

        pb.command(interpreter, mainScript);
        pb.redirectErrorStream(true);

        try {
            Process proc = pb.start();
            String output = new String(proc.getInputStream().readAllBytes());
            proc.waitFor();
            return output;
        } catch (Exception e) {
            throw new RuntimeException("Script invocation failed: " + e.getMessage(), e);
        }
    }
}
