package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimeLifecycleTest {

    @Test
    void derivesReadyLifecycleFromDefaultRuntimeConfig() {
        HermesRuntimeLifecycle lifecycle = HermesRuntimeDiagnostics.from(
                HermesAgentModeConfig.defaults(),
                HermesRuntimePorts.noop())
                .lifecycle();

        assertThat(lifecycle.phase()).isEqualTo("ready");
        assertThat(lifecycle.ready()).isTrue();
        assertThat(lifecycle.backgroundWorkEnabled()).isTrue();
        assertThat(lifecycle.sessionContinuityEnabled()).isTrue();
        assertThat(lifecycle.enabledLoops())
                .containsExactly(
                        "gateway-continuity",
                        "cron-automation",
                        "memory-reflection",
                        "skill-learning",
                        "skill-self-improvement",
                        "sub-agent-supervision");
        assertThat(lifecycle.disabledLoops()).containsExactly("runtime-journal");
        assertThat(lifecycle.toMetadata())
                .containsEntry("phase", "ready")
                .containsEntry("runtimeJournalEnabled", false);
    }

    @Test
    void reportsDegradedLifecycleWhenDurableSkillPersistenceResourcesAreMissing() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeEventJournalEnabled(true)
                .persistenceHints(Map.of(
                        HermesSkillPersistenceHintKeys.DEFINITIONS, "database",
                        HermesSkillPersistenceHintKeys.ARTIFACTS, "s3",
                        HermesSkillPersistenceHintKeys.FALLBACK, "file-system"))
                .build();

        HermesRuntimeLifecycle lifecycle = HermesRuntimeDiagnostics.from(
                config,
                HermesRuntimePorts.noop(),
                Optional.empty(),
                Optional.empty())
                .lifecycle();

        assertThat(lifecycle.phase()).isEqualTo("degraded");
        assertThat(lifecycle.ready()).isFalse();
        assertThat(lifecycle.runtimePortsReady()).isTrue();
        assertThat(lifecycle.skillPersistenceReady()).isFalse();
        assertThat(lifecycle.runtimeJournalEnabled()).isTrue();
        assertThat(lifecycle.enabledLoops()).contains("runtime-journal");
        assertThat(lifecycle.attention())
                .contains("Missing learned-skill persistence resources: DataSource, ObjectStorageService");
    }
}
