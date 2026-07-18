package tech.kayys.wayang.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class WorkspaceContext {

    private WorkspaceContext() {
    }

    static Map<String, Object> from(WorkspaceSnapshot snapshot) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("rootPath", snapshot.rootPath());
        context.put("exists", snapshot.exists());
        context.put("directory", snapshot.directory());
        context.put("gitRepository", snapshot.gitRepository());
        if (!snapshot.gitRoot().isBlank()) {
            context.put("gitRoot", snapshot.gitRoot());
        }
        if (!snapshot.branch().isBlank()) {
            context.put("branch", snapshot.branch());
        }
        context.put("buildFiles", snapshot.buildFiles());
        context.put("packageManagers", snapshot.packageManagers());
        context.put("modules", snapshot.modules());
        context.put("importantPaths", snapshot.importantPaths());
        context.put("notes", snapshot.notes());
        return Collections.unmodifiableMap(context);
    }
}
