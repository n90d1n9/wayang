package tech.kayys.wayang.agent.hermes;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillArtifact;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillValidation;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * File-backed Hermes learned-skill persistence adapter for local fallback deployments.
 */
public final class FileSystemHermesLearnedSkillPersistenceAdapter
        implements HermesLearnedSkillPersistenceAdapter {

    private static final String DEFINITIONS_DIRECTORY = "definitions";
    private static final String ARTIFACTS_DIRECTORY = "artifacts";

    private final Path definitionDirectory;
    private final Path artifactDirectory;
    private final SkillManagementHermesLearnedSkillPersistenceAdapter delegate;

    public FileSystemHermesLearnedSkillPersistenceAdapter(Path rootDirectory) {
        this(
                requirePath(rootDirectory, "rootDirectory").resolve(DEFINITIONS_DIRECTORY),
                requirePath(rootDirectory, "rootDirectory").resolve(ARTIFACTS_DIRECTORY));
    }

    public FileSystemHermesLearnedSkillPersistenceAdapter(
            Path definitionDirectory,
            Path artifactDirectory) {
        this.definitionDirectory = requirePath(definitionDirectory, "definitionDirectory");
        this.artifactDirectory = requirePath(artifactDirectory, "artifactDirectory");
        this.delegate = SkillManagementHermesLearnedSkillPersistenceAdapter.from(
                serviceFor(this.definitionDirectory, this.artifactDirectory),
                fileTargetPlan());
    }

    public static FileSystemHermesLearnedSkillPersistenceAdapter at(Path rootDirectory) {
        return new FileSystemHermesLearnedSkillPersistenceAdapter(rootDirectory);
    }

    public Path definitionDirectory() {
        return definitionDirectory;
    }

    public Path artifactDirectory() {
        return artifactDirectory;
    }

    @Override
    public String adapterId() {
        return "file-system";
    }

    @Override
    public HermesSkillPersistenceTargetPlan targetPlan() {
        return delegate.targetPlan();
    }

    @Override
    public Uni<Optional<SkillDefinition>> find(String skillId) {
        return delegate.find(skillId);
    }

    @Override
    public Uni<List<SkillDefinition>> listLearnedSkills() {
        return delegate.listLearnedSkills();
    }

    @Override
    public SkillValidation validate(SkillDefinition skill) {
        return delegate.validate(skill);
    }

    @Override
    public Uni<SkillDefinition> create(SkillDefinition skill, SkillArtifact artifact) {
        return delegate.create(skill, artifact);
    }

    @Override
    public Uni<SkillDefinition> update(String skillId, SkillDefinition skill, SkillArtifact artifact) {
        return delegate.update(skillId, skill, artifact);
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>(HermesLearnedSkillPersistenceAdapter.super.toMetadata());
        values.put("storageFamily", "file-system");
        values.put("definitionDirectory", normalizedPath(definitionDirectory));
        values.put("artifactDirectory", normalizedPath(artifactDirectory));
        return Map.copyOf(values);
    }

    private static SkillManagementService serviceFor(
            Path definitionDirectory,
            Path artifactDirectory) {
        return new SkillManagementService(
                new FileSystemSkillDefinitionStore(definitionDirectory),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new FileSystemSkillArtifactStore(artifactDirectory),
                SkillManagementEventSink.noop());
    }

    private static HermesSkillPersistenceTargetPlan fileTargetPlan() {
        return HermesSkillPersistenceStrategy.fromHints(Map.of(
                HermesSkillPersistenceRouteRoles.DEFINITIONS, "file-system",
                HermesSkillPersistenceRouteRoles.ARTIFACTS, "file-system",
                HermesSkillPersistenceRouteRoles.FALLBACK, "file-system"))
                .routePlan()
                .targetPlan();
    }

    private static Path requirePath(Path path, String name) {
        return Objects.requireNonNull(path, name);
    }

    private static String normalizedPath(Path path) {
        return path.toAbsolutePath().normalize().toString();
    }
}
