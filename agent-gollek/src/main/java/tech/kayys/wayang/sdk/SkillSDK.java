package tech.kayys.gamelan.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/**
 * Skill SDK — strongly typed, locally testable skill development kit.
 *
 * <h2>Developer Experience Goals</h2>
 * <ol>
 *   <li><b>Strongly typed</b>: Skills define their parameter schemas using Java records.
 *       Validation happens before the LLM is involved.</li>
 *   <li><b>Locally testable</b>: Skills can be unit-tested without a running LLM.
 *       The SDK provides a {@link SkillTestHarness} that simulates the agent environment.</li>
 *   <li><b>Hot-reload</b>: Changes to SKILL.md are picked up without restart in dev mode.</li>
 *   <li><b>Scaffolding</b>: {@link SkillScaffolder} generates the correct directory structure
 *       from a fluent builder.</li>
 *   <li><b>Schema validation</b>: The SDK validates SKILL.md against the agentskills.io spec.</li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>
 * // Build a skill programmatically
 * SkillDefinition skill = SkillDefinition.builder("my-analyzer")
 *     .description("Analyzes Java code for common issues")
 *     .allowedTools("read_file", "search_files")
 *     .trigger("user asks to review code")
 *     .trigger("user asks to find bugs")
 *     .instruction("Always read the file first before making suggestions")
 *     .instruction("Focus on: null handling, exception handling, performance")
 *     .parameter("path", "string", true, "The file or directory to analyze")
 *     .build();
 *
 * // Write to disk
 * SkillScaffolder.scaffold(skill, Path.of("~/.gamelan/skills"));
 *
 * // Test locally without LLM
 * SkillTestHarness harness = new SkillTestHarness(skill);
 * SkillTestResult result = harness.run("review the code in UserService.java");
 * assertThat(result.toolCallsMade()).contains("read_file");
 * </pre>
 */
public final class SkillSDK {

    private static final Logger log = LoggerFactory.getLogger(SkillSDK.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private SkillSDK() {}

    // ── Skill definition builder ───────────────────────────────────────────

    /**
     * Fluent builder for skill definitions, validated against the agentskills.io spec.
     */
    public static final class SkillDefinition {
        private final String            name;
        private String                  description = "";
        private String                  license     = "Apache-2.0";
        private String                  compatibility = "";
        private String                  version     = "1.0";
        private String                  author      = System.getProperty("user.name", "unknown");
        private final List<String>      allowedTools = new ArrayList<>();
        private final List<String>      triggers     = new ArrayList<>();
        private final List<String>      instructions = new ArrayList<>();
        private final List<SkillParam>  parameters   = new ArrayList<>();
        private final Map<String,String> metadata    = new LinkedHashMap<>();
        private boolean                 evolveEnabled = false;

        private SkillDefinition(String name) {
            validateName(name);
            this.name = name;
        }

        public static SkillDefinition builder(String name) { return new SkillDefinition(name); }

        public SkillDefinition description(String d)    { this.description   = d; return this; }
        public SkillDefinition license(String l)        { this.license       = l; return this; }
        public SkillDefinition compatibility(String c)  { this.compatibility = c; return this; }
        public SkillDefinition version(String v)        { this.version       = v; return this; }
        public SkillDefinition author(String a)         { this.author        = a; return this; }
        public SkillDefinition evolveEnabled(boolean e) { this.evolveEnabled = e; return this; }

        public SkillDefinition allowedTools(String... tools) {
            allowedTools.addAll(Arrays.asList(tools)); return this;
        }
        public SkillDefinition trigger(String t) { triggers.add(t); return this; }
        public SkillDefinition instruction(String i) { instructions.add(i); return this; }
        public SkillDefinition metadata(String k, String v) { metadata.put(k, v); return this; }

        public SkillDefinition parameter(String name, String type, boolean required, String desc) {
            parameters.add(new SkillParam(name, type, required, desc, null));
            return this;
        }
        public SkillDefinition parameter(String name, String type, boolean required,
                                         String desc, String defaultValue) {
            parameters.add(new SkillParam(name, type, required, desc, defaultValue));
            return this;
        }

        public SkillDefinition build() {
            if (description.isBlank())
                throw new IllegalStateException("Skill '" + name + "' requires a description");
            return this;
        }

        // ── Accessors ──────────────────────────────────────────────────────
        public String            name()          { return name; }
        public String            description()   { return description; }
        public String            license()       { return license; }
        public String            compatibility() { return compatibility; }
        public String            version()       { return version; }
        public String            author()        { return author; }
        public boolean           evolveEnabled() { return evolveEnabled; }
        public List<String>      allowedTools()  { return List.copyOf(allowedTools); }
        public List<String>      triggers()      { return List.copyOf(triggers); }
        public List<String>      instructions()  { return List.copyOf(instructions); }
        public List<SkillParam>  parameters()    { return List.copyOf(parameters); }
        public Map<String,String> metadata()     { return Collections.unmodifiableMap(metadata); }

        private static void validateName(String name) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Skill name cannot be blank");
            if (!name.matches("^[a-z0-9]([a-z0-9-]{0,62}[a-z0-9])?$"))
                throw new IllegalArgumentException(
                        "Skill name must be lowercase alphanumeric + hyphens: " + name);
        }
    }

    public record SkillParam(String name, String type, boolean required,
                              String description, String defaultValue) {}

    // ── Scaffolder ─────────────────────────────────────────────────────────

    /**
     * Generates the correct skill directory structure from a SkillDefinition.
     */
    public static final class SkillScaffolder {

        public static Path scaffold(SkillDefinition skill, Path skillsDir) throws IOException {
            Path skillDir = skillsDir.resolve(skill.name());
            Files.createDirectories(skillDir.resolve("references"));
            Files.createDirectories(skillDir.resolve("scripts"));
            Files.createDirectories(skillDir.resolve("assets"));

            String skillMd = generateSkillMd(skill);
            Files.writeString(skillDir.resolve("SKILL.md"), skillMd);

            String readme = generateReadme(skill);
            Files.writeString(skillDir.resolve("README.md"), readme);

            String testFile = generateTestTemplate(skill);
            Files.createDirectories(skillDir.resolve("tests"));
            Files.writeString(skillDir.resolve("tests").resolve("test_" + skill.name().replace("-","_") + ".py"), testFile);

            log.info("[sdk] scaffolded skill '{}' → {}", skill.name(), skillDir);
            return skillDir;
        }

        static String generateSkillMd(SkillDefinition s) {
            StringBuilder sb = new StringBuilder();
            sb.append("---\n");
            sb.append("name: ").append(s.name()).append("\n");
            sb.append("description: ").append(s.description()).append("\n");
            if (!s.license().isBlank())        sb.append("license: ").append(s.license()).append("\n");
            if (!s.compatibility().isBlank())   sb.append("compatibility: ").append(s.compatibility()).append("\n");
            if (s.evolveEnabled())             sb.append("evolve: \"true\"\n");
            sb.append("metadata:\n");
            sb.append("  author: ").append(s.author()).append("\n");
            sb.append("  version: \"").append(s.version()).append("\"\n");
            s.metadata().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
            if (!s.allowedTools().isEmpty())
                sb.append("allowed-tools: ").append(String.join(" ", s.allowedTools())).append("\n");
            sb.append("---\n\n");
            sb.append("# ").append(toTitleCase(s.name())).append("\n\n");
            sb.append(s.description()).append("\n\n");
            if (!s.triggers().isEmpty()) {
                sb.append("## When to activate\n");
                s.triggers().forEach(t -> sb.append("- ").append(t).append("\n"));
                sb.append("\n");
            }
            if (!s.instructions().isEmpty()) {
                sb.append("## Instructions\n\n");
                for (int i = 0; i < s.instructions().size(); i++) {
                    sb.append(i + 1).append(". ").append(s.instructions().get(i)).append("\n");
                }
                sb.append("\n");
            }
            if (!s.parameters().isEmpty()) {
                sb.append("## Parameters\n\n");
                s.parameters().forEach(p -> sb.append(String.format(
                        "- `%s` (%s)%s: %s%s\n", p.name(), p.type(),
                        p.required() ? " **required**" : " *(optional)*",
                        p.description(),
                        p.defaultValue() != null ? " Default: `" + p.defaultValue() + "`" : "")));
            }
            return sb.toString();
        }

        static String generateReadme(SkillDefinition s) {
            return "# " + toTitleCase(s.name()) + "\n\n" +
                    s.description() + "\n\n" +
                    "## Quick Start\n\n" +
                    "```bash\ngamelan skill show " + s.name() + "\n```\n\n" +
                    "## Testing\n\n" +
                    "```bash\ngamelan skill validate ./" + s.name() + "/\npython tests/test_" +
                    s.name().replace("-","_") + ".py\n```\n\n" +
                    "## License\n\n" + s.license() + "\n";
        }

        static String generateTestTemplate(SkillDefinition s) {
            return """
                    #!/usr/bin/env python3
                    \"\"\"
                    Skill test template for: %s
                    Generated by Gamelan Skill SDK %s
                    \"\"\"
                    import subprocess
                    import json
                    import sys

                    SKILL_NAME = "%s"

                    def test_skill_validates():
                        result = subprocess.run(
                            ["gamelan", "skill", "validate", ".."],
                            capture_output=True, text=True, cwd=".."
                        )
                        assert result.returncode == 0, f"Skill validation failed: {result.stdout}"
                        print("✓ Skill validates against agentskills.io spec")

                    def test_skill_is_loadable():
                        result = subprocess.run(
                            ["gamelan", "skill", "show", SKILL_NAME],
                            capture_output=True, text=True
                        )
                        assert result.returncode == 0, f"Skill not loadable: {result.stderr}"
                        assert SKILL_NAME in result.stdout
                        print(f"✓ Skill '{SKILL_NAME}' is loadable")

                    def test_skill_triggers_correctly():
                        # TODO: Add task-specific tests here
                        # Example:
                        # result = subprocess.run(
                        #     ["gamelan", "run", "--no-stream", "your trigger phrase here"],
                        #     capture_output=True, text=True
                        # )
                        # assert "expected_output" in result.stdout
                        print(f"✓ Trigger test placeholder for '{SKILL_NAME}'")

                    if __name__ == "__main__":
                        test_skill_validates()
                        test_skill_is_loadable()
                        test_skill_triggers_correctly()
                        print(f"\\n✅ All tests passed for skill '{SKILL_NAME}'")
                    """.formatted(s.name(), s.version(), s.name());
        }

        private static String toTitleCase(String s) {
            return Arrays.stream(s.split("-"))
                    .map(w -> w.isEmpty() ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1))
                    .reduce("", (a, b) -> a.isBlank() ? b : a + " " + b);
        }
    }

    // ── Test harness ───────────────────────────────────────────────────────

    /**
     * Local test harness for skills — simulates the agent environment without a running LLM.
     * Intercepts tool calls and records them for assertion.
     */
    public static final class SkillTestHarness {

        private final SkillDefinition skill;
        private final List<ToolCallRecord> recordedCalls = new ArrayList<>();
        private final Map<String, Function<Map<String,String>, String>> toolMocks = new LinkedHashMap<>();

        public SkillTestHarness(SkillDefinition skill) { this.skill = skill; }

        /** Registers a mock for a tool call. The function receives params and returns output. */
        public SkillTestHarness mockTool(String toolName,
                                         Function<Map<String, String>, String> mock) {
            toolMocks.put(toolName, mock);
            return this;
        }

        /** Registers a simple canned response for a tool. */
        public SkillTestHarness mockTool(String toolName, String cannedOutput) {
            toolMocks.put(toolName, params -> cannedOutput);
            return this;
        }

        /**
         * Simulates running a task with this skill active.
         * Records all tool calls made. In real execution these would go to the LLM;
         * here we simulate the skill instructions being followed deterministically.
         *
         * @param task the user's task description
         * @return test result for assertion
         */
        public SkillTestResult run(String task) {
            recordedCalls.clear();
            long start = System.currentTimeMillis();

            // Verify trigger matching
            boolean triggered = skill.triggers().isEmpty() ||
                    skill.triggers().stream()
                            .anyMatch(t -> task.toLowerCase().contains(
                                    t.toLowerCase().replaceAll("[^a-z0-9 ]", "")));

            if (!triggered) {
                return new SkillTestResult(skill.name(), task, false,
                        "SKILL_NOT_TRIGGERED", List.of(), List.of(),
                        "Task does not match any trigger. Triggers: " + skill.triggers(),
                        System.currentTimeMillis() - start);
            }

            // Validate allowed tools
            List<String> onlyAllowed = skill.allowedTools();
            List<String> violations = new ArrayList<>();

            // Simulate tool calls based on instructions (heuristic: look for tool keywords)
            for (String instruction : skill.instructions()) {
                String lower = instruction.toLowerCase();
                for (String tool : onlyAllowed.isEmpty() ?
                        List.of("read_file","search_files","write_file","run_command") :
                        onlyAllowed) {
                    if (lower.contains(tool.replace("_", " ")) ||
                        lower.contains(tool.replace("_", ""))) {
                        Map<String, String> params = Map.of("task", task);
                        String output = toolMocks.containsKey(tool)
                                ? toolMocks.get(tool).apply(params)
                                : "[mock output for " + tool + "]";
                        recordedCalls.add(new ToolCallRecord(tool, params, output, Instant.now()));
                    }
                }
            }

            return new SkillTestResult(skill.name(), task, true, null,
                    recordedCalls.stream().map(ToolCallRecord::toolName).distinct().toList(),
                    recordedCalls, null, System.currentTimeMillis() - start);
        }

        public List<ToolCallRecord> recordedCalls() { return List.copyOf(recordedCalls); }
    }

    // ── Schema validator ───────────────────────────────────────────────────

    /**
     * Validates a SkillDefinition against the agentskills.io specification.
     */
    public static List<String> validate(SkillDefinition skill) {
        List<String> errors = new ArrayList<>();
        if (skill.name().isBlank())        errors.add("'name' is required");
        if (skill.name().length() > 64)    errors.add("'name' must be ≤64 chars");
        if (skill.description().isBlank()) errors.add("'description' is required");
        if (skill.description().length() > 1024) errors.add("'description' must be ≤1024 chars");
        if (skill.instructions().isEmpty()) errors.add("At least one instruction is required");
        return errors;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record ToolCallRecord(
            String              toolName,
            Map<String, String> params,
            String              output,
            Instant             calledAt
    ) {}

    public record SkillTestResult(
            String           skillName,
            String           task,
            boolean          triggered,
            String           notTriggeredReason,
            List<String>     toolCallsMade,
            List<ToolCallRecord> allCalls,
            String           error,
            long             durationMs
    ) {
        public boolean success() { return triggered && error == null; }

        public String summary() {
            return String.format("[%s] '%s' | triggered=%b | tools=%s | %dms",
                    skillName, task.length() > 60 ? task.substring(0,60)+"…" : task,
                    triggered, toolCallsMade, durationMs);
        }
    }
}
