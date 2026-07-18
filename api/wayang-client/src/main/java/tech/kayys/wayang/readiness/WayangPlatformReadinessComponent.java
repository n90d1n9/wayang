package tech.kayys.wayang.readiness;

import java.util.Objects;

import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangGollekSdk;

public record WayangPlatformReadinessComponent(
        String readinessId,
        WayangPlatformReadinessAssessor assessor) {

    public WayangPlatformReadinessComponent {
        readinessId = SdkText.trimToDefault(readinessId, WayangReadinessReport.DEFAULT_READINESS_ID);
        assessor = Objects.requireNonNull(assessor, "Platform readiness assessor is required.");
    }

    public WayangReadinessReport assess(WayangGollekSdk sdk) {
        return validateReport(assessUnchecked(sdk));
    }

   public  WayangReadinessReport assessUnchecked(WayangGollekSdk sdk) {
        WayangGollekSdk resolved = sdk == null ? WayangGollekSdk.local() : sdk;
        return assessor.assess(resolved);
    }

   public  WayangReadinessReport validateReport(WayangReadinessReport report) {
        if (report == null) {
            throw new IllegalStateException("Platform readiness component '" + readinessId
                    + "' returned no readiness report.");
        }
        if (!readinessId.equals(report.readinessId())) {
            throw new IllegalStateException("Platform readiness component '" + readinessId
                    + "' returned readiness report '" + report.readinessId() + "'.");
        }
        return report;
    }
}
