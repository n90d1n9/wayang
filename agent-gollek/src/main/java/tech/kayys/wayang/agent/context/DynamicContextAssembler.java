package tech.kayys.gamelan.agent.context;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.role.AgentRole;
import tech.kayys.gamelan.cache.semantic.SemanticEmbeddingCache;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.memory.hierarchy.*;
import tech.kayys.gamelan.planning.HierarchicalTaskPlanner;
import tech.kayys.gamelan.skill.Skill;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DynamicContextAssembler — assembles the optimal system prompt for each agent turn.
 *
 * <h2>The Context Window is a Finite Resource</h2>
 * Every token in the context window has opportunity cost: it displaces potentially
 * more useful information. The naive approach (stuff everything in) degrades quality
 * as the model attends poorly to the middle of long contexts.
 *
 * <h2>Slot-based Assembly</h2>
 * The context is divided into named slots with fixed token budgets:
 * <pre>
 * ┌─────────────────────────────────────────────────────────┐
 * │ IDENTITY    (150 tokens, MANDATORY)                     │
 * │ ROLE        (200 tokens, conditional on multi-agent)    │
 * │ PROJECT     (300 tokens, from ProjectContext)           │
 * │ MEMORY      (400 tokens, top semantic facts)            │
 * │ TASK        (500 tokens, current task + plan)           │
 * │ SKILLS      (600 tokens, top-3 relevant skills)         │
 * │ TOOLS       (300 tokens, allowed tools only)            │
 * │ SAFETY      (100 tokens, constraint reminders)          │
 * └─────────────────────────────────────────────────────────┘
 * Total: ~2,550 tokens — leaves ~5,450 for conversation history
 * in an 8K model, or ~13,450 in a 16K model.
 * </pre>
 *
 * <h2>Relevance-ranked Memory Injection</h2>
 * Semantic memory is ranked by cosine similarity to the current task,
 * not by recency or importance tier alone. The most relevant facts
 * occupy the MEMORY slot regardless of when they were learned.
 *
 * <h2>Role Injection</h2>
 * When operating in multi-agent mode (RoleAgent), the role persona and
 * tool constraints are injected into a dedicated ROLE slot, replacing the
 * generic "helpful assistant" identity.
 */
@ApplicationScoped
public class DynamicContextAssembler {

    private static final Logger log = LoggerFactory.getLogger(DynamicContextAssembler.class);

    // Slot token budgets
    private static final int SLOT_IDENTITY  = 150;
    private static final int SLOT_ROLE      = 200;
    private static final int SLOT_PROJECT   = 300;
    private static final int SLOT_MEMORY    = 400;
    private static final int SLOT_TASK      = 500;
    private static final int SLOT_SKILLS    = 600;
    private static final int SLOT_TOOLS     = 300;
    private static final int SLOT_SAFETY    = 100;

    @Inject GamelanConfig          config;
    @Inject SemanticMemory         semantic;
    @Inject EpisodicMemory         episodic;
    @Inject SemanticEmbeddingCache embedCache;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Assembles a context-aware system prompt for a standard (single-agent) turn.
     *
     * @param task           the current task text
     * @param relevantSkills skills selected for this turn
     * @param projectInfo    detected project metadata
     * @param toolCatalogue  available tools (pre-formatted)
     * @param totalBudget    max tokens for the entire system prompt
     * @return the assembled system prompt
     */
    public AssembledContext assemble(String task, List<Skill> relevantSkills,
                                      String projectInfo, String toolCatalogue,
                                      int totalBudget) {
        return assembleInternal(task, null, relevantSkills, projectInfo,
                toolCatalogue, null, totalBudget);
    }

    /**
     * Assembles a role-specific context for multi-agent mode.
     *
     * @param role  the agent role (drives persona and tool constraints)
     */
    public AssembledContext assembleForRole(String task, AgentRole role,
                                             List<Skill> relevantSkills,
                                             String projectInfo, String toolCatalogue,
                                             int totalBudget) {
        return assembleInternal(task, role, relevantSkills, projectInfo,
                toolCatalogue, null, totalBudget);
    }

    /**
     * Assembles a plan-aware context that includes the current execution plan.
     */
    public AssembledContext assembleWithPlan(String task, HierarchicalTaskPlanner.Plan plan,
                                              List<Skill> relevantSkills,
                                              String projectInfo, String toolCatalogue,
                                              int totalBudget) {
        return assembleInternal(task, null, relevantSkills, projectInfo,
                toolCatalogue, plan, totalBudget);
    }

    // ── Assembly pipeline ──────────────────────────────────────────────────

    private AssembledContext assembleInternal(String task, AgentRole role,
                                               List<Skill> skills, String projectInfo,
                                               String toolCatalogue,
                                               HierarchicalTaskPlanner.Plan plan,
                                               int totalBudget) {
        Instant start = Instant.now();
        List<ContextSlot> slots = new ArrayList<>();
        int usedTokens = 0;

        // Slot 1: Identity (always present)
        String identity = buildIdentity(role);
        slots.add(new ContextSlot("identity", identity, SLOT_IDENTITY, true));
        usedTokens += estimateTokens(identity);

        // Slot 2: Role persona (multi-agent only)
        if (role != null) {
            String roleBlock = buildRoleBlock(role);
            slots.add(new ContextSlot("role", roleBlock, SLOT_ROLE, true));
            usedTokens += estimateTokens(roleBlock);
        }

        // Slot 3: Project context
        if (projectInfo != null && !projectInfo.isBlank()) {
            String proj = truncateToTokens(projectInfo, SLOT_PROJECT);
            slots.add(new ContextSlot("project", proj, SLOT_PROJECT, false));
            usedTokens += estimateTokens(proj);
        }

        // Slot 4: Semantic memory (relevance-ranked to current task)
        int memBudget = Math.min(SLOT_MEMORY, totalBudget - usedTokens - 500);
        if (memBudget > 50) {
            String memBlock = buildMemoryBlock(task, memBudget);
            if (!memBlock.isBlank()) {
                slots.add(new ContextSlot("memory", memBlock, SLOT_MEMORY, false));
                usedTokens += estimateTokens(memBlock);
            }
        }

        // Slot 5: Task + plan
        int taskBudget = Math.min(SLOT_TASK, totalBudget - usedTokens - 600);
        if (taskBudget > 50) {
            String taskBlock = buildTaskBlock(task, plan, taskBudget);
            slots.add(new ContextSlot("task", taskBlock, SLOT_TASK, true));
            usedTokens += estimateTokens(taskBlock);
        }

        // Slot 6: Skills (relevance-ranked)
        int skillBudget = Math.min(SLOT_SKILLS, totalBudget - usedTokens - 400);
        if (skillBudget > 100 && !skills.isEmpty()) {
            List<Skill> topSkills = rankSkillsByRelevance(task, skills, 3);
            String skillBlock = buildSkillBlock(topSkills, skillBudget);
            slots.add(new ContextSlot("skills", skillBlock, SLOT_SKILLS, false));
            usedTokens += estimateTokens(skillBlock);
        }

        // Slot 7: Tool catalogue (filtered by role if applicable)
        int toolBudget = Math.min(SLOT_TOOLS, totalBudget - usedTokens - 100);
        if (toolBudget > 50 && toolCatalogue != null) {
            String filteredTools = filterToolsForRole(toolCatalogue, role, toolBudget);
            slots.add(new ContextSlot("tools", filteredTools, SLOT_TOOLS, false));
            usedTokens += estimateTokens(filteredTools);
        }

        // Slot 8: Safety reminders (always last)
        String safety = buildSafetyBlock(role);
        slots.add(new ContextSlot("safety", safety, SLOT_SAFETY, false));
        usedTokens += estimateTokens(safety);

        // Assemble final prompt
        String prompt = slots.stream()
                .map(ContextSlot::content)
                .collect(Collectors.joining("\n\n"));

        long elapsedMs = java.time.Duration.between(start, Instant.now()).toMillis();
        log.debug("[ctx-assembler] assembled {}t in {}ms: {} slots",
                usedTokens, elapsedMs, slots.size());

        return new AssembledContext(prompt, slots, usedTokens, totalBudget, elapsedMs);
    }

    // ── Slot builders ──────────────────────────────────────────────────────

    private String buildIdentity(AgentRole role) {
        String base = "You are Gamelan, an expert AI software engineering assistant.\n" +
                "Date: " + java.time.LocalDate.now() + " | CWD: `" +
                System.getProperty("user.dir", ".") + "`\n\n" +
                "## Core Principles\n" +
                "1. Read before writing — always inspect files before modifying them\n" +
                "2. Minimal diffs — prefer apply_patch over full rewrites\n" +
                "3. Be concrete — show actual code, not just descriptions\n" +
                "4. Surface failures — never swallow exceptions silently\n" +
                "5. Think before acting — use the think tool for complex tasks";
        if (role == null) return base;
        return base + "\n\nCurrently operating as: **" + role.name() + "**";
    }

    private String buildRoleBlock(AgentRole role) {
        return "## Active Role: " + role.name() + " (" + role.family() + ")\n\n" +
                role.persona() + "\n\n" +
                "**Response style**: " + role.constraint().description() + "\n" +
                "**Allowed tools**: " + String.join(", ", role.allowedTools());
    }

    private String buildMemoryBlock(String task, int tokenBudget) {
        // Rank semantic nodes by cosine similarity to the task
        List<SemanticMemory.KnowledgeNode> nodes = new ArrayList<>(semantic.allNodes().values());
        if (nodes.isEmpty()) return "";

        // Use cached embeddings for fast ranking
        float[] taskVec = embedCache.embed(task, SemanticEmbeddingCache.TtlType.FILE);
        if (taskVec.length > 0) {
            nodes.sort((a, b) -> {
                float[] va = embedCache.embed(a.fact(), SemanticEmbeddingCache.TtlType.STATIC);
                float[] vb = embedCache.embed(b.fact(), SemanticEmbeddingCache.TtlType.STATIC);
                double sa = cosineSim(taskVec, va), sb = cosineSim(taskVec, vb);
                return Double.compare(sb, sa); // descending
            });
        }

        StringBuilder sb = new StringBuilder("## Relevant Context from Memory\n");
        int used = 20;
        for (SemanticMemory.KnowledgeNode node : nodes) {
            String line = "- **" + node.concept() + "**: " + node.fact() + "\n";
            int cost = estimateTokens(line);
            if (used + cost > tokenBudget) break;
            sb.append(line);
            used += cost;
        }
        return used > 20 ? sb.toString() : "";
    }

    private String buildTaskBlock(String task, HierarchicalTaskPlanner.Plan plan, int budget) {
        StringBuilder sb = new StringBuilder("## Current Task\n").append(task);
        if (plan != null) {
            sb.append("\n\n## Execution Plan (").append(plan.tasks().size()).append(" steps)\n");
            sb.append("Mode: ").append(plan.mode()).append("\n");
            int i = 1;
            for (HierarchicalTaskPlanner.TaskNode t : plan.tasks()) {
                String line = i++ + ". " + t.task() + "\n";
                if (estimateTokens(sb.toString() + line) > budget) {
                    sb.append("... (").append(plan.tasks().size() - i + 2).append(" more steps)\n");
                    break;
                }
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String buildSkillBlock(List<Skill> skills, int budget) {
        if (skills.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("## Active Skill Guides\n");
        for (Skill s : skills) {
            String header = "### `" + s.name() + "`\n";
            String instructions = truncateToTokens(s.instructions(), budget / skills.size());
            String section = header + instructions + "\n\n";
            if (estimateTokens(sb.toString() + section) > budget) break;
            sb.append(section);
        }
        return sb.toString();
    }

    private String filterToolsForRole(String catalogue, AgentRole role, int budget) {
        if (role == null) return truncateToTokens(catalogue, budget);
        Set<String> allowed = role.allowedTools();
        String filtered = Arrays.stream(catalogue.split("\n"))
                .filter(line -> {
                    if (line.isBlank() || line.startsWith("#")) return true;
                    return allowed.stream().anyMatch(line::contains);
                })
                .collect(Collectors.joining("\n"));
        return "## Available Tools (filtered for role " + role.name() + ")\n" +
                truncateToTokens(filtered, budget - 40);
    }

    private String buildSafetyBlock(AgentRole role) {
        String base = "## Safety\n" +
                "Never delete files. Never run destructive commands without confirmation. " +
                "Prefer targeted edits over full rewrites.";
        if (role == AgentRole.EXECUTOR) {
            base += " Execute plan steps exactly as specified. Do not improvise.";
        } else if (role == AgentRole.CRITIC) {
            base += " Be rigorous. Never approve without finding at least one improvement.";
        }
        return base;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private List<Skill> rankSkillsByRelevance(String task, List<Skill> skills, int topK) {
        float[] taskVec = embedCache.embed(task, SemanticEmbeddingCache.TtlType.FILE);
        if (taskVec.length == 0) return skills.stream().limit(topK).toList();

        return skills.stream()
                .sorted(Comparator.comparingDouble((Skill s) -> {
                    float[] sv = embedCache.embed(
                            s.name() + " " + s.description(),
                            SemanticEmbeddingCache.TtlType.STATIC);
                    return -cosineSim(taskVec, sv);  // descending
                }))
                .limit(topK)
                .toList();
    }

    private double cosineSim(float[] a, float[] b) {
        if (a.length == 0 || b.length == 0) return 0;
        int len = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) {
            dot += (double) a[i] * b[i];
            na  += (double) a[i] * a[i];
            nb  += (double) b[i] * b[i];
        }
        double d = Math.sqrt(na) * Math.sqrt(nb);
        return d < 1e-9 ? 0 : dot / d;
    }

    private int estimateTokens(String s) {
        return s == null ? 0 : Math.max(1, s.length() / 4);
    }

    private String truncateToTokens(String s, int tokenBudget) {
        if (s == null) return "";
        int maxChars = tokenBudget * 4;
        return s.length() > maxChars ? s.substring(0, maxChars) + "\n...(truncated)" : s;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record ContextSlot(
            String  name,
            String  content,
            int     tokenBudget,
            boolean mandatory
    ) {
        public int estimatedTokens() { return content.length() / 4; }
    }

    public record AssembledContext(
            String           prompt,
            List<ContextSlot> slots,
            int              usedTokens,
            int              totalBudget,
            long             assemblyMs
    ) {
        public double utilizationRate() { return (double) usedTokens / totalBudget; }
        public int    remainingTokens() { return totalBudget - usedTokens; }

        public String slotSummary() {
            return slots.stream()
                    .map(s -> s.name() + ":" + s.estimatedTokens() + "t")
                    .collect(Collectors.joining(", "));
        }

        public String summary() {
            return String.format("Context: %d/%d tokens (%.0f%%) in %dms | slots=[%s]",
                    usedTokens, totalBudget, utilizationRate() * 100, assemblyMs, slotSummary());
        }
    }
}
