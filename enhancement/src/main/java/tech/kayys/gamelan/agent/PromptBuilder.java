package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.gamelan.context.ProjectContext;
import tech.kayys.gamelan.memory.AgentMemory;
import tech.kayys.gamelan.memory.MemoryHierarchy;
import tech.kayys.gamelan.skill.Skill;
import tech.kayys.gamelan.tool.BuiltInTools;

import java.time.LocalDate;
import java.util.List;

/**
 * Assembles the system prompt with 6 injected layers.
 *
 * <h2>Layers (token cost order: cheapest → most expensive)</h2>
 * <ol>
 *   <li><b>Identity + principles</b> — ~150 tokens (static)</li>
 *   <li><b>Project context</b> — ~300 tokens (cached, scanned at startup)</li>
 *   <li><b>Remembered context</b> — ~200 tokens (from AgentMemory, per project)</li>
 *   <li><b>Tool protocol</b> — ~100 tokens (static)</li>
 *   <li><b>Tool catalogue</b> — ~300 tokens (cached after PostConstruct)</li>
 *   <li><b>Active skill instructions</b> — ~200-400 tokens (per-turn, skill-selected)</li>
 * </ol>
 *
 * Total typical: ~1,300 tokens. Leaves ~2,700 tokens for conversation history
 * in a 4K-context model, or ~6,700 in an 8K model.
 *
 * <h2>REMEMBER protocol</h2>
 * The prompt instructs the model to emit {@code REMEMBER: key = value} lines
 * for facts it discovers. These are extracted by {@link AgentLoop} and stored
 * in {@link AgentMemory} for future sessions.
 */
@ApplicationScoped
public class PromptBuilder {

    @Inject BuiltInTools   builtInTools;
    @Inject ProjectContext projectContext;
    @Inject AgentMemory    memory;
    @Inject MemoryHierarchy memoryHierarchy;

    private volatile String cachedToolCatalogue;

    // ─────────────────────────────────────────────────────────────────────────
    // Prompt layers (static templates)
    // ─────────────────────────────────────────────────────────────────────────

    private static final String IDENTITY = """
        You are Gamelan, an expert AI software engineer assistant running locally.
        Date: %s | CWD: `%s`

        ## Operating Principles
        1. **Read before writing** — always inspect files before modifying them.
        2. **Minimal diffs** — use `apply_patch` for targeted changes, not full rewrites.
        3. **Be concrete** — show actual code, not just descriptions.
        4. **Match the codebase** — adopt existing style, naming, and patterns.
        5. **Surface failures** — report errors clearly; never swallow exceptions.
        6. **Think before complex tasks** — use the `think` tool to reason step-by-step.
        7. **Track progress** — use `todo` for multi-step tasks so users see progress.

        ## Memory Protocol
        When you discover a useful fact about this project, emit it on its own line:
        ```
        REMEMBER: <key> = <value>
        ```
        Examples:
        - `REMEMBER: test-command = mvn test -pl auth-service`
        - `REMEMBER: code-style = Google Java Style, 2-space indent`
        - `REMEMBER: db-migration-tool = Flyway`
        This fact will be available in future sessions automatically.
        """;

    private static final String TOOL_PROTOCOL = """
        ## Tool Use Protocol
        Emit `<tool_call>` blocks to invoke tools. Rules:
        - **One block per tool**. Multiple tool calls: emit them sequentially.
        - **Emit the block then STOP** — do not write prose after a tool call.
        - **Never guess results** — wait for `<tool_result>` before continuing.
        - Multi-line parameter values go verbatim inside the tag (patches, file content).

        ```xml
        <tool_call>
          <n>TOOL_NAME</n>
          <PARAM>VALUE</PARAM>
        </tool_call>
        ```

        Results arrive as:
        ```xml
        <tool_result name="TOOL_NAME">
        [content or error]
        </tool_result>
        ```
        """;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    public String buildSystemPrompt(List<Skill> relevantSkills) {
        StringBuilder sb = new StringBuilder(2048);

        // 1. Identity
        sb.append(String.format(IDENTITY,
                LocalDate.now(), System.getProperty("user.dir", ".")));
        sb.append("\n\n");

        // 2. Project context (auto-detected)
        String pctx = projectContext.contextBlock();
        if (!pctx.isBlank()) {
            sb.append(pctx).append("\n");
        }

        // 3a. Working memory (AgentMemory — short key-value facts)
        String mem = memory.promptBlock();
        if (!mem.isBlank()) {
            sb.append(mem).append("\n");
        }

        // 3b. Deep memory (MemoryHierarchy — vector-retrieved episodes, procedures, learned knowledge)
        // Note: task is not available here; skill descriptions serve as a proxy.
        // The orchestrator also calls memoryHierarchy.buildPromptBlock(task) separately
        // and appends it, so we skip here to avoid duplication when task is available.
        String deepMem = "";
        if (!deepMem.isBlank()) {
            sb.append(deepMem).append("\n");
        }

        // 4. Tool protocol
        sb.append(TOOL_PROTOCOL).append("\n");

        // 5. Tool catalogue (cached)
        sb.append("## Available Tools\n").append(toolCatalogue()).append("\n\n");

        // 6. Active skills
        if (!relevantSkills.isEmpty()) {
            sb.append("## Active Skill Guides\n");
            sb.append("Follow these instructions when handling the current request.\n\n");
            for (Skill skill : relevantSkills) {
                sb.append("### `").append(skill.name()).append("`\n");
                sb.append(skill.instructions().strip()).append("\n\n");
                if (!skill.references().isEmpty()) {
                    skill.references().forEach((name, content) -> {
                        sb.append("<reference name=\"").append(name).append("\">\n");
                        String truncated = content.length() > 3000
                                ? content.substring(0, 3000) + "\n...(truncated)"
                                : content;
                        sb.append(truncated).append("\n</reference>\n\n");
                    });
                }
            }
        }

        return sb.toString();
    }

    public String buildMinimalPrompt() {
        return String.format(IDENTITY, LocalDate.now(), System.getProperty("user.dir", "."))
                + "\n\n" + TOOL_PROTOCOL + "\n## Available Tools\n" + toolCatalogue();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String toolCatalogue() {
        if (cachedToolCatalogue == null) {
            synchronized (this) {
                if (cachedToolCatalogue == null) {
                    cachedToolCatalogue = builtInTools.describeAll();
                }
            }
        }
        return cachedToolCatalogue;
    }
}
