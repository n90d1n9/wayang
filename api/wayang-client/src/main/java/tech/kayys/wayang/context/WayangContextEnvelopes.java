package tech.kayys.wayang.context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.harness.HarnessCheck;
import tech.kayys.wayang.harness.HarnessPlan;
import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.WorkspaceSnapshot;

public final class WayangContextEnvelopes {

    private WayangContextEnvelopes() {
    }

    public static Map<String, Object> workspace(WorkspaceSnapshot snapshot) {
        WorkspaceSnapshot model = normalizeWorkspace(snapshot);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("rootPath", model.rootPath());
        values.put("exists", model.exists());
        values.put("directory", model.directory());
        values.put("gitRepository", model.gitRepository());
        values.put("gitRoot", model.gitRoot());
        values.put("branch", model.branch());
        values.put("buildFiles", model.buildFiles());
        values.put("packageManagers", model.packageManagers());
        values.put("modules", model.modules());
        values.put("importantPaths", model.importantPaths());
        values.put("notes", model.notes());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> harness(HarnessPlan plan) {
        HarnessPlan model = normalizeHarness(plan);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("workspace", workspace(model.workspace()));
        values.put("checks", model.checks().stream()
                .map(WayangContextEnvelopes::check)
                .toList());
        values.put("notes", model.notes());
        return SdkMaps.orderedCopy(values);
    }

    public static Map<String, Object> check(HarnessCheck check) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", check.id());
        values.put("label", check.label());
        values.put("command", check.command());
        values.put("commandLine", check.commandLine());
        values.put("workingDirectory", check.workingDirectory());
        values.put("optional", check.optional());
        values.put("reason", check.reason());
        return SdkMaps.orderedCopy(values);
    }

    public static WorkspaceSnapshot normalizeWorkspace(WorkspaceSnapshot snapshot) {
        return snapshot == null
                ? new WorkspaceSnapshot(".", false, false, false, "", "", List.of(), List.of(), List.of(), List.of(),
                        List.of())
                : snapshot;
    }

    public static HarnessPlan normalizeHarness(HarnessPlan plan) {
        return plan == null ? new HarnessPlan(null, List.of(), List.of()) : plan;
    }
}
