package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunInspection;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2uiSurfaceContractTest {

    private final A2uiContractAssert contracts = new A2uiContractAssert();

    @Test
    void runStatusReadOnlyMatchesContractFixture() throws IOException {
        AgentRunStatus status = RunSurfaceFixtures.completedStatus("run-1");

        contracts.matchesFixture(
                "contracts/a2ui/wayang-run-status-read-only.jsonl",
                WayangA2uiSurfaces.runStatus(status, WayangA2uiSurfaceOptions.readOnly()));
    }

    @Test
    void runEventsReadOnlyMatchesContractFixture() throws IOException {
        AgentRunEvents events = RunSurfaceFixtures.runEventsPage();

        contracts.matchesFixture(
                "contracts/a2ui/wayang-run-events-read-only.jsonl",
                WayangA2uiSurfaces.runEvents(events, WayangA2uiSurfaceOptions.readOnly()));
    }

    @Test
    void runHistoryReadOnlyMatchesContractFixture() throws IOException {
        AgentRunHistory history = RunSurfaceFixtures.runHistory(4);

        contracts.matchesFixture(
                "contracts/a2ui/wayang-run-history-read-only.jsonl",
                WayangA2uiSurfaces.runHistory(history, WayangA2uiSurfaceOptions.readOnly()));
    }

    @Test
    void runInspectionReadOnlyMatchesContractFixture() throws IOException {
        AgentRunInspection inspection = RunSurfaceFixtures.runInspection();

        contracts.matchesFixture(
                "contracts/a2ui/wayang-run-inspection-read-only.jsonl",
                WayangA2uiSurfaces.runInspection(inspection, WayangA2uiSurfaceOptions.readOnly()));
    }

    @Test
    void rejectedActionFeedbackMatchesContractFixture() throws IOException {
        WayangA2uiActionResult result = WayangA2uiActionResult.rejected(
                WayangA2uiActions.RUN_CANCEL,
                "run-1",
                "A2UI action is not allowed.");

        contracts.matchesFixture(
                "contracts/a2ui/wayang-action-result-rejected.jsonl",
                WayangA2uiResultSurfaces.actionResult(result, 1));
    }

    @Test
    void surfaceCatalogBodyMatchesContractFixture() throws IOException {
        String body = WayangA2uiTransportResponse.from(WayangA2uiSurfaceCatalog.from(null)).body();

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-surface-catalog-body.json",
                body);
        assertThat(body).startsWith("{\"surfaceKinds\":");
        assertThat(body.indexOf("\"descriptors\""))
                .isGreaterThan(body.indexOf("\"descriptorCount\""));
    }
}
