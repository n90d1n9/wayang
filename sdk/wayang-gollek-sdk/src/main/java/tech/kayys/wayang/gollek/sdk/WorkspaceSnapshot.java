package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record WorkspaceSnapshot(
        String rootPath,
        boolean exists,
        boolean directory,
        boolean gitRepository,
        String gitRoot,
        String branch,
        List<String> buildFiles,
        List<String> packageManagers,
        List<String> modules,
        List<String> importantPaths,
        List<String> notes) {

    public WorkspaceSnapshot {
        rootPath = SdkText.trimToDefault(rootPath, ".");
        gitRoot = SdkText.trimToEmpty(gitRoot);
        branch = SdkText.trimToEmpty(branch);
        buildFiles = SdkLists.copy(buildFiles);
        packageManagers = SdkLists.copy(packageManagers);
        modules = SdkLists.copy(modules);
        importantPaths = SdkLists.copy(importantPaths);
        notes = SdkLists.copy(notes);
    }
}
