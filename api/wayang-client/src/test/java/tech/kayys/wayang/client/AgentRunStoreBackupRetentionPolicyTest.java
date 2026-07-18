package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.agent.store.AgentRunStoreBackupRetentionPolicy;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRunStoreBackupRetentionPolicyTest {

    @Test
    void parsesNestedBackupRetentionPolicyMaps() {
        AgentRunStoreBackupRetentionPolicy policy = AgentRunStoreBackupRetentionPolicy.fromMap(Map.of(
                "backupRetention", Map.of("maxBackups", "2")));

        assertThat(policy).isEqualTo(AgentRunStoreBackupRetentionPolicy.of(2));
        assertThat(policy.bounded()).isTrue();
        assertThat(policy.isUnlimited()).isFalse();
    }

    @Test
    void parsesUnlimitedBackupRetentionAliases() {
        assertThat(AgentRunStoreBackupRetentionPolicy.fromMap(Map.of(
                "backupRetention", Map.of("mode", "unlimited", "maxBackups", 2))))
                .isEqualTo(AgentRunStoreBackupRetentionPolicy.unlimited());
        assertThat(AgentRunStoreBackupRetentionPolicy.fromMap(Map.of(
                "backupRetention", "off")))
                .isEqualTo(AgentRunStoreBackupRetentionPolicy.unlimited());
        assertThat(AgentRunStoreBackupRetentionPolicy.fromMap(Map.of(
                "backupRetention", Map.of("enabled", false))))
                .isEqualTo(AgentRunStoreBackupRetentionPolicy.unlimited());
    }

    @Test
    void serializesPolicyToStableMapShape() {
        Map<String, Object> values = AgentRunStoreBackupRetentionPolicy.of(2).toMap();

        assertThat(values)
                .containsEntry("mode", "bounded")
                .containsEntry("maxBackups", 2)
                .containsEntry("bounded", true)
                .containsEntry("unlimited", false);
        assertThat(AgentRunStoreBackupRetentionPolicy.toMap(null))
                .containsEntry("mode", "bounded")
                .containsEntry("maxBackups", AgentRunStoreBackupRetentionPolicy.DEFAULT_MAX_BACKUPS);
        assertThat(AgentRunStoreBackupRetentionPolicy.unlimited().toMap())
                .containsEntry("mode", "unlimited")
                .containsEntry("maxBackups", 0)
                .containsEntry("bounded", false)
                .containsEntry("unlimited", true);
    }
}
