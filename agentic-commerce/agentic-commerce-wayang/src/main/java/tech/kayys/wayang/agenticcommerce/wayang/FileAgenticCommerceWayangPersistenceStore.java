package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JSON-file backed fallback persistence for Agentic Commerce Wayang state.
 */
public final class FileAgenticCommerceWayangPersistenceStore implements AgenticCommerceWayangPersistenceStore {

    public static final String STORAGE_KIND = "file";
    public static final String RUNTIME_CONFIG_FILE =
            AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.fileName();
    public static final String BOOTSTRAP_CONFIG_FILE =
            AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.fileName();
    public static final String BOOTSTRAP_REPORT_FILE =
            AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.fileName();
    public static final String MANIFEST_FILE =
            AgenticCommerceWayangPersistenceDocuments.MANIFEST.fileName();

    private final Path directory;

    public FileAgenticCommerceWayangPersistenceStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
    }

    public static FileAgenticCommerceWayangPersistenceStore at(Path directory) {
        return new FileAgenticCommerceWayangPersistenceStore(directory);
    }

    public Path directory() {
        return directory;
    }

    public Path runtimeConfigPath() {
        return directory.resolve(RUNTIME_CONFIG_FILE);
    }

    public Path bootstrapConfigPath() {
        return directory.resolve(BOOTSTRAP_CONFIG_FILE);
    }

    public Path bootstrapReportPath() {
        return directory.resolve(BOOTSTRAP_REPORT_FILE);
    }

    public Path manifestPath() {
        return directory.resolve(MANIFEST_FILE);
    }

    @Override
    public String storageKind() {
        return STORAGE_KIND;
    }

    @Override
    public Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
        return readObject(runtimeConfigPath()).map(AgenticCommerceWayangRuntimeConfig::fromMap);
    }

    @Override
    public void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
        AgenticCommerceWayangRuntimeConfig resolved = runtimeConfig == null
                ? AgenticCommerceWayangRuntimeConfig.defaults()
                : runtimeConfig;
        writeObject(runtimeConfigPath(), resolved.toStorageMap());
    }

    @Override
    public Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
        return readObject(bootstrapConfigPath()).map(AgenticCommerceWayangBootstrapConfig::fromMap);
    }

    @Override
    public void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
        AgenticCommerceWayangBootstrapConfig resolved = bootstrapConfig == null
                ? AgenticCommerceWayangBootstrapConfig.defaults()
                : bootstrapConfig;
        writeObject(bootstrapConfigPath(), resolved.toMap());
    }

    @Override
    public Optional<Map<String, Object>> loadBootstrapReport() {
        return readObject(bootstrapReportPath());
    }

    @Override
    public void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport) {
        writeObject(
                bootstrapReportPath(),
                Objects.requireNonNull(bootstrapReport, "bootstrapReport").toMap());
    }

    @Override
    public Optional<Map<String, Object>> loadManifest() {
        return readObject(manifestPath());
    }

    @Override
    public void saveManifest(AgenticCommerceWayangManifest manifest) {
        writeObject(
                manifestPath(),
                Objects.requireNonNull(manifest, "manifest").toMap());
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("storageKind", storageKind());
        values.put("directory", directory.toString());
        values.put("target", AgenticCommerceWayangPersistenceTargetDescriptor.fromStore(this).toMap());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.pathStatusKey(),
                runtimeConfigPath().toString());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.pathStatusKey(),
                bootstrapConfigPath().toString());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.pathStatusKey(),
                bootstrapReportPath().toString());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.MANIFEST.pathStatusKey(),
                manifestPath().toString());
        values.put(
                AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG.availabilityStatusKey(),
                Files.exists(runtimeConfigPath()));
        values.put(
                AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_CONFIG.availabilityStatusKey(),
                Files.exists(bootstrapConfigPath()));
        values.put(
                AgenticCommerceWayangPersistenceDocuments.BOOTSTRAP_REPORT.availabilityStatusKey(),
                Files.exists(bootstrapReportPath()));
        values.put(
                AgenticCommerceWayangPersistenceDocuments.MANIFEST.availabilityStatusKey(),
                Files.exists(manifestPath()));
        return Map.copyOf(values);
    }

    private Optional<Map<String, Object>> readObject(Path path) {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(AgenticCommerceJson.readObject(json));
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to read Agentic Commerce persistence file " + path, exception);
        }
    }

    private void writeObject(Path path, Map<String, Object> values) {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(
                    temporary,
                    AgenticCommerceJson.write(values),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write Agentic Commerce persistence file " + path, exception);
        }
    }
}
