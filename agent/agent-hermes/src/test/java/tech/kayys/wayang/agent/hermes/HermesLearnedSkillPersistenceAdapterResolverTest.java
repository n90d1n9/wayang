package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesLearnedSkillPersistenceAdapterResolverTest {

    @Test
    void resolvesSkillManagementAdapterForDefaultTargetPlan(@TempDir Path tempDir) {
        HermesLearnedSkillPersistenceAdapter adapter =
                HermesLearnedSkillPersistenceAdapterResolver.resolve(
                        service(tempDir),
                        HermesSkillPersistencePlan.from(null).targetPlan());

        assertThat(adapter).isInstanceOf(SkillManagementHermesLearnedSkillPersistenceAdapter.class);
        assertThat(adapter.adapterId()).isEqualTo("skill-management");
        assertThat(adapter.targetPlan().targetSummary())
                .isEqualTo("definitions=skill-management,artifacts=skill-management");
        assertThat(HermesLearnedSkillPersistenceAdapterResolver.fileSystemOnly(adapter.targetPlan()))
                .isFalse();
    }

    @Test
    void resolvesFileSystemAdapterForFileOnlyTargetPlan(@TempDir Path tempDir) {
        HermesSkillPersistenceTargetPlan targetPlan = HermesSkillPersistenceStrategy.fromHints(Map.of(
                HermesSkillPersistenceRouteRoles.DEFINITIONS, "file-system",
                HermesSkillPersistenceRouteRoles.ARTIFACTS, "file-system",
                HermesSkillPersistenceRouteRoles.FALLBACK, "file-system"))
                .routePlan()
                .targetPlan();
        HermesLearnedSkillPersistenceAdapterResolverOptions options =
                new HermesLearnedSkillPersistenceAdapterResolverOptions(tempDir, null, null);

        HermesLearnedSkillPersistenceAdapter adapter =
                HermesLearnedSkillPersistenceAdapterResolver.resolve(service(tempDir), targetPlan, options);

        assertThat(adapter).isInstanceOf(FileSystemHermesLearnedSkillPersistenceAdapter.class);
        assertThat(adapter.adapterId()).isEqualTo("file-system");
        assertThat(adapter.targetPlan().targetSummary())
                .isEqualTo("definitions=file-system,artifacts=file-system");
        assertThat(adapter.toMetadata())
                .containsEntry("definitionDirectory", tempDir.resolve("definitions").toAbsolutePath().normalize().toString())
                .containsEntry("artifactDirectory", tempDir.resolve("artifacts").toAbsolutePath().normalize().toString());
    }

    @Test
    void resolvesOptionsFromPersistenceHints(@TempDir Path tempDir) {
        HermesLearnedSkillPersistenceAdapterResolverOptions options =
                HermesLearnedSkillPersistenceAdapterResolverOptions.fromHints(Map.of(
                        HermesSkillPersistenceHintKeys.FILE_ROOT,
                        tempDir.resolve("learned").toString(),
                        "object-prefix",
                        "tenant-a/hermes",
                        "jdbc-definition-table",
                        "hermes_defs",
                        "jdbc-artifact-table",
                        "hermes_artifacts",
                        "jdbc-initialize-schema",
                        "false"));

        assertThat(options.fileSystemDefinitionDirectory())
                .isEqualTo(tempDir.resolve("learned").resolve("definitions"));
        assertThat(options.fileSystemArtifactDirectory())
                .isEqualTo(tempDir.resolve("learned").resolve("artifacts"));
        assertThat(options.objectStorageDefinitionPrefix()).isEqualTo("tenant-a/hermes/definitions");
        assertThat(options.objectStorageArtifactPrefix()).isEqualTo("tenant-a/hermes/artifacts");
        assertThat(options.jdbcDefinitionTableName()).isEqualTo("hermes_defs");
        assertThat(options.jdbcArtifactTableName()).isEqualTo("hermes_artifacts");
        assertThat(options.jdbcInitializeSchema()).isFalse();
    }

    @Test
    void resolvesObjectStorageBackedAdapterWhenRuntimeStorageIsAvailable(@TempDir Path tempDir) {
        HermesSkillPersistenceTargetPlan targetPlan = HermesSkillPersistenceStrategy.fromHints(Map.of(
                HermesSkillPersistenceHintKeys.DEFINITIONS, "s3",
                HermesSkillPersistenceHintKeys.ARTIFACTS, "rustfs",
                HermesSkillPersistenceHintKeys.FALLBACK, "file-system"))
                .routePlan()
                .targetPlan();
        HermesLearnedSkillPersistenceAdapterResolverOptions options =
                new HermesLearnedSkillPersistenceAdapterResolverOptions(
                        tempDir,
                        null,
                        null,
                        "tenant-a/hermes",
                        null,
                        null,
                        null,
                        null,
                        true);
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        HermesLearnedSkillPersistenceAdapter adapter =
                HermesLearnedSkillPersistenceAdapterResolver.resolve(
                        service(tempDir),
                        targetPlan,
                        options,
                        HermesPersistenceResources.of(Optional.of(storage), Optional.empty()));
        HermesLearnedSkillRepository repository = new HermesLearnedSkillRepository(
                adapter,
                new HermesSkillMarkdownRenderer());
        HermesLearningSignal signal = signal();
        var skill = new HermesSkillDistiller().distill(signal, HermesAgentModeConfig.defaults());

        HermesLearningResult result = repository.create(skill, signal).await().indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(adapter).isInstanceOf(SkillManagementHermesLearnedSkillPersistenceAdapter.class);
        assertThat(adapter.targetPlan().targetSummary()).isEqualTo("definitions=s3,artifacts=rustfs");
        assertThat(adapter.find(skill.id()).await().indefinitely()).contains(skill);
        assertThat(storage.objects.keySet()).anyMatch(key -> key.startsWith("tenant-a/hermes/definitions/"));
        assertThat(storage.objects.keySet()).anyMatch(key -> key.startsWith("tenant-a/hermes/artifacts/"));
    }

    @Test
    void resolvesDatabaseBackedAdapterWhenRuntimeDataSourceIsAvailable(@TempDir Path tempDir) {
        HermesSkillPersistenceTargetPlan targetPlan = HermesSkillPersistenceStrategy.fromHints(Map.of(
                HermesSkillPersistenceHintKeys.DEFINITIONS, "database",
                HermesSkillPersistenceHintKeys.ARTIFACTS, "database",
                HermesSkillPersistenceHintKeys.FALLBACK, "file-system"))
                .routePlan()
                .targetPlan();
        HermesLearnedSkillPersistenceAdapterResolverOptions options =
                new HermesLearnedSkillPersistenceAdapterResolverOptions(
                        tempDir,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "hermes_skill_defs",
                        "hermes_skill_artifacts",
                        true);
        InMemoryLearnedSkillDataSource dataSource = new InMemoryLearnedSkillDataSource();
        HermesLearnedSkillPersistenceAdapter adapter =
                HermesLearnedSkillPersistenceAdapterResolver.resolve(
                        service(tempDir),
                        targetPlan,
                        options,
                        HermesPersistenceResources.of(Optional.empty(), Optional.of(dataSource)));
        HermesLearnedSkillRepository repository = new HermesLearnedSkillRepository(
                adapter,
                new HermesSkillMarkdownRenderer());
        HermesLearningSignal signal = signal();
        var skill = new HermesSkillDistiller().distill(signal, HermesAgentModeConfig.defaults());

        HermesLearningResult result = repository.create(skill, signal).await().indefinitely();

        assertThat(result.decision()).isEqualTo(HermesLearningDecision.CREATED);
        assertThat(adapter).isInstanceOf(SkillManagementHermesLearnedSkillPersistenceAdapter.class);
        assertThat(adapter.targetPlan().targetSummary()).isEqualTo("definitions=database,artifacts=database");
        assertThat(adapter.find(skill.id()).await().indefinitely()).contains(skill);
        assertThat(dataSource.definitions).containsKey(skill.id());
        assertThat(dataSource.artifacts.keySet())
                .anyMatch(reference -> reference.skillId().equals(skill.id())
                        && reference.artifactName().equals("SKILL.md"));
    }

    private static SkillManagementService service(Path tempDir) {
        return new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new InMemorySkillArtifactStore(),
                SkillManagementEventSink.noop());
    }

    private static HermesLearningSignal signal() {
        return new HermesLearningSignal(
                "req-object-adapter",
                "Archive multi tenant audit evidence",
                "Audit evidence archived",
                true,
                List.of(),
                List.of("rag"),
                Map.of(),
                Instant.parse("2026-06-02T00:00:00Z"));
    }

    private record ArtifactReference(
            String skillId,
            String artifactKind,
            String artifactName,
            String artifactVersion) {
    }

    private record ArtifactRow(String manifest, String contentBase64) {
    }

    private static final class InMemoryLearnedSkillDataSource extends AbstractHermesJdbcDataSource {
        private final Map<String, String> definitions = new LinkedHashMap<>();
        private final Map<ArtifactReference, ArtifactRow> artifacts = new LinkedHashMap<>();

        @Override
        protected int executeUpdate(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.startsWith("UPDATE") && normalizedSql.contains(" CONTENT = ")) {
                String skillId = (String) parameters.get(3);
                if (!definitions.containsKey(skillId)) {
                    return 0;
                }
                definitions.put(skillId, (String) parameters.get(1));
                return 1;
            }
            if (normalizedSql.startsWith("INSERT") && normalizedSql.contains("(SKILL_ID, CONTENT")) {
                definitions.put((String) parameters.get(1), (String) parameters.get(2));
                return 1;
            }
            if (normalizedSql.startsWith("DELETE") && normalizedSql.contains("ARTIFACT_KIND")) {
                return artifacts.remove(artifactReference(parameters, 1)) == null ? 0 : 1;
            }
            if (normalizedSql.startsWith("DELETE")) {
                return definitions.remove((String) parameters.get(1)) == null ? 0 : 1;
            }
            if (normalizedSql.startsWith("UPDATE") && normalizedSql.contains("CONTENT_BASE64")) {
                ArtifactReference reference = artifactReference(parameters, 4);
                if (!artifacts.containsKey(reference)) {
                    return 0;
                }
                artifacts.put(reference, new ArtifactRow((String) parameters.get(2), (String) parameters.get(1)));
                return 1;
            }
            if (normalizedSql.startsWith("INSERT") && normalizedSql.contains("CONTENT_BASE64")) {
                artifacts.put(
                        artifactReference(parameters, 1),
                        new ArtifactRow((String) parameters.get(6), (String) parameters.get(5)));
                return 1;
            }
            return 0;
        }

        @Override
        protected List<List<Object>> select(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.startsWith("SELECT CONTENT FROM") && normalizedSql.contains("WHERE SKILL_ID")) {
                String content = definitions.get((String) parameters.get(1));
                return content == null ? List.of() : List.of(List.of(content));
            }
            if (normalizedSql.startsWith("SELECT CONTENT FROM")) {
                return definitions.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(entry -> List.<Object>of(entry.getValue()))
                        .toList();
            }
            if (normalizedSql.startsWith("SELECT MANIFEST, CONTENT_BASE64")) {
                ArtifactRow row = artifacts.get(artifactReference(parameters, 1));
                return row == null ? List.of() : List.of(List.of(row.manifest(), row.contentBase64()));
            }
            if (normalizedSql.startsWith("SELECT SKILL_ID, ARTIFACT_KIND")) {
                return artifactReferences().stream()
                        .map(reference -> List.<Object>of(
                                reference.skillId(),
                                reference.artifactKind(),
                                reference.artifactName(),
                                reference.artifactVersion()))
                        .toList();
            }
            return List.of();
        }

        private List<ArtifactReference> artifactReferences() {
            return new ArrayList<>(artifacts.keySet()).stream()
                    .sorted(Comparator
                            .comparing(ArtifactReference::skillId)
                            .thenComparing(ArtifactReference::artifactKind)
                            .thenComparing(ArtifactReference::artifactName)
                            .thenComparing(ArtifactReference::artifactVersion))
                    .toList();
        }

        private static ArtifactReference artifactReference(Map<Integer, Object> parameters, int startIndex) {
            return new ArtifactReference(
                    (String) parameters.get(startIndex),
                    (String) parameters.get(startIndex + 1),
                    (String) parameters.get(startIndex + 2),
                    (String) parameters.get(startIndex + 3));
        }
    }
}
