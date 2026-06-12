package tech.kayys.wayang.a2ui.wayang.http;

import tech.kayys.wayang.a2ui.wayang.WayangA2uiContractFixtures;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpScenarios;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpSmokeResult;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpSmokeResultProjectionTest {

    @Test
    void projectsOrderedPassingSmokeResultAndRecordDelegates() {
        WayangA2uiHttpSmokeResult result = WayangA2uiContractFixtures.contractSmokeResult();

        Map<String, Object> values = HttpSmokeResultProjection.result(result);

        assertThat(result.toMap()).isEqualTo(values);
        assertThat(values.keySet()).containsExactly(
                "passed",
                "exitCode",
                "suiteReport",
                "expectationResult",
                "attributes");
        assertThat(values)
                .containsEntry("passed", true)
                .containsEntry("exitCode", WayangA2uiHttpSmokeResult.EXIT_SUCCESS);
        assertThat((Map<String, Object>) values.get("suiteReport"))
                .containsEntry("suiteId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry("passed", true);
        assertThat((Map<String, Object>) values.get("expectationResult"))
                .containsEntry("targetId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry("passed", true)
                .containsEntry("validationIssues", List.of());
        assertThat((Map<String, Object>) values.get("attributes"))
                .containsEntry("suiteId", WayangA2uiHttpScenarios.SMOKE_SUITE_ID)
                .containsEntry("routeCount", 6);
    }

    @Test
    void projectsOrderedFailingSmokeResultWithExpectationIssues() {
        WayangA2uiHttpSmokeResult result = WayangA2uiContractFixtures.failedContractSmokeResult();

        Map<String, Object> values = HttpSmokeResultProjection.result(result);

        assertThat(values)
                .containsEntry("passed", false)
                .containsEntry("exitCode", WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat((Map<String, Object>) values.get("suiteReport"))
                .containsEntry("passed", true)
                .containsEntry("issueCount", 0L);
        assertThat((Map<String, Object>) values.get("expectationResult"))
                .containsEntry("passed", false)
                .containsEntry("issueCount", 1);
        assertThat((Iterable<Map<String, Object>>) ((Map<String, Object>) values.get("expectationResult"))
                .get("validationIssues"))
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("field", "scenarioCount")
                        .containsEntry("message", "Expected scenarioCount to match exactly."));
    }
}
