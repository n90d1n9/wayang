package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

public record ProductProfile(
        String id,
        String name,
        String surfaceId,
        String description,
        String starterPrompt,
        String workflowId,
        List<String> skills,
        boolean memoryEnabled,
        boolean workspaceEnabled,
        boolean harnessEnabled,
        boolean harnessIncludeOptional,
        boolean requireReady,
        int maxSteps,
        int workspaceMaxEntries,
        int harnessMaxChecks,
        Map<String, Object> context,
        List<String> notes) {

    public ProductProfile {
        id = SdkText.trimToEmpty(id);
        name = SdkText.trimToEmpty(name);
        surfaceId = WayangProductCatalog.requireKnownSurfaceId(surfaceId);
        description = SdkText.trimToEmpty(description);
        starterPrompt = SdkText.trimToDefault(starterPrompt, "Describe the task here.");
        workflowId = SdkText.trimToEmpty(workflowId);
        skills = SdkLists.copy(skills);
        maxSteps = maxSteps > 0 ? maxSteps : 12;
        workspaceMaxEntries = workspaceMaxEntries > 0 ? Math.min(workspaceMaxEntries, 500) : 80;
        harnessMaxChecks = harnessMaxChecks > 0 ? Math.min(harnessMaxChecks, 50) : 8;
        context = SdkMaps.copy(context);
        notes = SdkLists.copy(notes);
    }

    public AgentRunRequest requestTemplate() {
        AgentRunRequest.Builder builder = AgentRunRequest.builder()
                .prompt(starterPrompt)
                .surfaceId(surfaceId)
                .skills(skills)
                .memoryEnabled(memoryEnabled)
                .maxSteps(maxSteps);
        if (!workflowId.isBlank()) {
            builder.workflowId(workflowId);
        }
        if (workspaceEnabled) {
            builder.workspace(".", workspaceMaxEntries);
        } else {
            builder.workspaceMaxEntries(workspaceMaxEntries);
        }
        if (harnessEnabled) {
            builder.harness(harnessMaxChecks, harnessIncludeOptional);
        } else {
            builder.harnessMaxChecks(harnessMaxChecks)
                    .harnessIncludeOptional(harnessIncludeOptional);
        }
        context.forEach(builder::context);
        return builder.build();
    }
}
