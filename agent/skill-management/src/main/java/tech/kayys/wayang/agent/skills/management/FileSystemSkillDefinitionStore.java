package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * File-backed skill definition store for fallback and local-first deployments.
 */
public final class FileSystemSkillDefinitionStore implements SkillDefinitionStore {

    private final Path directory;
    private final SkillDefinitionPropertiesCodec codec;

    public FileSystemSkillDefinitionStore(Path directory) {
        this(directory, new SkillDefinitionPropertiesCodec());
    }

    FileSystemSkillDefinitionStore(Path directory, SkillDefinitionPropertiesCodec codec) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
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
    public List<SkillDefinition> listSkills() {
        return SkillManagementFileStoreSupport.regularFiles(
                        directory,
                        path -> path.getFileName().toString().endsWith(SkillDefinitionPropertiesCodec.EXTENSION),
                        "skill definition files")
                .stream()
                .map(this::read)
                .toList();
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        Objects.requireNonNull(skill, "skill");
        SkillManagementFileStoreSupport.ensureDirectory(
                directory,
                "Failed to create skill definition directory");
        SkillManagementFileStoreSupport.writeBytes(
                pathFor(skill.id()),
                codec.toBytes(skill),
                "Failed to persist skill definition: " + skill.id());
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return false;
        }
        return SkillManagementFileStoreSupport.deleteIfExists(
                pathFor(skillId),
                "Failed to delete skill definition: " + skillId);
    }

    private SkillDefinition read(Path path) {
        return codec.fromBytes(
                SkillManagementFileStoreSupport.readAllBytes(path, "Failed to read skill definition file"),
                path.toString());
    }

    private Path pathFor(String skillId) {
        return SkillManagementFileNames.skillFile(
                directory,
                skillId,
                SkillDefinitionPropertiesCodec.EXTENSION,
                "file persistence");
    }
}
