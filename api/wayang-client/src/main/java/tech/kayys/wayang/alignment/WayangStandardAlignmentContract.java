package tech.kayys.wayang.alignment;

import tech.kayys.wayang.client.SdkText;

public record WayangStandardAlignmentContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.standard.alignment";
    public static final int VERSION = 1;
    public static final String STANDARD_ALIGNMENT_HEALTH = "standard-alignment-health";

    public WayangStandardAlignmentContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangStandardAlignmentContract standardAlignmentHealth() {
        return new WayangStandardAlignmentContract(SCHEMA, VERSION, STANDARD_ALIGNMENT_HEALTH);
    }
}
