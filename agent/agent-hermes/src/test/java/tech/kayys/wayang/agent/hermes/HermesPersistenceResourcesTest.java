package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesPersistenceResourcesTest {

    @Test
    void prefersDatabaseBeforeObjectStorage() {
        HermesPersistenceResources resources = HermesPersistenceResources.of(
                Optional.of(new InMemoryHermesObjectStorageService()),
                Optional.of(new TestDataSource()));

        String selected = resources.databaseThenObjectStorageOr(
                dataSource -> "database",
                objectStorage -> "object-storage",
                () -> "fallback");

        assertThat(selected).isEqualTo("database");
    }

    @Test
    void usesObjectStorageWhenDatabaseIsMissing() {
        HermesPersistenceResources resources = HermesPersistenceResources.of(
                Optional.of(new InMemoryHermesObjectStorageService()),
                Optional.empty());

        String selected = resources.databaseThenObjectStorageOr(
                dataSource -> "database",
                objectStorage -> "object-storage",
                () -> "fallback");

        assertThat(selected).isEqualTo("object-storage");
    }

    @Test
    void usesFallbackWhenNoDurableResourcesArePresent() {
        String selected = HermesPersistenceResources.empty().databaseThenObjectStorageOr(
                dataSource -> "database",
                objectStorage -> "object-storage",
                () -> "fallback");

        assertThat(selected).isEqualTo("fallback");
    }

    @Test
    void normalizesNullResourceOptionalsToEmpty() {
        HermesPersistenceResources resources = HermesPersistenceResources.of(null, null);

        assertThat(resources.objectStorageService()).isEmpty();
        assertThat(resources.dataSource()).isEmpty();
        assertThatThrownBy(() -> resources.requireObjectStorage("object storage missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("object storage missing");
        assertThatThrownBy(() -> resources.requireDataSource("data source missing"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("data source missing");
    }

    private static final class TestDataSource extends AbstractHermesJdbcDataSource {

        @Override
        protected int executeUpdate(String sql, Map<Integer, Object> parameters) {
            return 0;
        }

        @Override
        protected List<List<Object>> select(String sql, Map<Integer, Object> parameters) {
            return List.of();
        }
    }
}
