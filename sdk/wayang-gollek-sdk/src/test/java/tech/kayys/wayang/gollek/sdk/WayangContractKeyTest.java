package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WayangContractKeyTest {

    @Test
    void normalizesAndRendersCanonicalJsonSchemaId() {
        WayangContractKey key = WayangContractKey.of(" wayang.run.lifecycle ", 0, " run-status ");

        assertThat(key.schema()).isEqualTo(AgentRunLifecycleContract.SCHEMA);
        assertThat(key.version()).isEqualTo(1);
        assertThat(key.envelope()).isEqualTo(AgentRunLifecycleContract.RUN_STATUS);
        assertThat(key.jsonSchemaId())
                .isEqualTo("urn:wayang:contract:wayang.run.lifecycle:v1:run-status");
        assertThat(key.label()).isEqualTo("wayang.run.lifecycle/run-status v1");
    }

    @Test
    void parsesJsonSchemaIdsBackIntoKeys() {
        assertThat(WayangContractKey.parseJsonSchemaId(
                        " urn:wayang:contract:wayang.run.planning:v1:run-preview "))
                .hasValueSatisfying(key -> {
                    assertThat(key.schema()).isEqualTo(AgentRunPlanningContract.SCHEMA);
                    assertThat(key.version()).isEqualTo(AgentRunPlanningContract.VERSION);
                    assertThat(key.envelope()).isEqualTo(AgentRunPlanningContract.RUN_PREVIEW);
                    assertThat(key.jsonSchemaId())
                            .isEqualTo("urn:wayang:contract:wayang.run.planning:v1:run-preview");
                });
    }

    @Test
    void rejectsMalformedJsonSchemaIds() {
        assertThat(WayangContractKey.parseJsonSchemaId(null)).isEmpty();
        assertThat(WayangContractKey.parseJsonSchemaId("")).isEmpty();
        assertThat(WayangContractKey.parseJsonSchemaId("wayang.run.planning:v1:run-preview")).isEmpty();
        assertThat(WayangContractKey.parseJsonSchemaId("urn:wayang:contract::v1:run-preview")).isEmpty();
        assertThat(WayangContractKey.parseJsonSchemaId("urn:wayang:contract:wayang.run.planning:v0:run-preview"))
                .isEmpty();
        assertThat(WayangContractKey.parseJsonSchemaId("urn:wayang:contract:wayang.run.planning:vx:run-preview"))
                .isEmpty();
        assertThat(WayangContractKey.parseJsonSchemaId("urn:wayang:contract:wayang.run.planning:v1:"))
                .isEmpty();
    }

    @Test
    void matchesDescriptorsAndCommandContractsByIdentity() {
        WayangContractDescriptor descriptor = WayangContractDescriptors.planning(
                AgentRunPlanningContract.RUN_PREVIEW,
                "Preview",
                java.util.List.of("run-dry-json"),
                "run <prompt> --dry-run --json");
        WorkbenchCommandContract commandContract =
                WorkbenchCommandContract.planning(AgentRunPlanningContract.RUN_PREVIEW);
        WayangContractKey key = WayangContractKey.from(descriptor);

        assertThat(key.matches(descriptor)).isTrue();
        assertThat(key.matches(commandContract)).isTrue();
        assertThat(WayangContractKey.from(commandContract)).isEqualTo(key);
        assertThat(WayangContractKey.of(
                        AgentRunLifecycleContract.SCHEMA,
                        AgentRunLifecycleContract.VERSION,
                        AgentRunLifecycleContract.RUN_STATUS)
                .matches(descriptor)).isFalse();
    }
}
