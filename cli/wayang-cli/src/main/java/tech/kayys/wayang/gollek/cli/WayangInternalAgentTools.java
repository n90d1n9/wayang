package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.tools.spi.ToolContext;
import tech.kayys.wayang.tools.spi.ToolResult;
import tech.kayys.wayang.sdk.gollek.ProjectStore;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Agent-callable tools that expose internal session/project/status/info
 * commands so the LLM can inspect and switch context programmatically.
 *
 * Each tool delegates to a lightweight {@link AgentContext} that is populated
 * by {@code WayangCodeCommand} before handing the agent to {@code ReplUi}.
 */
public final class WayangInternalAgentTools {

    private WayangInternalAgentTools() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Shared mutable context injected by WayangCodeCommand
    // ─────────────────────────────────────────────────────────────────────────

    public static final class AgentContext {
        public volatile ProjectStore projectStore;
        public volatile String resolvedProjectKey;
        public volatile String currentSessionId;
        public volatile String model;
        public volatile String provider;
        public volatile String workspace;
        public volatile boolean memoryEnabled;
        public volatile boolean harnessEnabled;
        public Supplier<String> statusSupplier;  // captures /status output
        public Supplier<String> infoSupplier;    // captures /info output
        /** Live agent reference — used to swap history on project/session switch. */
        public volatile tech.kayys.wayang.sdk.agent.WayangAgent agent;
    }

    public static List<Tool> getTools(AgentContext ctx) {
        return List.of(
                new SessionListTool(ctx),
                new SessionCurrentTool(ctx),
                new SessionSwitchTool(ctx),
                new SessionForkTool(ctx),
                new SessionDeleteTool(ctx),
                new ProjectListTool(ctx),
                new ProjectCurrentTool(ctx),
                new ProjectSwitchTool(ctx),
                new StatusTool(ctx),
                new InfoTool(ctx)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Session tools
    // ─────────────────────────────────────────────────────────────────────────

    public static class SessionListTool implements Tool {
        private final AgentContext ctx;
        SessionListTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "session_list"; }
        @Override public String name() { return "List Sessions"; }
        @Override public String description() { return "List all saved sessions for the current project."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of("type", "object", "properties", Map.of(), "required", List.of());
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            if (ctx.projectStore == null) return ToolResult.error("Session persistence unavailable.");
            try {
                List<String> sessions = ctx.projectStore.listSessions(ctx.resolvedProjectKey);
                if (sessions.isEmpty()) return ToolResult.success("No sessions found for project: " + ctx.resolvedProjectKey);
                StringBuilder sb = new StringBuilder("Sessions for project [" + ctx.resolvedProjectKey + "]:\n");
                for (String s : sessions) {
                    sb.append("  - ").append(s);
                    if (s.equals(ctx.currentSessionId)) sb.append(" (current)");
                    sb.append("\n");
                }
                return ToolResult.success(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Failed to list sessions: " + e.getMessage());
            }
        }
    }

    public static class SessionCurrentTool implements Tool {
        private final AgentContext ctx;
        SessionCurrentTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "session_current"; }
        @Override public String name() { return "Current Session"; }
        @Override public String description() { return "Return the current active session ID and project."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of("type", "object", "properties", Map.of(), "required", List.of());
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            return ToolResult.success(
                "Current project: " + (ctx.resolvedProjectKey != null ? ctx.resolvedProjectKey : "none") + "\n" +
                "Current session: " + (ctx.currentSessionId != null ? ctx.currentSessionId : "none")
            );
        }
    }

    public static class SessionSwitchTool implements Tool {
        private final AgentContext ctx;
        SessionSwitchTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "session_switch"; }
        @Override public String name() { return "Switch Session"; }
        @Override public String description() { return "Resume (switch to) a saved session by its session ID."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of(
                "type", "object",
                "properties", Map.of("session_id", Map.of("type", "string", "description", "The session ID to resume")),
                "required", List.of("session_id")
            );
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            if (ctx.projectStore == null) return ToolResult.error("Session persistence unavailable.");
            String sid = (String) params.get("session_id");
            if (sid == null || sid.isBlank()) return ToolResult.error("session_id is required");
            try {
                // 1. Save the current session before switching away
                if (ctx.agent != null && ctx.currentSessionId != null && ctx.resolvedProjectKey != null) {
                    try {
                        ctx.projectStore.saveTranscript(ctx.resolvedProjectKey, ctx.currentSessionId,
                                (java.util.List) ctx.agent.history());
                    } catch (Exception ignored) {}
                }
                // 2. Load the target session transcript
                var transcript = ctx.projectStore.loadTranscript(ctx.resolvedProjectKey, sid);
                if (transcript == null || transcript.isEmpty())
                    return ToolResult.error("No transcript found for session: " + sid);
                // 3. Swap agent history
                if (ctx.agent != null) {
                    List<tech.kayys.wayang.sdk.provider.ChatMessage> messages = deserializeTranscript(transcript);
                    ctx.agent.replaceHistory(messages);
                }
                ctx.currentSessionId = sid;
                return ToolResult.success("Switched to session '" + sid + "' (" + transcript.size() + " messages loaded).");
            } catch (Exception e) {
                return ToolResult.error("Failed to switch session: " + e.getMessage());
            }
        }
    }

    public static class SessionForkTool implements Tool {
        private final AgentContext ctx;
        SessionForkTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "session_fork"; }
        @Override public String name() { return "Fork Session"; }
        @Override public String description() { return "Fork (clone) a session into a new branch. Optionally provide a new name and a checkpoint index."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of(
                "type", "object",
                "properties", Map.of(
                    "session_id", Map.of("type", "string", "description", "Session ID to fork"),
                    "new_name", Map.of("type", "string", "description", "Optional name for the new session"),
                    "checkpoint", Map.of("type", "integer", "description", "Optional message index to fork from")
                ),
                "required", List.of("session_id")
            );
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            if (ctx.projectStore == null) return ToolResult.error("Session persistence unavailable.");
            String sid = (String) params.get("session_id");
            if (sid == null || sid.isBlank()) return ToolResult.error("session_id is required");
            String newName = params.containsKey("new_name") ? (String) params.get("new_name") : null;
            Integer checkpoint = params.containsKey("checkpoint") ? ((Number) params.get("checkpoint")).intValue() : null;
            try {
                var forked = ctx.projectStore.cloneSession(ctx.resolvedProjectKey, sid, newName, checkpoint);
                if (forked == null) return ToolResult.error("Failed to fork session: " + sid);
                return ToolResult.success("Forked session '" + sid + "' → new session '" + forked.id() + "' (name='" + forked.name() + "').");
            } catch (Exception e) {
                return ToolResult.error("Failed to fork session: " + e.getMessage());
            }
        }
    }

    public static class SessionDeleteTool implements Tool {
        private final AgentContext ctx;
        SessionDeleteTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "session_delete"; }
        @Override public String name() { return "Delete Session"; }
        @Override public String description() { return "Permanently delete a saved session by its ID."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of(
                "type", "object",
                "properties", Map.of("session_id", Map.of("type", "string", "description", "Session ID to delete")),
                "required", List.of("session_id")
            );
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            if (ctx.projectStore == null) return ToolResult.error("Session persistence unavailable.");
            String sid = (String) params.get("session_id");
            if (sid == null || sid.isBlank()) return ToolResult.error("session_id is required");
            try {
                boolean deleted = ctx.projectStore.deleteSession(ctx.resolvedProjectKey, sid);
                return deleted
                    ? ToolResult.success("Deleted session: " + sid)
                    : ToolResult.error("No session found with ID: " + sid);
            } catch (Exception e) {
                return ToolResult.error("Failed to delete session: " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Project tools
    // ─────────────────────────────────────────────────────────────────────────

    public static class ProjectListTool implements Tool {
        private final AgentContext ctx;
        ProjectListTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "project_list"; }
        @Override public String name() { return "List Projects"; }
        @Override public String description() { return "List all known Wayang projects with their IDs, names, and directories."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of("type", "object", "properties", Map.of(), "required", List.of());
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            try {
                ProjectStore store = new ProjectStore(null);
                var projects = store.listProjects();
                String current = null;
                try { current = store.currentProject(); } catch (Exception ignored) {}
                if (projects.isEmpty()) return ToolResult.success("No projects found.");
                StringBuilder sb = new StringBuilder("Projects:\n");
                for (var p : projects) {
                    sb.append("  - ").append(p.id()).append(" | ").append(p.name())
                      .append(" | ").append(p.directory());
                    if (p.id().equals(current) || p.id().equals(ctx.resolvedProjectKey)) sb.append(" (current)");
                    sb.append("\n");
                }
                return ToolResult.success(sb.toString());
            } catch (Exception e) {
                return ToolResult.error("Failed to list projects: " + e.getMessage());
            }
        }
    }

    public static class ProjectCurrentTool implements Tool {
        private final AgentContext ctx;
        ProjectCurrentTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "project_current"; }
        @Override public String name() { return "Current Project"; }
        @Override public String description() { return "Return the current active project ID and workspace path."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of("type", "object", "properties", Map.of(), "required", List.of());
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            return ToolResult.success(
                "Current project: " + (ctx.resolvedProjectKey != null ? ctx.resolvedProjectKey : "none") + "\n" +
                "Workspace: " + (ctx.workspace != null ? ctx.workspace : "unknown")
            );
        }
    }

    public static class ProjectSwitchTool implements Tool {
        private final AgentContext ctx;
        ProjectSwitchTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "project_switch"; }
        @Override public String name() { return "Switch Project"; }
        @Override public String description() { return "Switch the active project. If switching to a new project, the user should also call session_list to pick a session."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of(
                "type", "object",
                "properties", Map.of("project_id", Map.of("type", "string", "description", "Project ID to switch to")),
                "required", List.of("project_id")
            );
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            String projectId = (String) params.get("project_id");
            if (projectId == null || projectId.isBlank()) return ToolResult.error("project_id is required");
            try {
                // 1. Save current session before switching away
                if (ctx.agent != null && ctx.currentSessionId != null && ctx.resolvedProjectKey != null) {
                    try {
                        ctx.projectStore.saveTranscript(ctx.resolvedProjectKey, ctx.currentSessionId,
                                (java.util.List) ctx.agent.history());
                    } catch (Exception ignored) {}
                }
                // 2. Persist the project pointer
                ProjectStore store = ctx.projectStore != null ? ctx.projectStore : new ProjectStore(null);
                try {
                    store.switchProject(projectId);
                } catch (Exception ex) {
                    // fallback: write current_project.txt
                    java.nio.file.Path cfg = Path.of(System.getProperty("user.home"), ".wayang", "current_project.txt");
                    java.nio.file.Files.createDirectories(cfg.getParent());
                    java.nio.file.Files.writeString(cfg, projectId);
                }
                ctx.resolvedProjectKey = projectId;
                // 3. Load the most-recent session of the new project (if any)
                String resumedSession = null;
                int msgCount = 0;
                if (ctx.projectStore != null) {
                    try {
                        List<String> sessions = ctx.projectStore.listSessions(projectId);
                        if (!sessions.isEmpty()) {
                            // pick the last session in the list as most-recent
                            String targetSid = sessions.get(sessions.size() - 1);
                            java.util.List<?> transcript = ctx.projectStore.loadTranscript(projectId, targetSid);
                            if (ctx.agent != null) {
                                List<tech.kayys.wayang.sdk.provider.ChatMessage> messages = deserializeTranscript(transcript);
                                ctx.agent.replaceHistory(messages);
                            }
                            ctx.currentSessionId = targetSid;
                            resumedSession = targetSid;
                            msgCount = transcript != null ? transcript.size() : 0;
                        } else {
                            // No sessions yet — start fresh
                            if (ctx.agent != null) ctx.agent.clearHistory();
                            ctx.currentSessionId = java.util.UUID.randomUUID().toString();
                        }
                    } catch (Exception ignored) {
                        if (ctx.agent != null) ctx.agent.clearHistory();
                    }
                }
                String msg = "Switched to project: " + projectId;
                if (resumedSession != null) msg += ". Resumed session '" + resumedSession + "' (" + msgCount + " messages).";
                else msg += ". Started a new session.";
                return ToolResult.success(msg);
            } catch (Exception e) {
                return ToolResult.error("Failed to switch project: " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Status / Info tools
    // ─────────────────────────────────────────────────────────────────────────

    public static class StatusTool implements Tool {
        private final AgentContext ctx;
        StatusTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "get_status"; }
        @Override public String name() { return "Get Status"; }
        @Override public String description() { return "Return the current Wayang platform status: active model, provider, project, session, memory, workspace."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of("type", "object", "properties", Map.of(), "required", List.of());
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            if (ctx.statusSupplier != null) {
                try { return ToolResult.success(ctx.statusSupplier.get()); } catch (Exception ignored) {}
            }
            return ToolResult.success(
                "=== Wayang Status ===\n" +
                "Model    : " + nvl(ctx.model) + "\n" +
                "Provider : " + nvl(ctx.provider) + "\n" +
                "Project  : " + nvl(ctx.resolvedProjectKey) + "\n" +
                "Session  : " + nvl(ctx.currentSessionId) + "\n" +
                "Workspace: " + nvl(ctx.workspace) + "\n" +
                "Memory   : " + (ctx.memoryEnabled ? "enabled" : "disabled") + "\n" +
                "Harness  : " + (ctx.harnessEnabled ? "enabled" : "disabled")
            );
        }
        private static String nvl(String v) { return v != null ? v : "N/A"; }
    }

    public static class InfoTool implements Tool {
        private final AgentContext ctx;
        InfoTool(AgentContext ctx) { this.ctx = ctx; }

        @Override public String id() { return "get_info"; }
        @Override public String name() { return "Get System Info"; }
        @Override public String description() { return "Return system/runtime information: Java version, OS, available memory, and Wayang configuration."; }
        @Override public Map<String, Object> inputSchema() {
            return Map.of("type", "object", "properties", Map.of(), "required", List.of());
        }
        @Override public ToolResult execute(Map<String, Object> params, ToolContext context) {
            if (ctx.infoSupplier != null) {
                try { return ToolResult.success(ctx.infoSupplier.get()); } catch (Exception ignored) {}
            }
            Runtime rt = Runtime.getRuntime();
            return ToolResult.success(
                "=== System Info ===\n" +
                "Java     : " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")\n" +
                "OS       : " + System.getProperty("os.name") + " " + System.getProperty("os.version") + "\n" +
                "Heap     : " + (rt.totalMemory() / 1024 / 1024) + " MB total / " + (rt.maxMemory() / 1024 / 1024) + " MB max\n" +
                "CPUs     : " + rt.availableProcessors() + "\n" +
                "Home     : " + System.getProperty("user.home") + "\n" +
                "Workspace: " + nvl(ctx.workspace)
            );
        }
        private static String nvl(String v) { return v != null ? v : "N/A"; }
    }

    /** Helper to parse a generic JSON map list from ProjectStore into ChatMessage objects. */
    private static List<tech.kayys.wayang.sdk.provider.ChatMessage> deserializeTranscript(List<?> raw) {
        List<tech.kayys.wayang.sdk.provider.ChatMessage> result = new java.util.ArrayList<>();
        if (raw == null) return result;
        for (Object item : raw) {
            if (item instanceof Map<?,?> m) {
                String roleStr = m.get("role") != null ? m.get("role").toString().toUpperCase() : "ASSISTANT";
                tech.kayys.wayang.sdk.provider.ChatMessage.Role role = 
                    tech.kayys.wayang.sdk.provider.ChatMessage.Role.USER.name().equals(roleStr) ? 
                    tech.kayys.wayang.sdk.provider.ChatMessage.Role.USER : 
                    tech.kayys.wayang.sdk.provider.ChatMessage.Role.ASSISTANT;
                
                String text = m.get("text") != null ? m.get("text").toString() : "";
                // Currently only deserializing basic text back; restoring tool-calls 
                // in the history is complex and less critical for simple resumption.
                result.add(new tech.kayys.wayang.sdk.provider.ChatMessage(
                    role, List.of(new tech.kayys.wayang.sdk.provider.ContentBlock.Text(text))
                ));
            }
        }
        return result;
    }
}
