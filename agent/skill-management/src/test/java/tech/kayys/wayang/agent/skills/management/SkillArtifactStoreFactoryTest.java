package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillArtifactStoreFactoryTest {

    @Test
    void createsMemoryStoreByDefault() {
        assertThat(new SkillArtifactStoreFactory().create(null))
                .isInstanceOf(InMemorySkillArtifactStore.class);
    }

    @Test
    void createsFilesystemStore(@TempDir Path tempDir) {
        assertThat(new SkillArtifactStoreFactory().create(SkillArtifactStoreConfig.fileSystem(tempDir)))
                .isInstanceOf(FileSystemSkillArtifactStore.class);
    }

    @Test
    void createsObjectStorageStoreWithObjectDependency() {
        SkillArtifactStore store = new SkillArtifactStoreFactory(new InMemoryObjectStore())
                .create(SkillArtifactStoreConfig.objectStorage("tenant-a/artifacts"));

        assertThat(store).isInstanceOf(ObjectStorageSkillArtifactStore.class);
    }

    @Test
    void createsJdbcStoreWithDataSourceDependency() {
        SkillArtifactStore store = new SkillArtifactStoreFactory(new EmptyDataSource())
                .create(SkillArtifactStoreConfig.jdbc("skill_artifacts", false));

        assertThat(store).isInstanceOf(JdbcSkillArtifactStore.class);
    }

    @Test
    void createsHybridStore() {
        SkillArtifactStore store = new SkillArtifactStoreFactory(new InMemoryObjectStore())
                .create(SkillArtifactStoreConfig.hybrid(
                        SkillArtifactStoreConfig.objectStorage("tenant-a/artifacts"),
                        SkillArtifactStoreConfig.memory()));

        assertThat(store).isInstanceOf(HybridSkillArtifactStore.class);
    }

    @Test
    void createsCustomStore() {
        InMemorySkillArtifactStore custom = new InMemorySkillArtifactStore();
        SkillArtifactStore store = new SkillArtifactStoreFactory(Map.of("tenant-artifacts", custom))
                .create(SkillArtifactStoreConfig.custom("tenant-artifacts"));

        assertThat(store).isSameAs(custom);
    }

    @Test
    void rejectsObjectStorageWithoutObjectStore() {
        SkillArtifactStoreFactory factory = new SkillArtifactStoreFactory();

        assertThatThrownBy(() -> factory.create(SkillArtifactStoreConfig.objectStorage("tenant-a/artifacts")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("objectStore");
    }

    @Test
    void rejectsJdbcWithoutDataSource() {
        SkillArtifactStoreFactory factory = new SkillArtifactStoreFactory();

        assertThatThrownBy(() -> factory.create(SkillArtifactStoreConfig.jdbc()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jdbcDataSource");
    }

    @Test
    void rejectsUnknownCustomStore() {
        SkillArtifactStoreFactory factory = new SkillArtifactStoreFactory(Map.of());

        assertThatThrownBy(() -> factory.create(SkillArtifactStoreConfig.custom("missing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No custom artifact store registered for: missing");
    }

    private static final class InMemoryObjectStore implements SkillManagementObjectStore {
        private final Map<String, byte[]> objects = new LinkedHashMap<>();

        @Override
        public Optional<byte[]> get(String key) {
            return Optional.ofNullable(objects.get(key));
        }

        @Override
        public List<String> list(String prefix) {
            String normalizedPrefix = prefix == null ? "" : prefix;
            return objects.keySet().stream()
                    .filter(key -> key.startsWith(normalizedPrefix))
                    .toList();
        }

        @Override
        public void put(String key, byte[] content) {
            objects.put(key, content);
        }

        @Override
        public boolean delete(String key) {
            return objects.remove(key) != null;
        }
    }

    private static final class EmptyDataSource implements DataSource {

        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("No connection expected");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("unwrap is not supported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
