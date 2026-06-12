package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesPersistenceStoreKindTest {

    @Test
    void canonicalizesConfiguredAliases() {
        assertThat(HermesPersistenceStoreKind.runtimeEventJournal("object_storage").configValue())
                .isEqualTo("object-storage");
        assertThat(HermesPersistenceStoreKind.runtimeEventJournal("jdbc").configValue())
                .isEqualTo("database");
        assertThat(HermesPersistenceStoreKind.repairStore("in_memory", "approval").configValue())
                .isEqualTo("in-memory");
        assertThat(HermesPersistenceStoreKind.repairStore("db", "ledger").configValue())
                .isEqualTo("database");
    }

    @Test
    void keepsCapabilityFlagsWithCanonicalStore() {
        HermesPersistenceStoreKind hybrid = HermesPersistenceStoreKind.repairStore("hybrid", "store");

        assertThat(hybrid.configValue()).isEqualTo("hybrid");
        assertThat(hybrid.durable()).isTrue();
        assertThat(hybrid.fileFallback()).isTrue();
        assertThat(hybrid.objectStorageCapable()).isTrue();
        assertThat(hybrid.databaseCapable()).isTrue();
        assertThat(hybrid.replaySupported()).isTrue();
    }

    @Test
    void runtimeJournalRejectsRepairOnlyStores() {
        assertThatThrownBy(() -> HermesPersistenceStoreKind.runtimeEventJournal("noop"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runtimeEventJournalStore");
        assertThatThrownBy(() -> HermesPersistenceStoreKind.runtimeEventJournal("in-memory"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runtimeEventJournalStore");
    }
}
