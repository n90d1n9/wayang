package tech.kayys.wayang.gollek.sdk;

import java.util.List;

public record HarnessPlan(
        WorkspaceSnapshot workspace,
        List<HarnessCheck> checks,
        List<String> notes) {

    public HarnessPlan {
        workspace = workspace == null
                ? new WorkspaceSnapshot(".", false, false, false, "", "", null, null, null, null, null)
                : workspace;
        checks = SdkLists.copy(checks);
        notes = SdkLists.copy(notes);
    }
}
