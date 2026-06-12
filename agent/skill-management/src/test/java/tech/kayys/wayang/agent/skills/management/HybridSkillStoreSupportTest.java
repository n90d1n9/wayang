package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class HybridSkillStoreSupportTest {

    @Test
    void returnsPrimaryValueWithoutReadingFallback() {
        AtomicBoolean fallbackRead = new AtomicBoolean();

        Optional<String> value = HybridSkillStoreSupport.primaryOrFallback(
                () -> Optional.of("primary"),
                () -> {
                    fallbackRead.set(true);
                    return Optional.of("fallback");
                });

        assertThat(value).contains("primary");
        assertThat(fallbackRead).isFalse();
    }

    @Test
    void readsFallbackWhenPrimaryIsMissing() {
        Optional<String> value = HybridSkillStoreSupport.primaryOrFallback(
                Optional::empty,
                () -> Optional.of("fallback"));

        assertThat(value).contains("fallback");
    }

    @Test
    void repairsPrimaryWhenFallbackServesMissingValue() {
        AtomicBoolean repaired = new AtomicBoolean();

        Optional<String> value = HybridSkillStoreSupport.primaryOrFallback(
                Optional::empty,
                () -> Optional.of("fallback"),
                repairedValue -> {
                    assertThat(repairedValue).isEqualTo("fallback");
                    repaired.set(true);
                });

        assertThat(value).contains("fallback");
        assertThat(repaired).isTrue();
    }

    @Test
    void returnsFallbackValueWhenRepairFails() {
        Optional<String> value = HybridSkillStoreSupport.primaryOrFallback(
                Optional::empty,
                () -> Optional.of("fallback"),
                repairedValue -> {
                    throw new IllegalStateException("repair unavailable");
                });

        assertThat(value).contains("fallback");
    }

    @Test
    void readsFallbackWhenPrimaryLookupFails() {
        Optional<String> value = HybridSkillStoreSupport.primaryOrFallback(
                () -> {
                    throw new IllegalStateException("primary unavailable");
                },
                () -> Optional.of("fallback"));

        assertThat(value).contains("fallback");
    }

    @Test
    void removesFromBothStores() {
        AtomicInteger calls = new AtomicInteger();

        boolean removed = HybridSkillStoreSupport.removeFromBoth(
                () -> {
                    calls.incrementAndGet();
                    return true;
                },
                () -> {
                    calls.incrementAndGet();
                    return false;
                });

        assertThat(removed).isTrue();
        assertThat(calls).hasValue(2);
    }

    @Test
    void mergesListsWithPrimaryEntriesOverridingFallbackEntries() {
        List<Item> merged = HybridSkillStoreSupport.mergeFallbackThenPrimary(
                List.of(new Item("backup", "fallback"), new Item("override", "fallback")),
                List.of(new Item("override", "primary"), new Item("new", "primary")),
                Item::id);

        assertThat(merged).extracting(Item::id)
                .containsExactly("backup", "override", "new");
        assertThat(merged).extracting(Item::value)
                .containsExactly("fallback", "primary", "primary");
    }

    @Test
    void mergeListFallsBackWhenPrimaryListFails() {
        List<Item> merged = HybridSkillStoreSupport.mergeFallbackThenPrimary(
                () -> List.of(new Item("backup", "fallback")),
                () -> {
                    throw new IllegalStateException("primary unavailable");
                },
                Item::id);

        assertThat(merged).extracting(Item::id).containsExactly("backup");
    }

    @Test
    void mergesMapsWithPrimaryValuesOverridingFallbackValues() {
        Map<String, String> merged = HybridSkillStoreSupport.mergeFallbackThenPrimary(
                Map.of("backup", "fallback", "override", "fallback"),
                Map.of("override", "primary", "new", "primary"));

        assertThat(merged)
                .containsEntry("backup", "fallback")
                .containsEntry("override", "primary")
                .containsEntry("new", "primary");
    }

    @Test
    void mergeMapFallsBackWhenPrimarySnapshotFails() {
        Map<String, String> merged = HybridSkillStoreSupport.mergeFallbackThenPrimary(
                () -> Map.of("backup", "fallback"),
                () -> {
                    throw new IllegalStateException("primary unavailable");
                });

        assertThat(merged).containsOnly(Map.entry("backup", "fallback"));
    }

    private record Item(String id, String value) {
    }
}
