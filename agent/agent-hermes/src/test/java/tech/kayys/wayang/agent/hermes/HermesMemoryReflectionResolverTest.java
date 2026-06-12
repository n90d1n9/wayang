package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesMemoryReflectionResolverTest {

    @Test
    void resolvesExplicitReflectionRequest() {
        HermesMemoryReflectionResolver resolver = new HermesMemoryReflectionResolver(HermesAgentModeConfig.defaults());

        HermesMemoryReflectionPlan plan = resolver.resolve(AgentRequest.builder()
                .sessionId("session-a")
                .parameter("reflectMemory", true)
                .parameter("memory.scope", "user")
                .parameter("reflection.cadence", "daily")
                .parameter("reflection.priority", "high")
                .build());

        assertThat(plan.memoryEnabled()).isTrue();
        assertThat(plan.requested()).isTrue();
        assertThat(plan.reflect()).isTrue();
        assertThat(plan.active()).isTrue();
        assertThat(plan.scope()).isEqualTo("user");
        assertThat(plan.cadence()).isEqualTo("daily");
        assertThat(plan.priority()).isEqualTo("high");
        assertThat(plan.source()).isEqualTo("explicit");
    }

    @Test
    void infersReflectionFromPrompt() {
        HermesMemoryReflectionResolver resolver = new HermesMemoryReflectionResolver(HermesAgentModeConfig.defaults());

        HermesMemoryReflectionPlan plan = resolver.resolve(AgentRequest.builder()
                .userId("user-a")
                .prompt("Important: remember this preference for long-term memory")
                .build());

        assertThat(plan.requested()).isTrue();
        assertThat(plan.reflect()).isTrue();
        assertThat(plan.scope()).isEqualTo("user");
        assertThat(plan.cadence()).isEqualTo("post-run");
        assertThat(plan.priority()).isEqualTo("high");
        assertThat(plan.source()).isEqualTo("prompt");
        assertThat(plan.reason()).isEqualTo("memory reflection inferred from prompt");
    }

    @Test
    void respectsExplicitOptOut() {
        HermesMemoryReflectionResolver resolver = new HermesMemoryReflectionResolver(HermesAgentModeConfig.defaults());

        HermesMemoryReflectionPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Remember this")
                .parameter("memory.reflect", false)
                .build());

        assertThat(plan.memoryEnabled()).isTrue();
        assertThat(plan.requested()).isFalse();
        assertThat(plan.reflect()).isFalse();
        assertThat(plan.active()).isFalse();
        assertThat(plan.source()).isEqualTo("explicit");
        assertThat(plan.reason()).isEqualTo("memory reflection disabled for request");
    }

    @Test
    void keepsReflectionInactiveWhenPersistentMemoryIsDisabled() {
        HermesMemoryReflectionResolver resolver = new HermesMemoryReflectionResolver(HermesAgentModeConfig.builder()
                .persistentMemoryEnabled(false)
                .build());

        HermesMemoryReflectionPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Remember this preference")
                .build());

        assertThat(plan.memoryEnabled()).isFalse();
        assertThat(plan.requested()).isTrue();
        assertThat(plan.reflect()).isFalse();
        assertThat(plan.active()).isFalse();
        assertThat(plan.source()).isEqualTo("disabled");
        assertThat(plan.reason()).isEqualTo("persistent memory disabled");
    }

    @Test
    void reportsNoReflectionForOrdinaryRequests() {
        HermesMemoryReflectionResolver resolver = new HermesMemoryReflectionResolver(HermesAgentModeConfig.defaults());

        HermesMemoryReflectionPlan plan = resolver.resolve(AgentRequest.builder()
                .prompt("Prepare a release report")
                .build());

        assertThat(plan.memoryEnabled()).isTrue();
        assertThat(plan.requested()).isFalse();
        assertThat(plan.reflect()).isFalse();
        assertThat(plan.active()).isFalse();
        assertThat(plan.scope()).isEqualTo("tenant");
        assertThat(plan.cadence()).isEqualTo("none");
        assertThat(plan.priority()).isEqualTo("normal");
        assertThat(plan.source()).isEqualTo("none");
    }

    @Test
    void disablesReflectionWhenCadenceIsNone() {
        HermesMemoryReflectionResolver resolver = new HermesMemoryReflectionResolver(HermesAgentModeConfig.defaults());

        HermesMemoryReflectionPlan plan = resolver.resolve(AgentRequest.builder()
                .parameter("reflection.cadence", "never")
                .build());

        assertThat(plan.requested()).isFalse();
        assertThat(plan.reflect()).isFalse();
        assertThat(plan.cadence()).isEqualTo("none");
        assertThat(plan.source()).isEqualTo("explicit");
        assertThat(plan.reason()).isEqualTo("memory reflection cadence disabled");
    }

    @Test
    void rejectsInvalidReflectionBooleanHints() {
        HermesMemoryReflectionResolver resolver = new HermesMemoryReflectionResolver(HermesAgentModeConfig.defaults());

        assertThatThrownBy(() -> resolver.resolve(AgentRequest.builder()
                .parameter("memory.reflect", "maybe")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memory reflection boolean");
    }
}
