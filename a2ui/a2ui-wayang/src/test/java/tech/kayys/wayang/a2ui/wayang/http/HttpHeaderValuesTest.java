package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpHeaderValuesTest {

    @Test
    void flattensFlexibleHeaderValuesIntoImmutableLists() {
        List<Object> nested = new ArrayList<>();
        nested.add(" one ");
        nested.add(Optional.of(new String[] {"two", " "}));
        List<Object> inner = new ArrayList<>();
        inner.add(null);
        inner.add(Optional.of("three"));
        nested.add(inner);

        List<String> values = HttpHeaderValues.values(nested);

        assertThat(values).containsExactly("one", "two", "three");
        assertThat(HttpHeaderValues.joined(nested)).isEqualTo("one, two, three");
        assertThatThrownBy(() -> values.add("four"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void treatsEmptyOrBlankHeaderValuesAsMissing() {
        assertThat(HttpHeaderValues.values(null)).isEmpty();
        assertThat(HttpHeaderValues.values(Optional.empty())).isEmpty();
        assertThat(HttpHeaderValues.values(" ")).isEmpty();
        assertThat(HttpHeaderValues.joined(null)).isEmpty();
    }
}
