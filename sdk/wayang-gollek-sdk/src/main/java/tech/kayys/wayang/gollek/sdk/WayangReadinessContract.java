package tech.kayys.wayang.gollek.sdk;

public record WayangReadinessContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.readiness";
    public static final int VERSION = 1;
    public static final String READINESS_REPORT = "readiness-report";
    public static final String READINESS_AGGREGATE = "readiness-aggregate";

    public WayangReadinessContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangReadinessContract report() {
        return new WayangReadinessContract(SCHEMA, VERSION, READINESS_REPORT);
    }

    public static WayangReadinessContract aggregate() {
        return new WayangReadinessContract(SCHEMA, VERSION, READINESS_AGGREGATE);
    }
}
