package tech.kayys.wayang.a2ui.wayang.action;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActionContextReaderTest {

    @Test
    void readsTrimmedTextAndAliasFallbacks() {
        A2uiUserAction action = action(Map.of(
                "runId", " run-1 ",
                "tenant", " tenant-a ",
                "tenantId", " "));

        assertThat(ActionContextReader.text(action, "runId")).isEqualTo("run-1");
        assertThat(ActionContextReader.text(action, "missing")).isEmpty();
        assertThat(ActionContextReader.firstText(action, "tenantId", "tenant"))
                .isEqualTo("tenant-a");
        assertThat(ActionContextReader.firstText(action)).isEmpty();
    }

    @Test
    void readsNumericValuesFromNumbersAndStrings() {
        A2uiUserAction action = action(Map.of(
                "limit", " 10 ",
                "offset", 2L,
                "afterSequence", " 7 ",
                "blank", " "));

        assertThat(ActionContextReader.integer(action, "limit")).isEqualTo(10);
        assertThat(ActionContextReader.integer(action, "offset")).isEqualTo(2);
        assertThat(ActionContextReader.integer(action, "blank")).isNull();
        assertThat(ActionContextReader.integer(action, "missing")).isNull();
        assertThat(ActionContextReader.longValue(action, "afterSequence")).isEqualTo(7L);
        assertThat(ActionContextReader.longValue(action, "offset")).isEqualTo(2L);
    }

    @Test
    void keepsInvalidNumericContextFailuresVisible() {
        A2uiUserAction action = action(Map.of("limit", "later"));

        assertThatThrownBy(() -> ActionContextReader.integer(action, "limit"))
                .isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> ActionContextReader.longValue(action, "limit"))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void detectsPresentContextValuesAndAddsSnapshotValues() {
        A2uiUserAction action = action(Map.of(
                "runId", "run-1",
                "blank", " "));

        A2uiUserAction enriched = ActionContextReader.withValue(action, "afterSequence", 4L);

        assertThat(ActionContextReader.hasValue(action, "runId")).isTrue();
        assertThat(ActionContextReader.hasValue(action, "blank")).isFalse();
        assertThat(ActionContextReader.hasValue(action, "missing")).isFalse();
        assertThat(action.context()).doesNotContainKey("afterSequence");
        assertThat(enriched.context())
                .containsEntry("runId", "run-1")
                .containsEntry("afterSequence", 4L);
        assertThat(enriched.name()).isEqualTo(action.name());
        assertThat(enriched.surfaceId()).isEqualTo(action.surfaceId());
        assertThat(enriched.sourceComponentId()).isEqualTo(action.sourceComponentId());
        assertThat(enriched.timestamp()).isEqualTo(action.timestamp());
    }

    private static A2uiUserAction action(Map<String, Object> context) {
        return new A2uiUserAction(
                WayangA2uiActions.RUN_EVENTS,
                "main",
                "button",
                Instant.parse("2026-05-31T00:00:00Z"),
                context);
    }
}
