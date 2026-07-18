package tech.kayys.wayang.agent.run;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangProductCatalog;

public record AgentRunRequest(
        String prompt,
        String tenantId,
        String modelId,
        String workflowId,
        List<String> skills,
        boolean memoryEnabled,
        int maxSteps,
        String workspacePath,
        boolean workspaceEnabled,
        int workspaceMaxEntries,
        boolean harnessEnabled,
        int harnessMaxChecks,
        boolean harnessIncludeOptional,
        String surfaceId,
        String sessionId,
        String userId,
        Map<String, Object> context,
        String systemPrompt) {

    public AgentRunRequest(
            String prompt,
            String tenantId,
            String modelId,
            String workflowId,
            List<String> skills,
            boolean memoryEnabled,
            int maxSteps) {
        this(prompt, tenantId, modelId, workflowId, skills, memoryEnabled, maxSteps, ".", false, 80);
    }

    public AgentRunRequest(
            String prompt,
            String tenantId,
            String modelId,
            String workflowId,
            List<String> skills,
            boolean memoryEnabled,
            int maxSteps,
            String workspacePath,
            boolean workspaceEnabled,
            int workspaceMaxEntries) {
        this(prompt, tenantId, modelId, workflowId, skills, memoryEnabled, maxSteps,
                workspacePath, workspaceEnabled, workspaceMaxEntries, false, 8, true);
    }

    public AgentRunRequest(
            String prompt,
            String tenantId,
            String modelId,
            String workflowId,
            List<String> skills,
            boolean memoryEnabled,
            int maxSteps,
            String workspacePath,
            boolean workspaceEnabled,
            int workspaceMaxEntries,
            boolean harnessEnabled,
            int harnessMaxChecks,
            boolean harnessIncludeOptional) {
        this(prompt, tenantId, modelId, workflowId, skills, memoryEnabled, maxSteps,
                workspacePath, workspaceEnabled, workspaceMaxEntries,
                harnessEnabled, harnessMaxChecks, harnessIncludeOptional, WayangProductCatalog.DEFAULT_SURFACE_ID);
    }

    public AgentRunRequest(
            String prompt,
            String tenantId,
            String modelId,
            String workflowId,
            List<String> skills,
            boolean memoryEnabled,
            int maxSteps,
            String workspacePath,
            boolean workspaceEnabled,
            int workspaceMaxEntries,
            boolean harnessEnabled,
            int harnessMaxChecks,
            boolean harnessIncludeOptional,
            String surfaceId) {
        this(prompt, tenantId, modelId, workflowId, skills, memoryEnabled, maxSteps,
                workspacePath, workspaceEnabled, workspaceMaxEntries,
                harnessEnabled, harnessMaxChecks, harnessIncludeOptional, surfaceId, "", "", Map.of());
    }

    public AgentRunRequest(
            String prompt,
            String tenantId,
            String modelId,
            String workflowId,
            List<String> skills,
            boolean memoryEnabled,
            int maxSteps,
            String workspacePath,
            boolean workspaceEnabled,
            int workspaceMaxEntries,
            boolean harnessEnabled,
            int harnessMaxChecks,
            boolean harnessIncludeOptional,
            String surfaceId,
            String sessionId,
            String userId,
            Map<String, Object> context) {
        this(prompt, tenantId, modelId, workflowId, skills, memoryEnabled, maxSteps,
                workspacePath, workspaceEnabled, workspaceMaxEntries,
                harnessEnabled, harnessMaxChecks, harnessIncludeOptional,
                surfaceId, sessionId, userId, context, "");
    }

    public AgentRunRequest {
        prompt = SdkText.trimToEmpty(prompt);
        systemPrompt = SdkText.trimToEmpty(systemPrompt);
        tenantId = SdkText.trimToDefault(tenantId, "default");
        modelId = SdkText.trimToEmpty(modelId);
        workflowId = SdkText.trimToEmpty(workflowId);
        surfaceId = WayangProductCatalog.normalizeSurfaceId(surfaceId);
        sessionId = SdkText.trimToEmpty(sessionId);
        userId = SdkText.trimToEmpty(userId);
        skills = SdkLists.copy(skills);
        context = SdkMaps.copy(context);
        maxSteps = maxSteps > 0 ? maxSteps : 12;
        workspacePath = SdkText.trimToDefault(workspacePath, ".");
        workspaceMaxEntries = workspaceMaxEntries > 0 ? Math.min(workspaceMaxEntries, 500) : 80;
        harnessMaxChecks = harnessMaxChecks > 0 ? Math.min(harnessMaxChecks, 50) : 8;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(AgentRunRequest request) {
        return new Builder(request);
    }

    public static final class Builder {
        private String prompt = "";
        private String tenantId = "default";
        private String modelId = "";
        private String workflowId = "";
        private String surfaceId = WayangProductCatalog.DEFAULT_SURFACE_ID;
        private List<String> skills = new ArrayList<>();
        private boolean memoryEnabled = true;
        private int maxSteps = 12;
        private String workspacePath = ".";
        private boolean workspaceEnabled = false;
        private int workspaceMaxEntries = 80;
        private boolean harnessEnabled = false;
        private int harnessMaxChecks = 8;
        private boolean harnessIncludeOptional = true;
        private String sessionId = "";
        private String userId = "";
        private Map<String, Object> context = Map.of();
        private String systemPrompt = "";

        private Builder() {
        }

        private Builder(AgentRunRequest request) {
            AgentRunRequest source = request == null ? new AgentRunRequest("", "default", "", "", null, true, 12) : request;
            this.prompt = source.prompt();
            this.tenantId = source.tenantId();
            this.modelId = source.modelId();
            this.workflowId = source.workflowId();
            this.surfaceId = source.surfaceId();
            this.skills = new ArrayList<>(source.skills());
            this.memoryEnabled = source.memoryEnabled();
            this.maxSteps = source.maxSteps();
            this.workspacePath = source.workspacePath();
            this.workspaceEnabled = source.workspaceEnabled();
            this.workspaceMaxEntries = source.workspaceMaxEntries();
            this.harnessEnabled = source.harnessEnabled();
            this.harnessMaxChecks = source.harnessMaxChecks();
            this.harnessIncludeOptional = source.harnessIncludeOptional();
            this.sessionId = source.sessionId();
            this.userId = source.userId();
            this.context = new LinkedHashMap<>(source.context());
            this.systemPrompt = source.systemPrompt();
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder workflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }

        public Builder surfaceId(String surfaceId) {
            this.surfaceId = surfaceId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder context(Map<String, Object> context) {
            this.context = new LinkedHashMap<>(SdkMaps.copy(context));
            return this;
        }

        public Builder context(String key, Object value) {
            Map<String, Object> next = new LinkedHashMap<>(this.context);
            next.put(key, value);
            this.context = next;
            return this;
        }

        public Builder skills(List<String> skills) {
            this.skills = new ArrayList<>(SdkLists.copy(skills));
            return this;
        }

        public Builder skill(String skill) {
            String normalized = SdkText.trimToEmpty(skill);
            if (!normalized.isEmpty()) {
                this.skills.add(normalized);
            }
            return this;
        }

        public Builder memoryEnabled(boolean memoryEnabled) {
            this.memoryEnabled = memoryEnabled;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder workspace(String workspacePath) {
            this.workspacePath = workspacePath;
            this.workspaceEnabled = true;
            return this;
        }

        public Builder workspace(String workspacePath, int workspaceMaxEntries) {
            this.workspacePath = workspacePath;
            this.workspaceMaxEntries = workspaceMaxEntries;
            this.workspaceEnabled = true;
            return this;
        }

        public Builder workspaceEnabled(boolean workspaceEnabled) {
            this.workspaceEnabled = workspaceEnabled;
            return this;
        }

        public Builder workspaceMaxEntries(int workspaceMaxEntries) {
            this.workspaceMaxEntries = workspaceMaxEntries;
            return this;
        }

        public Builder harness(boolean harnessEnabled) {
            this.harnessEnabled = harnessEnabled;
            return this;
        }

        public Builder harness(int harnessMaxChecks, boolean harnessIncludeOptional) {
            this.harnessEnabled = true;
            this.harnessMaxChecks = harnessMaxChecks;
            this.harnessIncludeOptional = harnessIncludeOptional;
            return this;
        }

        public Builder harnessMaxChecks(int harnessMaxChecks) {
            this.harnessMaxChecks = harnessMaxChecks;
            return this;
        }

        public Builder harnessIncludeOptional(boolean harnessIncludeOptional) {
            this.harnessIncludeOptional = harnessIncludeOptional;
            return this;
        }

        public AgentRunRequest build() {
            return new AgentRunRequest(
                    prompt,
                    tenantId,
                    modelId,
                    workflowId,
                    skills,
                    memoryEnabled,
                    maxSteps,
                    workspacePath,
                    workspaceEnabled,
                    workspaceMaxEntries,
                    harnessEnabled,
                    harnessMaxChecks,
                    harnessIncludeOptional,
                    surfaceId,
                    sessionId,
                    userId,
                    context,
                    systemPrompt);
        }
    }
}
