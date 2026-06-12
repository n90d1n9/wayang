package tech.kayys.wayang.agent.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesJournalPresetMapperTest {

    private final HermesJournalPresetMapper mapper = new HermesJournalPresetMapper();

    @Test
    void mapsLatestPresetToLatestQuery() {
        var directive = mapper.latest(new HermesJournalPresetRequest(5));

        assertThat(directive.target()).isEqualTo("latest");
        assertThat(directive.query())
                .extracting(
                        query -> query.outcome(),
                        query -> query.limit())
                .containsExactly("", 5);
    }

    @Test
    void mapsFailurePresetToFailureQuery() {
        var directive = mapper.failures(new HermesJournalPresetRequest(7));

        assertThat(directive.target()).isEqualTo("outcome:failed");
        assertThat(directive.query())
                .extracting(
                        query -> query.outcome(),
                        query -> query.limit())
                .containsExactly("failed", 7);
    }

    @Test
    void mapsLearningPresetToLearningTypePrefixQuery() {
        var directive = mapper.learning(new HermesJournalPresetRequest(6));

        assertThat(directive.target()).isEqualTo("type-prefix:skill.learning");
        assertThat(directive.query())
                .extracting(
                        query -> query.typePrefix(),
                        query -> query.limit())
                .containsExactly("skill.learning", 6);
    }

    @Test
    void defaultsMissingPresetLimit() {
        assertThat(mapper.latest(null).query().limit()).isEqualTo(100);
        assertThat(mapper.failures(null).query().limit()).isEqualTo(100);
        assertThat(mapper.learning(null).query().limit()).isEqualTo(100);
    }

    @Test
    void mapsIdentityPresetsToFilteredQueries() {
        assertThat(mapper.request("req-1", new HermesJournalPresetRequest(2)).query())
                .extracting(
                        query -> query.requestId(),
                        query -> query.limit())
                .containsExactly("req-1", 2);
        assertThat(mapper.session("session-a", new HermesJournalPresetRequest(3)).query())
                .extracting(
                        query -> query.sessionId(),
                        query -> query.limit())
                .containsExactly("session-a", 3);
        assertThat(mapper.user("user-a", new HermesJournalPresetRequest(4)).query())
                .extracting(
                        query -> query.userId(),
                        query -> query.limit())
                .containsExactly("user-a", 4);
        assertThat(mapper.tenant("tenant-a", new HermesJournalPresetRequest(5)).query())
                .extracting(
                        query -> query.tenantId(),
                        query -> query.limit())
                .containsExactly("tenant-a", 5);
    }

    @Test
    void trimsIdentityPresetValues() {
        assertThat(mapper.request(" req-1 ", new HermesJournalPresetRequest(2)).query().requestId())
                .isEqualTo("req-1");
    }

    @Test
    void rejectsBlankIdentityPresetValues() {
        assertThatThrownBy(() -> mapper.request(null, new HermesJournalPresetRequest(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("requestId is required");
        assertThatThrownBy(() -> mapper.session(" ", new HermesJournalPresetRequest(3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sessionId is required");
        assertThatThrownBy(() -> mapper.user(" ", new HermesJournalPresetRequest(4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("userId is required");
        assertThatThrownBy(() -> mapper.tenant(" ", new HermesJournalPresetRequest(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tenantId is required");
    }
}
