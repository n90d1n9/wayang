package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessSpecAlignmentChecksTest {

    @Test
    void buildsSummaryAndCategoryRowsFromSnapshot() {
        WayangA2aSpecAlignmentSnapshot specAlignment = new WayangA2aSpecAlignmentSnapshot(
                "a2a",
                A2aProtocol.VERSION,
                A2aProtocol.BINDING_JSONRPC,
                false,
                20,
                18,
                2,
                List.of("route.SendMessage", "jsonrpc.RequestShape"),
                List.of(
                        new WayangA2aSpecAlignmentCategorySummary(
                                "route",
                                12,
                                11,
                                1,
                                List.of("route.SendMessage")),
                        new WayangA2aSpecAlignmentCategorySummary(
                                "jsonrpc",
                                8,
                                7,
                                1,
                                List.of("jsonrpc.RequestShape"))));

        List<Map<String, Object>> rows =
                WayangA2aJsonRpcReadinessSpecAlignmentChecks.from(specAlignment).toMaps();

        assertThat(rows)
                .extracting(row -> row.get("probe"))
                .containsExactly("specAlignment", "specAlignment:route", "specAlignment:jsonrpc");
        assertThat(rows)
                .first()
                .satisfies(row -> assertThat(row)
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 2));
        assertThat(rows)
                .anySatisfy(row -> assertThat(row)
                        .containsEntry("probe", "specAlignment:route")
                        .containsEntry("category", "route")
                        .containsEntry("gapIds", List.of("route.SendMessage")));
    }

    @Test
    void defaultsToCurrentSpecAlignmentSnapshot() {
        assertThat(WayangA2aJsonRpcReadinessSpecAlignmentChecks.from(null).toMaps())
                .extracting(row -> row.get("probe"))
                .containsExactly(
                        "specAlignment",
                        "specAlignment:protocol",
                        "specAlignment:binding",
                        "specAlignment:agent_card",
                        "specAlignment:route",
                        "specAlignment:jsonrpc");
    }
}
