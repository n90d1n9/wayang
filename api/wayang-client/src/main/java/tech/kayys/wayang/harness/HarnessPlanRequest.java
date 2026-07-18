package tech.kayys.wayang.harness;

import tech.kayys.wayang.client.SdkText;

public record HarnessPlanRequest(
        String rootPath,
        int maxChecks,
        boolean includeOptional) {

    public HarnessPlanRequest {
        rootPath = SdkText.trimToDefault(rootPath, ".");
        maxChecks = maxChecks > 0 ? Math.min(maxChecks, 50) : 8;
    }

    public static HarnessPlanRequest current() {
        return new HarnessPlanRequest(".", 8, true);
    }
}
