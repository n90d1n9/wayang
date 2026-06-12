package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Optional;

/**
 * Store SPI for A2A task lifecycle state.
 */
public interface WayangA2aTaskStore {

    A2aTask create(A2aTask task);

    Optional<A2aTask> get(String taskId);

    List<A2aTask> list(WayangA2aTaskQuery query);

    A2aTask updateStatus(String taskId, A2aTaskStatus status);

    A2aTask appendMessage(String taskId, A2aMessage message);

    A2aTask appendArtifact(String taskId, A2aArtifact artifact);

    A2aTask cancel(String taskId, A2aMessage message);

    List<WayangA2aTaskEvent> events(String taskId, long afterSequence, int limit);

    WayangA2aPushNotificationConfig putPushNotificationConfig(WayangA2aPushNotificationConfig config);

    Optional<WayangA2aPushNotificationConfig> getPushNotificationConfig(String taskId, String configId);

    List<WayangA2aPushNotificationConfig> listPushNotificationConfigs(String taskId);

    boolean deletePushNotificationConfig(String taskId, String configId);
}
