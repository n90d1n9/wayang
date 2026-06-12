package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportOutcome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpReportMetricsTest {

    @Test
    void appendsOrderedOutcomeAndTransportDigestFragments() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("prefix", "report");

        HttpReportMetrics.putOutcomeCounts(values, 2, 1, 0);
        HttpReportMetrics.putTransportCounts(values, 3, 4);
        HttpReportMetrics.putTransportDigest(
                values,
                true,
                List.of(200, 404),
                List.of(WayangA2uiTransportOutcome.SUCCESS.name()));

        assertThat(values.keySet()).containsExactly(
                "prefix",
                "successfulCount",
                "clientErrorCount",
                "serverErrorCount",
                "handledCount",
                "rejectedCount",
                "transportErrors",
                "statusCodes",
                "outcomes");
        assertThat(values)
                .containsEntry("successfulCount", 2L)
                .containsEntry("clientErrorCount", 1L)
                .containsEntry("serverErrorCount", 0L)
                .containsEntry("handledCount", 3L)
                .containsEntry("rejectedCount", 4L)
                .containsEntry("transportErrors", true)
                .containsEntry("statusCodes", List.of(200, 404))
                .containsEntry("outcomes", List.of(WayangA2uiTransportOutcome.SUCCESS.name()));
    }

    @Test
    void usesEmptyCollectionsForMissingTransportDigestLists() {
        Map<String, Object> values = new LinkedHashMap<>();

        HttpReportMetrics.putTransportDigest(values, false, null, null);

        assertThat(values)
                .containsEntry("transportErrors", false)
                .containsEntry("statusCodes", List.of())
                .containsEntry("outcomes", List.of());
    }

    @Test
    void copiesTransportDigestListsAsImmutableSnapshots() {
        Map<String, Object> values = new LinkedHashMap<>();
        List<Integer> statusCodes = new ArrayList<>();
        statusCodes.add(200);
        List<String> outcomes = new ArrayList<>();
        outcomes.add(WayangA2uiTransportOutcome.SUCCESS.name());

        HttpReportMetrics.putTransportDigest(values, true, statusCodes, outcomes);

        statusCodes.add(404);
        outcomes.add(WayangA2uiTransportOutcome.TRANSPORT_ERROR.name());

        @SuppressWarnings("unchecked")
        List<Integer> copiedStatusCodes = (List<Integer>) values.get("statusCodes");
        @SuppressWarnings("unchecked")
        List<String> copiedOutcomes = (List<String>) values.get("outcomes");
        assertThat(copiedStatusCodes).containsExactly(200);
        assertThat(copiedOutcomes).containsExactly(WayangA2uiTransportOutcome.SUCCESS.name());
        assertThatThrownBy(() -> copiedStatusCodes.add(500))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> copiedOutcomes.add(WayangA2uiTransportOutcome.ACTION_REJECTED.name()))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
