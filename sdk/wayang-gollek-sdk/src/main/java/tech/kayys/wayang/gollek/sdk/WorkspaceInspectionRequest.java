package tech.kayys.wayang.gollek.sdk;

public record WorkspaceInspectionRequest(
        String rootPath,
        int maxEntries,
        boolean includeHidden) {

    public WorkspaceInspectionRequest {
        rootPath = SdkText.trimToDefault(rootPath, ".");
        maxEntries = maxEntries > 0 ? Math.min(maxEntries, 500) : 80;
    }

    public static WorkspaceInspectionRequest current() {
        return new WorkspaceInspectionRequest(".", 80, false);
    }
}
