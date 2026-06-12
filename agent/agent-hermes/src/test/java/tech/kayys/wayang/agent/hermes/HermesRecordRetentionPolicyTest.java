package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRecordRetentionPolicyTest {

    @Test
    void retainsNewestEntriesFromAppendOrderedRecords() {
        HermesRecordRetentionPolicy policy = HermesRecordRetentionPolicy.bounded(2);

        assertThat(policy.retainNewestFromAppendOrder(List.of("old", "middle", "new")))
                .containsExactly("middle", "new");
        assertThat(policy.staleCount(3)).isEqualTo(1);
        assertThat(policy.allowsAll(2)).isTrue();
    }

    @Test
    void selectsStaleEntriesFromNewestFirstRecords() {
        HermesRecordRetentionPolicy policy = HermesRecordRetentionPolicy.bounded(2);

        assertThat(policy.staleFromNewestFirst(List.of("new", "middle", "old")))
                .containsExactly("old");
    }

    @Test
    void selectsStaleEntriesFromOldestFirstComparator() {
        HermesRecordRetentionPolicy policy = HermesRecordRetentionPolicy.bounded(2);

        assertThat(policy.staleFromOldestFirst(
                        List.of("003-new", "001-old", "002-middle"),
                        Comparator.naturalOrder()))
                .containsExactly("001-old");
    }

    @Test
    void normalizesInvalidLimitsToOneRetainedEntry() {
        HermesRecordRetentionPolicy policy = HermesRecordRetentionPolicy.bounded(0);

        assertThat(policy.maxEntries()).isEqualTo(1);
        assertThat(policy.retainNewestFromAppendOrder(List.of("old", "new")))
                .containsExactly("new");
        assertThat(policy.toMetadata())
                .containsEntry("retentionMode", "max-entries")
                .containsEntry("maxEntries", 1);
    }
}
