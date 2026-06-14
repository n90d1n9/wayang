package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.AgentRunLifecycleContract;
import tech.kayys.wayang.gollek.sdk.AgentRunPlanningContract;

import java.util.LinkedHashMap;
import java.util.Map;

final class WayangContractFormat {

    private WayangContractFormat() {
    }

    static void putLifecycle(Map<String, Object> values, String envelope) {
        values.put("contract", lifecycle(envelope));
    }

    static void putPlanning(Map<String, Object> values, String envelope) {
        values.put("contract", planning(envelope));
    }

    static Map<String, Object> lifecycle(String envelope) {
        AgentRunLifecycleContract contract = AgentRunLifecycleContract.of(envelope);
        return contract(contract.schema(), contract.version(), contract.envelope());
    }

    static Map<String, Object> planning(String envelope) {
        AgentRunPlanningContract contract = AgentRunPlanningContract.of(envelope);
        return contract(contract.schema(), contract.version(), contract.envelope());
    }

    private static Map<String, Object> contract(String schema, int version, String envelope) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schema", schema);
        values.put("version", version);
        values.put("envelope", envelope);
        return values;
    }
}
