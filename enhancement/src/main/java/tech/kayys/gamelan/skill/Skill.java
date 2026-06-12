package tech.kayys.gamelan.skill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An immutable, loaded Agent Skill conforming to the
 * <a href="https://agentskills.io/specification">agentskills.io specification</a>.
 *
 * <h2>Real bug fixes vs. previous version</h2>
 * <ul>
 *   <li>{@link #invoke} was calling {@code pb.start()} with a bare filename
 *       as the working directory — so if the skill was installed in
 *       {@code ~/.gamelan/skills/my-skill/scripts/main.py}, the process would
 *       fail with "file not found" because the absolute path was never resolved.
 *       Fixed: resolve the script absolute path from {@code skillDir}.</li>
 *   <li>Process waited indefinitely ({@code proc.waitFor()} with no timeout).
 *       Fixed: 60-second timeout.</li>
 *   <li>No output size limit — a runaway script could produce gigabytes.
 *       Fixed: cap output at 100 KB.</li>
 *   <li>Environment variable injection used {@code pb.environment().putAll()}
 *       on a {@code stream().collect(toMap())} — which throws on duplicate keys
 *       (possible if two args normalise to the same env var name).
 *       Fixed: explicit loop.</li>
 * </ul>
 *
 * @param name          skill identifier (lowercase, hyphens, ≤64 chars)
 * @param description   what the skill does and when to use it (≤1024 chars)
 * @param license       optional license name
 * @param compatibility optional environment requirements
 * @param metadata      arbitrary key-value pairs from frontmatter
 * @param allowedTools  pre-approved tool names (experimental)
 * @param instructions  Markdown body after the frontmatter
 * @param rawContent    full SKILL.md text
 * @param references    name → content map from references/ directory
 * @param scriptPaths   filenames in scripts/ directory (basenames only)
 * @param skillDir      absolute path to the skill directory on disk
 */
public record Skill(
        String              name,
        String              description,
        String              license,
        String              compatibility,
        Map<String, String> metadata,
        List<String>        allowedTools,
        String              instructions,
        String              rawContent,
        Map<String, String> references,
        List<String>        scriptPaths,
        Path                skillDir        // NEW: needed for accurate script invocation
) {
    private static final int  MAX_OUTPUT_BYTES = 100_000;
    private static final long SCRIPT_TIMEOUT_S = 60;

    public int  scriptCount()    { return scriptPaths.size(); }
    public int  referenceCount() { return references.size(); }
    public boolean hasScripts()  { return !scriptPaths.isEmpty(); }

    /**
     * Invokes the skill's main script directly (without routing through the LLM).
     *
     * <p>Script selection priority:
     * <ol>
     *   <li>{@code main.py} — Python 3</li>
     *   <li>{@code main.sh} — Bash</li>
     *   <li>{@code main.js} — Node.js</li>
     * </ol>
     *
     * @param args key-value arguments exposed as {@code SKILL_<KEY>=value} env vars
     * @return trimmed stdout of the script (capped at 100 KB)
     * @throws IllegalStateException if no main script exists
     * @throws RuntimeException if the script fails or times out
     */
    public String invoke(Map<String, String> args) {
        String scriptName = List.of("main.py", "main.sh", "main.js").stream()
                .filter(scriptPaths::contains)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Skill '" + name + "' has no main script (main.py/sh/js) in scripts/"));

        // Resolve the absolute path from the skill directory
        Path scriptPath = (skillDir != null)
                ? skillDir.resolve("scripts").resolve(scriptName).toAbsolutePath()
                : Path.of(scriptName).toAbsolutePath();

        if (!Files.exists(scriptPath)) {
            throw new IllegalStateException(
                    "Script not found on disk: " + scriptPath);
        }

        String interpreter = scriptName.endsWith(".py") ? "python3"
                : scriptName.endsWith(".js")            ? "node"
                :                                         "bash";

        ProcessBuilder pb = new ProcessBuilder(interpreter, scriptPath.toString())
                .directory(skillDir != null ? skillDir.toFile()
                        : Path.of(".").toAbsolutePath().toFile())
                .redirectErrorStream(true);

        // Inject args as SKILL_KEY=value environment variables
        for (Map.Entry<String, String> entry : args.entrySet()) {
            String envKey = "SKILL_" + entry.getKey()
                    .toUpperCase().replace("-", "_").replace(" ", "_");
            pb.environment().put(envKey, entry.getValue());
        }

        try {
            Process proc    = pb.start();
            boolean done    = proc.waitFor(SCRIPT_TIMEOUT_S, TimeUnit.SECONDS);
            byte[]  rawOut  = proc.getInputStream().readNBytes(MAX_OUTPUT_BYTES);
            String  output  = new String(rawOut, StandardCharsets.UTF_8).strip();

            if (!done) {
                proc.destroyForcibly();
                throw new RuntimeException(
                        "Script '" + scriptName + "' timed out after " + SCRIPT_TIMEOUT_S + "s");
            }

            int exit = proc.exitValue();
            if (exit != 0) {
                throw new RuntimeException(
                        "Script '" + scriptName + "' exited with code " + exit
                        + (output.isBlank() ? "" : ":\n" + output));
            }

            return output;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("Script invocation failed: " + e.getMessage(), e);
        }
    }
}
