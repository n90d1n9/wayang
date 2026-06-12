package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonReportMapsTest {

    @Test
    void bodyMapParsesStrictJsonAndKeepsLenientProbeFallback() {
        assertThat(WayangA2aJsonReportMaps.bodyMap("{\"statusCode\":200}"))
                .containsEntry("statusCode", 200);
        assertThat(WayangA2aJsonReportMaps.bodyMap("")).isEmpty();

        assertThatThrownBy(() -> WayangA2aJsonReportMaps.bodyMap("not-json"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(WayangA2aJsonReportMaps.lenientBodyMap("not-json")).isEmpty();
    }

    @Test
    void mapHelpersCopyChildObjectsAndObjectLists() {
        Map<String, Object> child = Map.of("route", "/a2a");
        Map<String, Object> parent = Map.of("child", child);

        assertThat(WayangA2aJsonReportMaps.map(child)).containsEntry("route", "/a2a");
        assertThat(WayangA2aJsonReportMaps.child(parent, "child")).containsEntry("route", "/a2a");
        assertThat(WayangA2aJsonReportMaps.child(parent, "missing")).isEmpty();
        assertThat(WayangA2aJsonReportMaps.copyObjects(List.of(child))).containsExactly(child);
    }

    @Test
    void scalarHelpersNormalizeTextNumbersAndBooleans() {
        assertThat(WayangA2aJsonReportMaps.text(" value ", "fallback")).isEqualTo("value");
        assertThat(WayangA2aJsonReportMaps.text(" ", "fallback")).isEqualTo("fallback");
        assertThat(WayangA2aJsonReportMaps.number("42", 7)).isEqualTo(42);
        assertThat(WayangA2aJsonReportMaps.number("-5", 7)).isZero();
        assertThat(WayangA2aJsonReportMaps.number("nope", 7)).isEqualTo(7);
        assertThat(WayangA2aJsonReportMaps.bool("true", false)).isTrue();
        assertThat(WayangA2aJsonReportMaps.bool(null, true)).isTrue();
    }
}
