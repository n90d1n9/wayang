package tech.kayys.wayang.readiness;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.WayangJsonSchemaDocuments;
import tech.kayys.wayang.contract.WayangContractDescriptor;
import tech.kayys.wayang.contract.WayangContractJsonSchema;

public final class WayangReadinessJsonSchema {

    private WayangReadinessJsonSchema() {
    }

    public static boolean matches(WayangContractDescriptor contract) {
        return WayangReadinessContract.SCHEMA.equals(contract.schema());
    }

    public static WayangContractJsonSchema schema(WayangContractDescriptor contract) {
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
