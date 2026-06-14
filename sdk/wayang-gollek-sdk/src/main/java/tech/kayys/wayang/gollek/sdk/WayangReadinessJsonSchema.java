package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

final class WayangReadinessJsonSchema {

    private WayangReadinessJsonSchema() {
    }

    static boolean matches(WayangContractDescriptor contract) {
        return WayangReadinessContract.SCHEMA.equals(contract.schema());
    }

    static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
        return WayangJsonSchemaDocuments.envelopeSchema(
                contract,
                "Wayang readiness report envelope",
                required(),
                properties(contract.envelope()));
    }

    private static List<String> required() {
        return List.of(
                "readinessId",
                "ready",
                "exitCode",
                "issueCount",
                "probes",
                "issues",
                "attributes");
    }

    private static Map<String, Object> properties(String envelope) {
        return WayangReadinessContract.READINESS_AGGREGATE.equals(envelope)
                ? WayangReadinessJsonSchemaProperties.aggregateProperties()
                : WayangReadinessJsonSchemaProperties.reportProperties();
    }
}
