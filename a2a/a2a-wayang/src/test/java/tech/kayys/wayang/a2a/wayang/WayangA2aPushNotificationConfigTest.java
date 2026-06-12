package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aPushNotificationConfigTest {

    @Test
    void readsConfigIdAliasesAndDefaults() {
        WayangA2aPushNotificationConfig defaultConfig = WayangA2aPushNotificationConfig.fromMap(
                "task-1",
                Map.of("url", "https://hooks.test/default"));
        WayangA2aPushNotificationConfig idAliasConfig = WayangA2aPushNotificationConfig.fromMap(
                "task-1",
                Map.of("id", "primary", "url", "https://hooks.test/primary"));

        assertThat(defaultConfig.configId()).isEqualTo("default");
        assertThat(idAliasConfig.configId()).isEqualTo("primary");
    }

    @Test
    void readsJsonRpcPushConfigRequestKeys() {
        Map<String, Object> params = Map.of(
                "taskId", "task-1",
                "configId", "primary",
                "url", "https://hooks.test/primary");

        assertThat(WayangA2aPushNotificationConfig.createTaskId(params)).isEqualTo("task-1");
        assertThat(WayangA2aPushNotificationConfig.requiredTaskId(params)).isEqualTo("task-1");
        assertThat(WayangA2aPushNotificationConfig.requiredConfigId(params)).isEqualTo("primary");
    }

    @Test
    void projectsPushConfigWithStableJsonOrder() {
        WayangA2aPushNotificationConfig config = new WayangA2aPushNotificationConfig(
                "task-1",
                "primary",
                "https://hooks.test/primary",
                Map.of("scheme", "Bearer"),
                Map.of("tenant", "tenant-a"));
        Map<String, Object> values = config.toMap();
        String json = WayangA2aHttpJson.write(values);

        assertThat(values.keySet()).containsExactly("taskId", "configId", "url", "auth", "metadata");
        assertThat(json).startsWith("{\"taskId\":");
        assertThat(json.indexOf("\"configId\"")).isGreaterThan(json.indexOf("\"taskId\""));
        assertThat(json.indexOf("\"url\"")).isGreaterThan(json.indexOf("\"configId\""));
        assertThat(json.indexOf("\"auth\"")).isGreaterThan(json.indexOf("\"url\""));
        assertThat(json.indexOf("\"metadata\"")).isGreaterThan(json.indexOf("\"auth\""));
    }

    @Test
    void rejectsMissingRequiredJsonRpcKeys() {
        assertThatThrownBy(() -> WayangA2aPushNotificationConfig.requiredTaskId(Map.of("id", "primary")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId is required");
        assertThatThrownBy(() -> WayangA2aPushNotificationConfig.requiredConfigId(Map.of("taskId", "task-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("configId is required");
    }
}
