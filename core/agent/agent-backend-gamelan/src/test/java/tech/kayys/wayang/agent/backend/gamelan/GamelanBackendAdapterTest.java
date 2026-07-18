package tech.kayys.wayang.agent.backend.gamelan;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.gamelan.engine.run.RunResponse;
import tech.kayys.gamelan.sdk.client.GamelanClient;
import tech.kayys.gamelan.sdk.client.GamelanClientConfig;
import tech.kayys.gamelan.sdk.client.TransportType;
import tech.kayys.gamelan.sdk.client.WorkflowRunOperations;
import tech.kayys.wayang.agent.spi.WorkflowTypes.WorkflowRunId;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GamelanBackendAdapterTest {

    @Test
    void mapsStartedRunResponseAndSdkCapabilities() {
        GamelanClient client = mock(GamelanClient.class);
        WorkflowRunOperations runs = mock(WorkflowRunOperations.class);
        GamelanClientConfig config = GamelanClientConfig.builder()
                .endpoint("http://localhost:8080")
                .tenantId("tenant-a")
                .transport(TransportType.LOCAL)
                .build();
        Instant startedAt = Instant.parse("2026-05-26T01:02:03Z");
        Instant completedAt = Instant.parse("2026-05-26T01:02:45Z");
        RunResponse sdkResponse = RunResponse.builder()
                .runId("run-1")
                .workflowId("workflow.alpha")
                .status("running")
                .startedAt(startedAt)
                .completedAt(completedAt)
                .durationMs(42L)
                .nodesExecuted(2)
                .nodesTotal(5)
                .outputs(Map.of("ok", true))
                .build();

        when(client.config()).thenReturn(config);
        when(client.runs()).thenReturn(runs);
        when(runs.start("run-1")).thenReturn(Uni.createFrom().item(sdkResponse));

        GamelanBackendAdapter adapter = new GamelanBackendAdapter(client);

        var result = adapter.startRun(new WorkflowRunId("run-1"), Map.of()).await().indefinitely();

        assertThat(result.runId()).isEqualTo(new WorkflowRunId("run-1"));
        assertThat(result.workflowId()).isEqualTo("workflow.alpha");
        assertThat(result.outputs()).containsEntry("ok", true);
        assertThat(result.durationMs()).isEqualTo(42);
        assertThat(result.updatedAt()).isEqualTo(completedAt);
        assertThat(result.status().state()).isEqualTo("running");
        assertThat(result.status().currentStep()).isEqualTo(2);
        assertThat(result.status().totalSteps()).isEqualTo(5);
        assertThat(result.status().startedAt()).isEqualTo(startedAt);
        assertThat(adapter.capabilities().supportedTransports()).containsExactly("LOCAL");

        adapter.shutdown();

        verify(client).close();
    }
}
