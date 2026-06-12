package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyTest {

    @Test
    void countPolicyKeepsLatestLines() {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy policy =
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.ofMaxTrails(2);

        assertThat(policy.retainLines(List.of("first", "second", "third")))
                .containsExactly("second", "third");
        assertThat(policy.byteLimited()).isFalse();
        assertThat(policy.toMap())
                .containsEntry("maxTrails", 2)
                .containsEntry("maxBytes", 0L)
                .containsEntry("maxBytesDisplay", "unlimited")
                .containsEntry("byteLimited", false);
    }

    @Test
    void bytePolicyKeepsNewestSuffixWithinLimit() {
        long limit = bytes("second") + bytes("third");
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy policy =
                new AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy(
                        10,
                        limit,
                        Map.of());

        assertThat(policy.retainLines(List.of("first", "second", "third")))
                .containsExactly("second", "third");
        assertThat(policy.byteLimited()).isTrue();
    }

    @Test
    void bytePolicyKeepsLatestLineEvenWhenItExceedsLimit() {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy policy =
                new AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy(
                        10,
                        1,
                        Map.of());

        assertThat(policy.retainLines(List.of("first", "second", "third")))
                .containsExactly("third");
    }

    @Test
    void fromMapBindsAliasesAndNestedRetention() {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy policy =
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.fromMap(
                        Map.of(
                                "capacity",
                                2,
                                "retentionPolicy",
                                Map.of(
                                        "maxEvents",
                                        "4",
                                        "byteLimit",
                                        "2KiB",
                                        "futureMode",
                                        "archive")),
                        100);

        assertThat(policy.maxTrails()).isEqualTo(4);
        assertThat(policy.maxBytes()).isEqualTo(2048L);
        assertThat(policy.attributes()).containsEntry("futureMode", "archive");
    }

    @Test
    void fromMapTreatsUnlimitedByteLimitAliasAsUnlimited() {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy policy =
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.fromMap(
                        Map.of(
                                "maxTrails",
                                4,
                                "maxBytes",
                                "unlimited"),
                        100);

        assertThat(policy.maxBytes()).isZero();
        assertThat(policy.byteLimited()).isFalse();
    }

    @Test
    void fromMapPreservesInvalidCountLimitMetadata() {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy policy =
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.fromMap(
                        Map.of(
                                "maxEvents",
                                "many"),
                        100);

        assertThat(policy.maxTrails()).isEqualTo(100);
        assertThat(policy.maxTrailsParseInvalid()).isTrue();
        assertThat(policy.maxTrailsParse())
                .containsEntry("rawValue", "many")
                .containsEntry("issue", AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy
                        .ISSUE_INVALID_MAX_TRAILS);
        assertThat(map(policy.toMap().get("attributes")).get("maxTrailsParse"))
                .isInstanceOf(Map.class);
    }

    @Test
    void fromMapPreservesInvalidByteLimitMetadata() {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy policy =
                AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy.fromMap(
                        Map.of(
                                "maxTrails",
                                4,
                                "maxBytes",
                                "64xb"),
                        100);

        assertThat(policy.maxBytes()).isZero();
        assertThat(policy.byteLimited()).isFalse();
        assertThat(policy.maxBytesParseInvalid()).isTrue();
        assertThat(policy.maxBytesParse())
                .containsEntry("rawValue", "64xb")
                .containsEntry("issue", AgenticCommerceWayangByteSizes.ISSUE_INVALID_BYTE_SIZE);
        assertThat(map(policy.toMap().get("attributes")).get("maxBytesParse"))
                .isInstanceOf(Map.class);
    }

    @Test
    void assessmentReportsRecommendedByteFloorForContractSamples() {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy policy =
                new AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy(
                        10,
                        1,
                        Map.of());

        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment assessment =
                policy.assess(
                        AgenticCommerceWayangPersistenceTransferAuditContractHarness.contractJournalLineSamples(),
                        AgenticCommerceWayangPersistenceTransferAuditContractHarness
                                .DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT);

        assertThat(assessment.satisfiesContract()).isFalse();
        assertThat(assessment.retainedTrailCount()).isEqualTo(1);
        assertThat(assessment.expectedRetainedTrailCount()).isEqualTo(2);
        assertThat(assessment.recommendedMinBytes()).isGreaterThan(1L);
        assertThat(assessment.toMap())
                .containsEntry("maxBytesDisplay", "1 B")
                .containsEntry("byteLimited", true)
                .containsEntry("satisfiesContract", false);
        assertThat((String) assessment.toMap().get("recommendedMinBytesDisplay"))
                .isNotBlank();
    }

    @Test
    void assessmentPassesAtRecommendedByteFloor() {
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy underSized =
                new AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy(
                        10,
                        1,
                        Map.of());
        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicyAssessment underSizedAssessment =
                underSized.assess(
                        AgenticCommerceWayangPersistenceTransferAuditContractHarness.contractJournalLineSamples(),
                        AgenticCommerceWayangPersistenceTransferAuditContractHarness
                                .DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT);

        AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy recommended =
                new AgenticCommerceWayangPersistenceTransferAuditRetentionPolicy(
                        10,
                        underSizedAssessment.recommendedMinBytes(),
                        Map.of());

        assertThat(recommended.assess(
                        AgenticCommerceWayangPersistenceTransferAuditContractHarness.contractJournalLineSamples(),
                        AgenticCommerceWayangPersistenceTransferAuditContractHarness
                                .DEFAULT_EXPECTED_RETAINED_TRAIL_COUNT)
                .satisfiesContract()).isTrue();
    }

    private static long bytes(String line) {
        return line.getBytes(StandardCharsets.UTF_8).length
                + System.lineSeparator().getBytes(StandardCharsets.UTF_8).length;
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
