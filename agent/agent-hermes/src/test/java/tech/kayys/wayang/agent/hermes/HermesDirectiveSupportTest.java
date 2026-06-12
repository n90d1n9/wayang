package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;

class HermesDirectiveSupportTest {

    @Test
    void extractsRequestIdentityWithDefaults() {
        HermesDirectiveSupport.Identity identity = HermesDirectiveSupport.identity(AgentRequest.builder()
                .requestId("req-a")
                .tenantId("tenant-a")
                .sessionId("session-a")
                .userId("user-a")
                .build());

        assertThat(identity.requestId()).isEqualTo("req-a");
        assertThat(identity.tenantId()).isEqualTo("tenant-a");
        assertThat(identity.sessionId()).isEqualTo("session-a");
        assertThat(identity.userId()).isEqualTo("user-a");
        assertThat(HermesDirectiveSupport.identity(null).tenantId()).isEqualTo("default");
    }

    @Test
    void normalizesPrefixedIdsForAdapterPayloads() {
        assertThat(HermesDirectiveSupport.prefixedId("hermes-automation", "Req 42", "task"))
                .isEqualTo("hermes-automation-req-42");
        assertThat(HermesDirectiveSupport.prefixedId("hermes-automation", " ", "task"))
                .isEqualTo("hermes-automation-task");
    }

    @Test
    void producesStableNonBlankHashBases() {
        String first = HermesDirectiveSupport.hashBase("tenant-a", "session-a", "work");
        String second = HermesDirectiveSupport.hashBase("tenant-a", "session-a", "work");

        assertThat(first).isNotBlank();
        assertThat(second).isEqualTo(first);
    }
}
