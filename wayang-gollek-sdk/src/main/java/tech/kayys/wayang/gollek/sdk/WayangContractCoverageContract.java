package tech.kayys.wayang.gollek.sdk;

public record WayangContractCoverageContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.contract.coverage";
    public static final int VERSION = 1;
    public static final String CONTRACT_COMMAND_COVERAGE = "contract-command-coverage";

    public WayangContractCoverageContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static WayangContractCoverageContract contractCommandCoverage() {
        return new WayangContractCoverageContract(SCHEMA, VERSION, CONTRACT_COMMAND_COVERAGE);
    }
}
