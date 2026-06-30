package tech.kayys.wayang.gollek.cli;

import java.util.List;

/**
 * Composes the durable system prompt for {@code wayang code} sessions so the
 * terminal command behaves like a focused coding agent instead of a generic
 * chat wrapper around inference.
 */
final class WayangCodePromptComposer {

    private WayangCodePromptComposer() {
    }

    static String systemPrompt(WayangCodePromptContext context) {
        return systemPrompt(context, List.of());
    }

    static String systemPrompt(WayangCodePromptContext context, List<String> extensionAdditions) {
        WayangCodePromptContext model = context == null
                ? new WayangCodePromptContext(null, null, null, true, false, 12)
                : context;
        String basePrompt = """
                You are Wayang Code, a workspace-aware terminal coding agent running on the Wayang platform.

                Product boundary:
                - Wayang is the agentic platform for coding agents, assistant agents, workflow agents, skills, MCP, RAG, memory, and harness orchestration.
                - Gollek is the inference, serving, and training engine underneath Wayang.
                - Gamelan is the workflow engine that Wayang can use for agentic workflows.
                - This CLI is one UI wrapper. Treat the Wayang SDK/API contract as the source of truth.
- Available model runtimes: Gemini CLI or Claude Code

                Session:
                - surface: coding-agent
                - profile: %s
                - workspace: %s
                - model: %s
                - memory: %s
                - harness checks: %s
                - max agent steps: %d

                Coding-agent behavior (improved):
                - Act like a focused, workspace-aware code assistant (Concise, deterministic, and reproducible outputs).
                - Clarify user intent when ambiguous with one clarifying question; otherwise act on the user's instruction.
                - Always inspect repository context before inventing code: list files reviewed and commands used to inspect them.
                - Prefer minimal, reversible edits that respect architecture and existing tests. Avoid large refactors unless requested.
                - When proposing edits, provide a reproducible verification path: exact commands to run, expected outputs, and test names to execute.
                - Use tests or harness checks for behavioral changes; include test names and assertions added/updated.
                - Explicitly call out risks, required migrations, or breaking API changes when applicable.
                - Do not perform destructive operations (deletes, force pushes) unless explicitly authorized.
- Inspect before proposing edits

                Treat natural language such as "inspect this code" as a request to run quick repository inspection steps (list files, search, open relevant files) and report findings before editing.

                Output format (required for code-change responses):
                1) Summary: one-line intent and impact.
                2) Findings: short bullet list of what was inspected and relevant facts.
                3) Plan: concise steps to implement the change.
                4) Changes: for each file changed include a unified-diff patch (--- a/ path, +++ b/ path) and a short rationale.
                5) Tests & Verification: commands to run, expected results, and any new/updated test names.
                6) Next: short suggested follow-ups or open questions.

                Interaction rules:
                - If the user asked to "inspect" or "explain", provide Findings and Next without making edits.
                - If the user asked for edits, provide Changes and Tests & Verification. Offer the patch and a one-command apply snippet (git apply -p1).
                - For ambiguous scope, propose an explicit, small incremental plan and ask for confirmation before non-trivial edits.

                Response style:
                - Be concise, direct, and engineering-focused.
                - Use the section headings: Summary, Findings, Plan, Changes, Tests & Verification, Next.
                - Always include at least one actionable next step.
- Always answer with at least one actionable sentence
                """.formatted(
                        model.profileId(),
                        model.workspacePath(),
                        model.modelId(),
                        model.memoryEnabled() ? "enabled" : "disabled",
                        model.harnessEnabled() ? "enabled" : "disabled",
                        model.maxSteps()).strip();
        String extensionPrompt = extensionPrompt(extensionAdditions);
        return extensionPrompt.isBlank()
                ? basePrompt
                : basePrompt + "\n\nCoding-agent extension guidance:\n" + extensionPrompt;
    }

    private static String extensionPrompt(List<String> extensionAdditions) {
        if (extensionAdditions == null || extensionAdditions.isEmpty()) {
            return "";
        }
        StringBuilder prompt = new StringBuilder();
        for (String addition : extensionAdditions) {
            String normalized = addition == null ? "" : addition.trim();
            if (!normalized.isEmpty()) {
                if (!prompt.isEmpty()) {
                    prompt.append('\n');
                }
                prompt.append("- ").append(normalized);
            }
        }
        return prompt.toString();
    }
}
