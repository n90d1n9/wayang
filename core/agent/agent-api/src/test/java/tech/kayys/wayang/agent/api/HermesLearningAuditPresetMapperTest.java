package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotion;
import tech.kayys.wayang.agent.hermes.HermesLearningPromotionReceipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesLearningAuditPresetMapperTest {

    private final HermesLearningAuditPresetMapper mapper = new HermesLearningAuditPresetMapper();

    @Test
    void mapsWindowPresetsToLearningAuditQueries() {
        assertThat(mapper.latest(new HermesLearningAuditPresetRequest(5)).query())
                .extracting(
                        query -> query.limit(),
                        query -> query.persistedOnly())
                .containsExactly(5, false);
        assertThat(mapper.persisted(new HermesLearningAuditPresetRequest(7)).query())
                .extracting(
                        query -> query.limit(),
                        query -> query.persistedOnly())
                .containsExactly(7, true);
    }

    @Test
    void mapsIdentityPresetsToFilteredQueries() {
        assertThat(mapper.skill("skill-a", new HermesLearningAuditPresetRequest(2)).query())
                .extracting(
                        query -> query.skillId(),
                        query -> query.limit())
                .containsExactly("skill-a", 2);
        assertThat(mapper.status(HermesLearningPromotion.STATUS_APPROVED, new HermesLearningAuditPresetRequest(3))
                .query())
                .extracting(
                        query -> query.status(),
                        query -> query.limit())
                .containsExactly(HermesLearningPromotion.STATUS_APPROVED, 3);
        assertThat(mapper.outcome(
                HermesLearningPromotionReceipt.OUTCOME_PERSISTED,
                new HermesLearningAuditPresetRequest(4)).query())
                .extracting(
                        query -> query.outcome(),
                        query -> query.limit())
                .containsExactly(HermesLearningPromotionReceipt.OUTCOME_PERSISTED, 4);
        assertThat(mapper.receipt("key-skill-a", new HermesLearningAuditPresetRequest(5)).query())
                .extracting(
                        query -> query.idempotencyKey(),
                        query -> query.limit())
                .containsExactly("key-skill-a", 5);
    }

    @Test
    void appliesCursorParametersToPresetQueries() {
        assertThat(mapper.persisted(new HermesLearningAuditPresetRequest(7, null, "key-3")).query())
                .extracting(
                        query -> query.persistedOnly(),
                        query -> query.beforeReceiptId(),
                        query -> query.afterReceiptId(),
                        query -> query.limit())
                .containsExactly(true, "", "key-3", 7);
        assertThat(mapper.skill("skill-a", new HermesLearningAuditPresetRequest(5, "key-2", null)).query())
                .extracting(
                        query -> query.skillId(),
                        query -> query.beforeReceiptId(),
                        query -> query.afterReceiptId(),
                        query -> query.limit())
                .containsExactly("skill-a", "key-2", "", 5);
    }

    @Test
    void defaultsMissingPresetLimit() {
        assertThat(mapper.latest(null).query().limit()).isEqualTo(100);
        assertThat(mapper.persisted(null).query().limit()).isEqualTo(100);
        assertThat(mapper.skill("skill-a", null).query().limit()).isEqualTo(100);
    }

    @Test
    void trimsIdentityPresetValues() {
        assertThat(mapper.skill(" skill-a ", new HermesLearningAuditPresetRequest(2)).query().skillId())
                .isEqualTo("skill-a");
        assertThat(mapper.receipt(" key-skill-a ", new HermesLearningAuditPresetRequest(2)).query().idempotencyKey())
                .isEqualTo("key-skill-a");
    }

    @Test
    void rejectsBlankIdentityPresetValues() {
        assertThatThrownBy(() -> mapper.skill(null, new HermesLearningAuditPresetRequest(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("skillId is required");
        assertThatThrownBy(() -> mapper.status(" ", new HermesLearningAuditPresetRequest(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("status is required");
        assertThatThrownBy(() -> mapper.outcome(" ", new HermesLearningAuditPresetRequest(4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("outcome is required");
        assertThatThrownBy(() -> mapper.receipt(" ", new HermesLearningAuditPresetRequest(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idempotencyKey is required");
    }

    @Test
    void rejectsConflictingPresetCursors() {
        assertThatThrownBy(() -> mapper.latest(new HermesLearningAuditPresetRequest(5, "key-1", "key-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("beforeReceiptId and afterReceiptId cannot both be set");
    }
}
