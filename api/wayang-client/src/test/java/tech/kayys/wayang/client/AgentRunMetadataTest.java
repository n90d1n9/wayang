package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.run.AgentRunHandle;
import tech.kayys.wayang.agent.run.AgentRunMetadata;
import tech.kayys.wayang.agent.run.AgentRunStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunMetadataTest {

    @Test
    void resolvesLifecycleMetadataAliases() {
        AgentRunStatus status = new AgentRunStatus(
                AgentRunHandle.completed("run-1", "strategy-a"),
                true,
                "done",
                Map.of(
                        "tenantId", "tenant-a",
                        "sessionId", "session-a",
                        "surfaceId", "assistant-agent",
                        "wayang.profile", "low-code-agent"));

        assertThat(AgentRunMetadata.tenant(status)).isEqualTo("tenant-a");
        assertThat(AgentRunMetadata.session(status)).isEqualTo("session-a");
        assertThat(AgentRunMetadata.surface(status)).isEqualTo("assistant-agent");
        assertThat(AgentRunMetadata.profile(status)).isEqualTo("low-code-agent");
        assertThat(AgentRunMetadata.matches(status, "tenant-a", AgentRunMetadata.TENANT, AgentRunMetadata.TENANT_ID))
                .isTrue();
        assertThat(AgentRunMetadata.matches(
                status,
                "low-code-agent",
                AgentRunMetadata.PROFILE,
                AgentRunMetadata.PROFILE_ID,
                AgentRunMetadata.WAYANG_PROFILE))
                .isTrue();
        assertThat(AgentRunMetadata.matches(status, "tenant-b", AgentRunMetadata.TENANT, AgentRunMetadata.TENANT_ID))
                .isFalse();
    }

    @Test
    void writesProfileAliasesForRunMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();

        AgentRunMetadata.putProfileAliases(metadata, " low-code-agent ");

        assertThat(metadata)
                .containsEntry("profile", "low-code-agent")
                .containsEntry("profileId", "low-code-agent");
        assertThat(AgentRunMetadata.profile(Map.of("wayang.profile", "workflow-agent")))
                .isEqualTo("workflow-agent");
        assertThat(AgentRunMetadata.profileContext(Map.of(
                "profile", "generic-context",
                "wayang.profile", "low-code-agent")))
                .isEqualTo("low-code-agent");
    }

    @Test
    void countsLifecycleMetadataClassifierValues() {
        List<AgentRunStatus> statuses = List.of(
                status("run-1", Map.of("profile", "low-code-agent")),
                status("run-2", Map.of("profileId", "low-code-agent")),
                status("run-3", Map.of("wayang.profile", "openclaw-agent")),
                status("run-4", Map.of()));

        assertThat(AgentRunMetadata.count(statuses, AgentRunMetadata::profile))
                .containsEntry("low-code-agent", 2)
                .containsEntry("openclaw-agent", 1)
                .doesNotContainKey("");
    }

    private static AgentRunStatus status(String runId, Map<String, Object> metadata) {
        return new AgentRunStatus(
                AgentRunHandle.completed(runId, "strategy-a"),
                true,
                "done",
                metadata);
    }
}
