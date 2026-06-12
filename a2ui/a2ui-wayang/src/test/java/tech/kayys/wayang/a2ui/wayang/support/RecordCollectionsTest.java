package tech.kayys.wayang.a2ui.wayang.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordCollectionsTest {

    @Test
    void copiesListsAsImmutableSnapshots() {
        List<String> values = new ArrayList<>();
        values.add("alpha");
        values.add("beta");

        List<String> normalized = RecordCollections.copyList(values);

        assertThat(normalized).containsExactly("alpha", "beta");

        values.add("gamma");
        assertThat(normalized).containsExactly("alpha", "beta");
        assertThatThrownBy(() -> normalized.add("delta"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void copyListPreservesListCopyOfNullElementRejection() {
        List<String> values = new ArrayList<>();
        values.add("alpha");
        values.add(null);

        assertThatThrownBy(() -> RecordCollections.copyList(values))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void copiesNullableSingletonsAsImmutableLists() {
        List<String> normalized = RecordCollections.singletonOrEmpty("alpha");

        assertThat(normalized).containsExactly("alpha");
        assertThat(RecordCollections.singletonOrEmpty(null)).isEmpty();
        assertThatThrownBy(() -> normalized.add("beta"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void copiesSetsAsImmutableSnapshots() {
        Set<String> values = new LinkedHashSet<>();
        values.add("alpha");
        values.add("beta");

        Set<String> normalized = RecordCollections.copySet(values);

        assertThat(normalized).containsExactlyInAnyOrder("alpha", "beta");

        values.add("gamma");
        assertThat(normalized).containsExactlyInAnyOrder("alpha", "beta");
        assertThatThrownBy(() -> normalized.add("delta"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void copySetPreservesSetCopyOfNullElementRejection() {
        Set<String> values = new LinkedHashSet<>();
        values.add("alpha");
        values.add(null);

        assertThatThrownBy(() -> RecordCollections.copySet(values))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void copiesOnlyNonNullValues() {
        List<String> values = new ArrayList<>();
        values.add("alpha");
        values.add(null);
        values.add("beta");

        List<String> normalized = RecordCollections.nonNullList(values);

        assertThat(normalized).containsExactly("alpha", "beta");

        values.add("gamma");
        assertThat(normalized).containsExactly("alpha", "beta");
    }

    @Test
    void copiesOnlyNonNullVarargsValues() {
        List<String> normalized = RecordCollections.nonNullVarargs("alpha", null, "beta");

        assertThat(normalized).containsExactly("alpha", "beta");
        assertThat(RecordCollections.nonNullVarargs((String[]) null)).isEmpty();
        assertThatThrownBy(() -> normalized.add("gamma"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void treatsNullAndEmptyListsAsEmpty() {
        assertThat(RecordCollections.copyList(null)).isEmpty();
        assertThat(RecordCollections.copyList(List.of())).isEmpty();
        assertThat(RecordCollections.nonNullList(null)).isEmpty();
        assertThat(RecordCollections.nonNullList(List.of())).isEmpty();
        assertThat(RecordCollections.nonBlankStrings(null)).isEmpty();
        assertThat(RecordCollections.nonBlankStrings(List.of())).isEmpty();
        assertThat(RecordCollections.copySet(null)).isEmpty();
        assertThat(RecordCollections.copySet(Set.of())).isEmpty();
        assertThat(RecordCollections.trimmedNonBlankStringSet(null)).isEmpty();
        assertThat(RecordCollections.trimmedNonBlankStringSet(Set.of())).isEmpty();
    }

    @Test
    void copiesOnlyNonBlankStringsWithoutTrimmingPayloads() {
        List<String> normalized = RecordCollections.nonBlankStrings(List.of(
                "{\"type\":\"ping\"}",
                "   ",
                "\n\t",
                " {\"type\":\"preserve-space\"} "));

        assertThat(normalized).containsExactly(
                "{\"type\":\"ping\"}",
                " {\"type\":\"preserve-space\"} ");
    }

    @Test
    void copiesTrimmedNonBlankStringSets() {
        Set<String> values = new LinkedHashSet<>();
        values.add(" run-a ");
        values.add(" ");
        values.add(null);
        values.add("run-b");

        Set<String> normalized = RecordCollections.trimmedNonBlankStringSet(values);

        assertThat(normalized).containsExactlyInAnyOrder("run-a", "run-b");
        assertThatThrownBy(() -> normalized.add("run-c"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
