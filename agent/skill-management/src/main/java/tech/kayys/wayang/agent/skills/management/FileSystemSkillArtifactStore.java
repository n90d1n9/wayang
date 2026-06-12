package tech.kayys.wayang.agent.skills.management;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * File-backed artifact store for local fallback and development deployments.
 */
public final class FileSystemSkillArtifactStore implements SkillArtifactStore {

    private final Path directory;
    private final SkillArtifactManifestCodec manifestCodec;

    public FileSystemSkillArtifactStore(Path directory) {
        this(directory, new SkillArtifactManifestCodec());
    }

    FileSystemSkillArtifactStore(Path directory, SkillArtifactManifestCodec manifestCodec) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.manifestCodec = Objects.requireNonNull(manifestCodec, "manifestCodec");
    }

    @Override
    public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
        SkillArtifactReference resolved = requireReference(reference);
        Path contentPath = contentPath(resolved);
        if (!SkillManagementFileStoreSupport.isRegularFile(contentPath)) {
            return Optional.empty();
        }
        byte[] content = SkillManagementFileStoreSupport.readAllBytes(
                contentPath,
                "Failed to read skill artifact content");
        SkillArtifactManifest manifest = readManifest(resolved)
                .orElseGet(() -> new SkillArtifactManifest(
                        resolved,
                        SkillArtifact.DEFAULT_CONTENT_TYPE,
                        Map.of(),
                        -1,
                        ""));
        return Optional.of(manifest.toArtifact(content));
    }

    @Override
    public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
        SkillArtifactQuery resolved = query == null ? SkillArtifactQuery.all() : query;
        return SkillManagementFileStoreSupport.regularFilesRecursively(
                        directory,
                        path -> path.getFileName().toString().equals(SkillArtifactManifestCodec.FILE_NAME),
                        "skill artifact manifests")
                .stream()
                .map(this::readManifestPath)
                .map(SkillArtifactManifest::reference)
                .filter(resolved::matches)
                .sorted(Comparator.comparing(SkillArtifactReference::qualifiedName))
                .limit(resolved.limit())
                .toList();
    }

    @Override
    public void putArtifact(SkillArtifact artifact) {
        Objects.requireNonNull(artifact, "artifact");
        Path artifactDirectory = artifactDirectory(artifact.reference());
        SkillManagementFileStoreSupport.ensureDirectory(
                artifactDirectory,
                "Failed to create skill artifact directory");
        SkillManagementFileStoreSupport.writeBytes(
                artifactDirectory.resolve(ObjectStorageSkillArtifactStore.CONTENT_FILE_NAME),
                artifact.content(),
                "Failed to persist skill artifact content");
        SkillManagementFileStoreSupport.writeBytes(
                artifactDirectory.resolve(SkillArtifactManifestCodec.FILE_NAME),
                manifestCodec.toBytes(artifact),
                "Failed to persist skill artifact manifest");
    }

    @Override
    public boolean deleteArtifact(SkillArtifactReference reference) {
        SkillArtifactReference resolved = requireReference(reference);
        Path artifactDirectory = artifactDirectory(resolved);
        boolean contentDeleted = SkillManagementFileStoreSupport.deleteIfExists(
                artifactDirectory.resolve(ObjectStorageSkillArtifactStore.CONTENT_FILE_NAME),
                "Failed to delete skill artifact content");
        boolean manifestDeleted = SkillManagementFileStoreSupport.deleteIfExists(
                artifactDirectory.resolve(SkillArtifactManifestCodec.FILE_NAME),
                "Failed to delete skill artifact manifest");
        deleteEmptyParents(artifactDirectory);
        return contentDeleted || manifestDeleted;
    }

    private Optional<SkillArtifactManifest> readManifest(SkillArtifactReference reference) {
        Path manifestPath = artifactDirectory(reference).resolve(SkillArtifactManifestCodec.FILE_NAME);
        if (!SkillManagementFileStoreSupport.isRegularFile(manifestPath)) {
            return Optional.empty();
        }
        return Optional.of(readManifestPath(manifestPath));
    }

    private SkillArtifactManifest readManifestPath(Path path) {
        return manifestCodec.fromBytes(
                SkillManagementFileStoreSupport.readAllBytes(path, "Failed to read skill artifact manifest"),
                path.toString());
    }

    private Path contentPath(SkillArtifactReference reference) {
        return artifactDirectory(reference).resolve(ObjectStorageSkillArtifactStore.CONTENT_FILE_NAME);
    }

    private Path artifactDirectory(SkillArtifactReference reference) {
        Path path = directory;
        for (String segment : requireReference(reference).pathSegments()) {
            path = path.resolve(segment);
        }
        return path;
    }

    private void deleteEmptyParents(Path start) {
        Path current = start;
        while (current != null && !current.equals(directory)) {
            if (!deleteIfEmpty(current)) {
                return;
            }
            current = current.getParent();
        }
    }

    private boolean deleteIfEmpty(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        try (var children = Files.list(path)) {
            if (children.findAny().isPresent()) {
                return false;
            }
        } catch (IOException error) {
            throw new IllegalStateException("Failed to inspect skill artifact directory: " + path, error);
        }
        try {
            return Files.deleteIfExists(path);
        } catch (IOException error) {
            throw new IllegalStateException("Failed to delete empty skill artifact directory: " + path, error);
        }
    }

    private static SkillArtifactReference requireReference(SkillArtifactReference reference) {
        return Objects.requireNonNull(reference, "reference");
    }
}
