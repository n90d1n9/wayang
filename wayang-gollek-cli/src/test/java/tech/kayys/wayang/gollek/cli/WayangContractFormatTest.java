package tech.kayys.wayang.gollek.cli;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangContractFormatTest {

    @Test
    void buildsLifecycleContractEnvelope() {
        assertThat(WayangContractFormat.lifecycle(" run-events "))
                .containsEntry("schema", "wayang.run.lifecycle")
                .containsEntry("version", 1)
                .containsEntry("envelope", "run-events");
    }

    @Test
    void buildsPlanningContractEnvelope() {
        assertThat(WayangContractFormat.planning(" run-preview "))
                .containsEntry("schema", "wayang.run.planning")
                .containsEntry("version", 1)
                .containsEntry("envelope", "run-preview");
    }

    @Test
    void insertsContractFirstWhenCalledBeforePayloadFields() {
        Map<String, Object> values = new LinkedHashMap<>();

        WayangContractFormat.putLifecycle(values, "run-status");
        values.put("runId", "run-1");

        assertThat(values.keySet()).containsExactly("contract", "runId");
    }
}
