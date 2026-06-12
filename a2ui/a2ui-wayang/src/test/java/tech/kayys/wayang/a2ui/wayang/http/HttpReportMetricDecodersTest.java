package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpReportMetricDecodersTest {

    private static final String OWNER = "A2UI HTTP test report";

    @Test
    void decodesStrictCountsAndIntegerLists() {
        assertThat(HttpReportMetricDecoders.count("42", "successfulCount", OWNER))
                .isEqualTo(42L);
        assertThat(HttpReportMetricDecoders.intCount("7", "exchangeCount", OWNER))
                .isEqualTo(7);
        assertThat(HttpReportMetricDecoders.integerList(
                        List.of("200", 404, " "),
                        "statusCodes",
                        OWNER))
                .containsExactly(200, 404);
    }

    @Test
    void defaultsBlankMetricInputsToZeroOrEmpty() {
        assertThat(HttpReportMetricDecoders.count(" ", "successfulCount", OWNER))
                .isZero();
        assertThat(HttpReportMetricDecoders.integerList(" ", "statusCodes", OWNER))
                .isEmpty();
    }

    @Test
    void rejectsNonNumericCountsAndIntegerListsWithOwnerContext() {
        assertThatThrownBy(() -> HttpReportMetricDecoders.count("nope", "successfulCount", OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP test report count must be numeric: successfulCount");
        assertThatThrownBy(() -> HttpReportMetricDecoders.intCount(
                        Long.MAX_VALUE,
                        "exchangeCount",
                        OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP test report count must fit int: exchangeCount");
        assertThatThrownBy(() -> HttpReportMetricDecoders.integerList(
                        List.of("200", "nope"),
                        "statusCodes",
                        OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP test report integer list must be numeric: statusCodes");
    }
}
