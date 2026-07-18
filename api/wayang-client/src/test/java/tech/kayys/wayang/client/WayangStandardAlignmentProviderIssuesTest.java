package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.alignment.WayangStandardAlignmentProviderIssue;
import tech.kayys.wayang.alignment.WayangStandardAlignmentProviderIssues;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangStandardAlignmentProviderIssuesTest {

    @Test
    void copyDropsNullsAndReturnsImmutableList() {
        WayangStandardAlignmentProviderIssue issue = issue("broken-provider", "boom");

        List<WayangStandardAlignmentProviderIssue> copied =
                WayangStandardAlignmentProviderIssues.copy(Arrays.asList(null, issue));

        assertThat(copied).containsExactly(issue);
        assertThatThrownBy(() -> copied.add(issue))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void toMapsRendersIssuePayloads() {
        assertThat(WayangStandardAlignmentProviderIssues.toMaps(List.of(issue("broken-provider", "boom"))))
                .singleElement()
                .satisfies(payload -> assertThat(payload)
                        .containsEntry("providerId", "broken-provider")
                        .containsEntry("providerClass", "example.Provider")
                        .containsEntry("message", "boom"));
    }

    @Test
    void recommendationsRenderIssueGuidance() {
        assertThat(WayangStandardAlignmentProviderIssues.recommendations(List.of(issue("broken-provider", "boom"))))
                .containsExactly("Review standard-alignment provider broken-provider: boom");
    }

    private static WayangStandardAlignmentProviderIssue issue(String providerId, String message) {
        return new WayangStandardAlignmentProviderIssue(
                providerId,
                "example.Provider",
                message);
    }
}
