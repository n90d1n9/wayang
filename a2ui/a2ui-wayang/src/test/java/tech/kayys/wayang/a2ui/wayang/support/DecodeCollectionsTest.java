package tech.kayys.wayang.a2ui.wayang.support;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DecodeCollectionsTest {

    @Test
    void decodesDistinctTokensFromScalarText() {
        assertThat(DecodeCollections.distinctTokens(" a2ui.exchange, a2ui.smoke a2ui.exchange "))
                .containsExactly("a2ui.exchange", "a2ui.smoke");
        assertThat(DecodeCollections.distinctTokens(" ")).isEmpty();
        assertThat(DecodeCollections.distinctTokens(null)).isEmpty();
    }

    @Test
    void decodesDistinctTrimmedValuesFromLists() {
        assertThat(DecodeCollections.distinctTokens(List.of(
                " a2ui.exchange ",
                "a2ui.smoke",
                "a2ui.smoke",
                " ")))
                .containsExactly("a2ui.exchange", "a2ui.smoke");
    }

    @Test
    void decodesTrimmedTextValuesWithoutSplitting() {
        assertThat(DecodeCollections.texts(List.of(" SUCCESS ", " TRANSPORT_ERROR ", "")))
                .containsExactly("SUCCESS", "TRANSPORT_ERROR", "");
        assertThat(DecodeCollections.texts(" SUCCESS TRANSPORT_ERROR "))
                .containsExactly("SUCCESS TRANSPORT_ERROR");
        assertThat(DecodeCollections.texts(" ")).isEmpty();
    }

    @Test
    void decodesRawTextValuesWithoutTrimming() {
        assertThat(DecodeCollections.rawTexts(List.of(" SUCCESS ", " TRANSPORT_ERROR ", "")))
                .containsExactly(" SUCCESS ", " TRANSPORT_ERROR ", "");
        assertThat(DecodeCollections.rawTexts(" SUCCESS TRANSPORT_ERROR "))
                .containsExactly(" SUCCESS TRANSPORT_ERROR ");
        assertThat(DecodeCollections.rawTexts(" ")).isEmpty();
    }

    @Test
    void decodesNonBlankTextLists() {
        assertThat(DecodeCollections.nonBlankTexts(List.of(" SUCCESS ", " ", "SUCCESS")))
                .containsExactly("SUCCESS", "SUCCESS");
        assertThat(DecodeCollections.nonBlankTexts(null)).isEmpty();
    }

    @Test
    void decodesDistinctNonBlankTextLists() {
        assertThat(DecodeCollections.distinctNonBlankTexts(List.of(" SUCCESS ", " ", "SUCCESS")))
                .containsExactly("SUCCESS");
        assertThat(DecodeCollections.distinctNonBlankTexts(null)).isEmpty();
    }

    @Test
    void decodesCommaSeparatedTextSetsFromScalarText() {
        assertThat(DecodeCollections.commaSeparatedTextSet(" run-a, run-b run-c, ,run-a "))
                .containsExactlyInAnyOrder("run-a", "run-b run-c");
        assertThat(DecodeCollections.commaSeparatedTextSet(" ")).isEmpty();
        assertThat(DecodeCollections.commaSeparatedTextSet(null)).isEmpty();
    }

    @Test
    void decodesCommaSeparatedTextSetsFromCollectionsWithoutSplittingEntries() {
        assertThat(DecodeCollections.commaSeparatedTextSet(List.of(
                " run-a ",
                "run-b,run-c",
                " ",
                "run-a")))
                .containsExactlyInAnyOrder("run-a", "run-b,run-c");
    }

    @Test
    void decodesIntegerValuesSkippingInvalidEntries() {
        assertThat(DecodeCollections.integers(List.of("200", "bad", 404L)))
                .containsExactly(200, 404);
        assertThat(DecodeCollections.integers(" 500 "))
                .containsExactly(500);
        assertThat(DecodeCollections.integers("bad")).isEmpty();
    }

    @Test
    void decodesMapValuesFromMapOrListInputs() {
        assertThat(DecodeCollections.maps(Map.of("index", 1)))
                .singleElement()
                .satisfies(map -> assertThat(map).containsEntry("index", 1));
        assertThat(DecodeCollections.maps(List.of(
                Map.of("index", 1),
                "ignored",
                Map.of("index", 2))))
                .extracting(map -> map.get("index"))
                .containsExactly(1, 2);
        assertThat(DecodeCollections.maps("ignored")).isEmpty();
    }
}
