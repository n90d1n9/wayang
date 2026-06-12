package tech.kayys.wayang.agent.skills.management;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * File-backed lifecycle state store for restart-safe local deployments.
 */
public final class FileSystemSkillLifecycleStateStore implements SkillLifecycleStateStore {

    private final Path directory;
    private final SkillLifecycleStatePropertiesCodec codec;

    public FileSystemSkillLifecycleStateStore(Path directory) {
        this(directory, new SkillLifecycleStatePropertiesCodec());
    }

    FileSystemSkillLifecycleStateStore(Path directory, SkillLifecycleStatePropertiesCodec codec) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public Optional<SkillLifecycleState> get(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return Optional.empty();
        }
        Path path = pathFor(skillId);
        if (!SkillManagementFileStoreSupport.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(read(path));
    }

    @Override
    public SkillLifecycleState save(SkillLifecycleState state) {
        Objects.requireNonNull(state, "state");
        SkillManagementFileStoreSupport.ensureDirectory(
                directory,
                "Failed to create skill lifecycle state directory");
        SkillManagementFileStoreSupport.writeBytes(
                pathFor(state.skillId()),
                codec.toBytes(state),
                "Failed to persist skill lifecycle state: " + state.skillId());
        return state;
    }

    @Override
    public boolean remove(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return false;
        }
        return SkillManagementFileStoreSupport.deleteIfExists(
                pathFor(skillId),
                "Failed to delete skill lifecycle state: " + skillId);
    }

    @Override
    public Map<String, SkillLifecycleState> snapshot() {
        List<SkillLifecycleState> states = SkillManagementFileStoreSupport.regularFiles(
                        directory,
                        path -> path.getFileName().toString()
                                .endsWith(SkillLifecycleStatePropertiesCodec.EXTENSION),
                        "skill lifecycle state files")
                .stream()
                .map(this::read)
                .toList();
        return states.stream()
                .collect(Collectors.toUnmodifiableMap(SkillLifecycleState::skillId, state -> state));
    }

    private SkillLifecycleState read(Path path) {
        return codec.fromBytes(
                SkillManagementFileStoreSupport.readAllBytes(path, "Failed to read skill lifecycle state file"),
                path.toString());
    }

    private Path pathFor(String skillId) {
        return SkillManagementFileNames.skillFile(
                directory,
                skillId,
                SkillLifecycleStatePropertiesCodec.EXTENSION,
                "lifecycle persistence");
    }
}
