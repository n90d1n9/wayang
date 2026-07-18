package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesPortDispatchResult;
import tech.kayys.wayang.agent.hermes.HermesRuntimePortCatalog;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearningAuditResponseTest {

    @Test
    void acceptsDescriptorStyleRetentionStatusFallback() {
        HermesLearningAuditResponse response = HermesLearningAuditResponse.from(HermesPortResponse.from(
                new HermesPortDispatchResult(
                        HermesRuntimePortCatalog.LEARNING_AUDIT,
                        "inspect",
                        "latest",
                        true,
                        true,
                        true,
                        "inspected",
                        "learning audit inspected",
                        Map.of(
                                "retentionStatus", Map.of(
                                        "ledgerType", "file-system",
                                        "status", "near-capacity",
                                        "requiresAttention", true)))));

        assertThat(response.learningAuditRetentionStatus())
                .containsEntry("ledgerType", "file-system")
                .containsEntry("status", "near-capacity")
                .containsEntry("requiresAttention", true);
    }
}
