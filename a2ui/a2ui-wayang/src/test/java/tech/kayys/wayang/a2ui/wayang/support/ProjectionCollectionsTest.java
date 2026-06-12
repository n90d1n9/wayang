package tech.kayys.wayang.a2ui.wayang.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectionCollectionsTest {

    @Test
    void projectsSetsAsSortedStringLists() {
        List<String> values = ProjectionCollections.sortedStrings(Set.of("run-z", "run-a", "run-m"));

        assertThat(values).containsExactly("run-a", "run-m", "run-z");
        assertThatThrownBy(() -> values.add("run-x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(ProjectionCollections.sortedStrings(null)).isEmpty();
        assertThat(ProjectionCollections.sortedStrings(Set.of())).isEmpty();
    }

    @Test
    void projectsValuesInReferenceOrderThenSortedRemainder() {
        List<String> values = ProjectionCollections.referenceOrderThenSortedRemainder(
                List.of("a2ui.exchange", "a2ui.surfaceCatalog", "a2ui.smoke"),
                List.of("a2ui.customB", "a2ui.smoke", "a2ui.customA", "a2ui.exchange"));

        assertThat(values).containsExactly(
                "a2ui.exchange",
                "a2ui.smoke",
                "a2ui.customA",
                "a2ui.customB");
        assertThatThrownBy(() -> values.add("a2ui.extra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void handlesMissingReferenceOrderAndEmptyInputs() {
        assertThat(ProjectionCollections.referenceOrderThenSortedRemainder(
                null,
                List.of("z", "a")))
                .containsExactly("a", "z");
        assertThat(ProjectionCollections.referenceOrderThenSortedRemainder(
                List.of("a"),
                null))
                .isEmpty();
        assertThat(ProjectionCollections.referenceOrderThenSortedRemainder(
                List.of("a"),
                List.of()))
                .isEmpty();
    }

    @Test
    void ignoresNullEntriesBeforeOrderingProjectionCollections() {
        Set<String> unsorted = new LinkedHashSet<>();
        unsorted.add("run-z");
        unsorted.add(null);
        unsorted.add("run-a");
        List<String> reference = new ArrayList<>();
        reference.add(null);
        reference.add("run-b");
        reference.add("run-a");
        List<String> values = new ArrayList<>();
        values.add("run-c");
        values.add(null);
        values.add("run-a");

        assertThat(ProjectionCollections.sortedStrings(unsorted))
                .containsExactly("run-a", "run-z");
        assertThat(ProjectionCollections.referenceOrderThenSortedRemainder(reference, values))
                .containsExactly("run-a", "run-c");
    }
}
