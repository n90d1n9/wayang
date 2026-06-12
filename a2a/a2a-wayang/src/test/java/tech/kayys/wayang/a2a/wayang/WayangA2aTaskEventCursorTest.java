package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aTaskEventCursorTest {

    @Test
    void normalizesCursorBoundsAndSlicesEvents() {
        WayangA2aTaskEventCursor cursor = WayangA2aTaskEventCursor.of(1, 2);

        assertThat(cursor.slice(List.of(event(1), event(2), event(3), event(4))))
                .extracting(WayangA2aTaskEvent::sequence)
                .containsExactly(2L, 3L);
        assertThat(WayangA2aTaskEventCursor.of(-10, -1))
                .satisfies(normalized -> {
                    assertThat(normalized.afterSequence()).isZero();
                    assertThat(normalized.limit()).isEqualTo(WayangA2aTaskQuery.DEFAULT_LIMIT);
                });
        assertThat(WayangA2aTaskEventCursor.of(0, WayangA2aTaskQuery.MAX_LIMIT + 10).limit())
                .isEqualTo(WayangA2aTaskQuery.MAX_LIMIT);
    }

    @Test
    void readsHttpAndJsonRpcLimitAliases() {
        assertThat(WayangA2aTaskEventCursor.fromHttpAttributes(Map.of(
                        "afterSequence", "2",
                        "limit", "3")))
                .isEqualTo(WayangA2aTaskEventCursor.of(2, 3));
        assertThat(WayangA2aTaskEventCursor.fromJsonRpcParams(Map.of(
                        "afterSequence", 4,
                        "pageSize", 5)))
                .isEqualTo(WayangA2aTaskEventCursor.of(4, 5));
    }

    private static WayangA2aTaskEvent event(long sequence) {
        return new WayangA2aTaskEvent(
                sequence,
                "task-1",
                "context-1",
                WayangA2aTaskEvent.TYPE_STATUS_UPDATED,
                Map.of("sequence", sequence),
                "2026-06-04T00:00:00Z");
    }
}
