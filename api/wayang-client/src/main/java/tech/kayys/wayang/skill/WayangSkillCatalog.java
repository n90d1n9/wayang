package tech.kayys.wayang.skill;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.skill.AgentSkillDescriptor;

public final class WayangSkillCatalog {

    private static final List<RegisteredSkill> DEFAULT_SKILLS = List.of(
            skill(
                    "repo.context",
                    "Workspace Context",
                    "Inspect repository context and summarize files for coding-agent runs.",
                    "Context",
                    "builtin",
                    List.of("coding-agent"),
                    List.of("workspace", "maxEntries"),
                    List.of("summary", "files"),
                    List.of("repo", "workspace"),
                    List.of("repo")),
            skill(
                    "tools.runtime",
                    "Tool Runtime",
                    "Route approved tool calls through Wayang tool boundaries.",
                    "Tools",
                    "tools",
                    List.of("coding-agent", "assistant-agent", "workflow-platform"),
                    List.of("tool.request"),
                    List.of("tool.result"),
                    List.of("tools", "sandbox"),
                    List.of("tools")),
            skill(
                    "patching.apply",
                    "Patch Application",
                    "Apply workspace edits through the coding-agent patch boundary.",
                    "Coding",
                    "builtin",
                    List.of("coding-agent"),
                    List.of("diff", "workspace"),
                    List.of("patch", "files"),
                    List.of("patching", "repo"),
                    List.of("patching")),
            skill(
                    "memory.session",
                    "Session Memory",
                    "Attach conversation and task memory to agent runs.",
                    "Memory",
                    "memory",
                    List.of("assistant-agent", "workflow-platform"),
                    List.of("sessionId", "userId"),
                    List.of("memory.context"),
                    List.of("memory"),
                    List.of("memory")),
            skill(
                    "rag.retrieve",
                    "RAG Retrieval",
                    "Retrieve grounded context chunks and citations for agent answers.",
                    "Retrieval",
                    "rag",
                    List.of("assistant-agent", "workflow-platform", "platform-admin"),
                    List.of("query", "collection", "filters"),
                    List.of("chunks", "citations"),
                    List.of("rag", "docs"),
                    List.of("rag")),
            skill(
                    "mcp.bridge",
                    "MCP Bridge",
                    "Expose MCP servers and tools through Wayang skill discovery.",
                    "Tools",
                    "mcp",
                    List.of("coding-agent", "assistant-agent", "workflow-platform"),
                    List.of("server", "tool", "arguments"),
                    List.of("tool.result", "metadata"),
                    List.of("mcp", "tools"),
                    List.of("mcp")),
            skill(
                    "workflow.gamelan",
                    "Gamelan Workflow",
                    "Bind agent runs to Gamelan workflow execution and metadata.",
                    "Workflow",
                    "gamelan",
                    List.of("workflow-platform", "coding-agent"),
                    List.of("workflowId", "inputs"),
                    List.of("runPlan", "events"),
                    List.of("workflow", "gamelan"),
                    List.of("workflow")),
            skill(
                    "hitl.approval",
                    "Human Approval",
                    "Pause agentic workflows for human-in-the-loop decisions.",
                    "Governance",
                    "hitl",
                    List.of("workflow-platform", "platform-admin"),
                    List.of("approval.request", "tenantId"),
                    List.of("decision", "audit"),
                    List.of("hitl", "approval"),
                    List.of("hitl")),
            skill(
                    "observability.traces",
                    "Runtime Traces",
                    "Expose run events, lifecycle traces, and operational metadata.",
                    "Observability",
                    "runtime",
                    List.of("coding-agent", "assistant-agent", "workflow-platform", "platform-admin"),
                    List.of("runId"),
                    List.of("events", "summary"),
                    List.of("observability", "traces"),
                    List.of("observability")),
            skill(
                    "skills.management",
                    "Skill Management",
                    "Manage skill definitions, lifecycle state, artifacts, and audit.",
                    "Administration",
                    "skill-management",
                    List.of("platform-admin"),
                    List.of("skill.definition", "tenantId"),
                    List.of("status", "audit"),
                    List.of("skills", "admin"),
                    List.of("skills")),
            skill(
                    "rag.admin",
                    "RAG Administration",
                    "Manage RAG collections, plugins, retrieval config, and ingestion status.",
                    "Administration",
                    "rag",
                    List.of("platform-admin"),
                    List.of("collection", "tenantId"),
                    List.of("status", "pluginStatus"),
                    List.of("rag-admin", "rag"),
                    List.of("rag-admin")),
            skill(
                    "slo.admin",
                    "SLO Administration",
                    "Inspect and tune RAG/runtime service-level objectives.",
                    "Administration",
                    "rag",
                    List.of("platform-admin"),
                    List.of("thresholds", "tenantId"),
                    List.of("sloStatus", "alerts"),
                    List.of("slo-admin", "slo"),
                    List.of("slo-admin")));

    private WayangSkillCatalog() {
    }

    public static List<RegisteredSkill> defaultSkills() {
        return DEFAULT_SKILLS;
    }

    public static SkillRegistry defaultRegistry() {
        return SkillRegistry.of(DEFAULT_SKILLS);
    }

    public static int defaultAvailableSkillCount() {
        return (int) DEFAULT_SKILLS.stream()
                .filter(RegisteredSkill::availableForRuns)
                .count();
    }

    private static RegisteredSkill skill(
            String id,
            String name,
            String description,
            String category,
            String source,
            List<String> surfaceIds,
            List<String> inputKeys,
            List<String> outputKeys,
            List<String> tags,
            List<String> aliases) {
        return RegisteredSkill.active(new AgentSkillDescriptor(
                id,
                name,
                description,
                category,
                source,
                "1.0.0",
                surfaceIds,
                inputKeys,
                outputKeys,
                tags,
                Map.of()))
                .withAliases(aliases);
    }
}
