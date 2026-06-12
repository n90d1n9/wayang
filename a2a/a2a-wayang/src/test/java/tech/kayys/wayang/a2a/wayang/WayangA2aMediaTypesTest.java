package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aMediaTypesTest {

    @Test
    void matchesExactTypesAndWildcards() {
        List<String> supported = WayangA2aMediaTypes.copyDistinct(List.of(
                " text/plain ",
                "application/json",
                "text/plain"));

        assertThat(supported).containsExactly("text/plain", "application/json");
        assertThat(WayangA2aMediaTypes.supports(supported, "text/plain")).isTrue();
        assertThat(WayangA2aMediaTypes.supports(supported, "text/*")).isTrue();
        assertThat(WayangA2aMediaTypes.supports(List.of("application/*"), "application/json")).isTrue();
        assertThat(WayangA2aMediaTypes.intersects(supported, List.of("image/png", "*/*"))).isTrue();
        assertThat(WayangA2aMediaTypes.intersects(supported, List.of("image/png"))).isFalse();
    }
}
